//! Official-metadata vanilla adapter for legacy and modern versions on x86_64 hosts.
use crate::{
    auth::Session,
    storage::{Profile, Result},
};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use sha1::{Digest, Sha1};
use std::{
    collections::HashMap,
    path::{Component, Path, PathBuf},
    time::Duration,
};
use tokio::{io::AsyncWriteExt, process::Command};

pub const MANIFEST: &str = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
#[derive(Clone, Deserialize)]
pub struct Artifact {
    pub url: String,
    pub sha1: String,
    pub size: u64,
    pub path: Option<String>,
}
#[derive(Serialize, Clone)]
pub struct Transfer {
    pub(crate) stage: String,
    pub(crate) completed: usize,
    pub(crate) total: usize,
}
pub fn http() -> Result<Client> {
    Client::builder()
        .user_agent(concat!(
            "HeraClient/",
            env!("CARGO_PKG_VERSION"),
            " (+https://github.com/Lyvt-Dev/hera-client)"
        ))
        .https_only(true)
        .redirect(reqwest::redirect::Policy::none())
        .connect_timeout(Duration::from_secs(15))
        .timeout(Duration::from_secs(120))
        .build()
        .map_err(|e| e.to_string())
}
pub fn os() -> &'static str {
    if cfg!(target_os = "windows") {
        "windows"
    } else if cfg!(target_os = "macos") {
        "osx"
    } else {
        "linux"
    }
}
pub fn safe_join(root: &Path, relative: &str) -> Result<PathBuf> {
    let path = Path::new(relative);
    if relative.is_empty()
        || relative.contains(['\\', ':', '\0'])
        || path
            .components()
            .any(|c| !matches!(c, Component::Normal(_)))
    {
        return Err("Unsicherer Downloadpfad".into());
    }
    Ok(root.join(path))
}
fn allowed_url(raw: &str) -> Result<()> {
    let u = reqwest::Url::parse(raw).map_err(|_| "Ungültige Download-URL")?;
    if u.scheme() != "https"
        || !matches!(
            u.host_str(),
            Some(
                "piston-meta.mojang.com"
                    | "piston-data.mojang.com"
                    | "launchermeta.mojang.com"
                    | "launcher.mojang.com"
                    | "libraries.minecraft.net"
                    | "resources.download.minecraft.net"
                    | "cdn.modrinth.com"
                    | "edge.forgecdn.net"
                    | "mediafilez.forgecdn.net"
                    | "media.forgecdn.net"
                    | "meta.fabricmc.net"
                    | "maven.fabricmc.net"
                    | "maven.minecraftforge.net"
                    | "files.minecraftforge.net"
                    | "libraries.minecraftforge.net"
            )
        )
        || u.port().is_some()
        || !u.username().is_empty()
        || u.password().is_some()
    {
        return Err("Nicht freigegebene Downloadquelle".into());
    }
    Ok(())
}
pub async fn json(http: &Client, url: &str) -> Result<Value> {
    allowed_url(url)?;
    let r = http
        .get(url)
        .send()
        .await
        .map_err(|_| "Metadatendienst nicht erreichbar")?;
    if !r.status().is_success() {
        return Err(format!("Metadaten: HTTP {}", r.status()));
    }
    // Metadata is bounded; response streams must not exhaust memory.
    let bytes = bounded_body(r, 16 * 1024 * 1024).await?;
    serde_json::from_slice(&bytes).map_err(|_| "Ungültige Metadaten".into())
}
pub(crate) async fn bounded_body(mut r: reqwest::Response, max: usize) -> Result<Vec<u8>> {
    let mut bytes = Vec::new();
    while let Some(chunk) = r.chunk().await.map_err(|_| "Download unterbrochen")? {
        if bytes.len() + chunk.len() > max {
            return Err("Datei überschreitet Größenlimit".into());
        }
        bytes.extend_from_slice(&chunk);
    }
    Ok(bytes)
}
pub(crate) async fn valid(path: &Path, a: &Artifact) -> bool {
    use tokio::io::AsyncReadExt;
    let Ok(mut f) = tokio::fs::File::open(path).await else {
        return false;
    };
    if !f.metadata().await.is_ok_and(|m| m.len() == a.size) {
        return false;
    }
    let mut h = Sha1::new();
    // This buffer survives an await. Keep it on the heap so callers do not
    // inherit a large inline future on the Windows webview command stack.
    let mut buf = vec![0u8; 65536];
    loop {
        match f.read(&mut buf).await {
            Ok(0) => break,
            Ok(n) => h.update(&buf[..n]),
            Err(_) => return false,
        }
    }
    format!("{:x}", h.finalize()) == a.sha1
}
pub async fn download(http: &Client, a: &Artifact, path: &Path) -> Result<()> {
    allowed_url(&a.url)?;
    if a.sha1.len() != 40
        || !a.sha1.bytes().all(|b| b.is_ascii_hexdigit())
        || a.size > 512 * 1024 * 1024
    {
        return Err("Ungültige Dateiintegritätsdaten".into());
    }
    if valid(path, a).await {
        return Ok(());
    }
    let parent = path.parent().ok_or("Downloadpfad ohne Elternverzeichnis")?;
    tokio::fs::create_dir_all(parent)
        .await
        .map_err(|e| e.to_string())?;
    let temporary = parent.join(format!(".{}.part", uuid::Uuid::new_v4()));
    let mut last_error = "Download fehlgeschlagen".to_string();
    for attempt in 0..3 {
        let result = async {
            let mut r = http
                .get(&a.url)
                .send()
                .await
                .map_err(|_| "Downloadquelle nicht erreichbar".to_string())?;
            if !r.status().is_success() {
                return Err(format!("Download: HTTP {}", r.status()));
            }
            if r.content_length().is_some_and(|n| n != a.size) {
                return Err("Unerwartete Dateigröße".into());
            }
            let mut file = tokio::fs::File::create(&temporary)
                .await
                .map_err(|e| e.to_string())?;
            let mut n = 0u64;
            let mut h = Sha1::new();
            while let Some(chunk) = r.chunk().await.map_err(|_| "Download unterbrochen")? {
                n += chunk.len() as u64;
                if n > a.size {
                    return Err("Download überschreitet erwartete Größe".into());
                }
                h.update(&chunk);
                file.write_all(&chunk).await.map_err(|e| e.to_string())?;
            }
            file.sync_all().await.map_err(|e| e.to_string())?;
            drop(file);
            if n != a.size || format!("{:x}", h.finalize()) != a.sha1 {
                return Err("Integritätsprüfung fehlgeschlagen".into());
            }
            // Replacement only after a complete, verified file is available.
            if path.exists() {
                tokio::fs::remove_file(path)
                    .await
                    .map_err(|e| e.to_string())?;
            }
            tokio::fs::rename(&temporary, path)
                .await
                .map_err(|e| e.to_string())?;
            Ok(())
        }
        .await;
        match result {
            Ok(()) => return Ok(()),
            Err(e) => last_error = e,
        }
        let _ = tokio::fs::remove_file(&temporary).await;
        if attempt < 2 {
            tokio::time::sleep(Duration::from_secs(1 << attempt)).await;
        }
    }
    Err(last_error)
}
pub fn rule_allowed(value: &Value) -> bool {
    let Some(rules) = value.get("rules").and_then(Value::as_array) else {
        return true;
    };
    let mut allowed = false;
    for r in rules {
        // Unknown constraints are rejected instead of accidentally broadening support.
        if r.get("os").is_some_and(|v| v.get("version").is_some()) {
            return false;
        }
        let matches_os = r.get("os").is_none_or(|v| {
            v["name"].as_str().is_none_or(|name| name == os())
                && v["arch"]
                    .as_str()
                    .is_none_or(|a| a == "x86_64" || a == "amd64")
                && v.get("version").is_none()
        });
        let matches_features = r.get("features").is_none_or(|v| {
            v.as_object()
                .is_some_and(|o| o.values().all(|v| v == false))
        });
        if matches_os && matches_features {
            allowed = r["action"] == "allow";
        }
    }
    allowed
}
fn artifact(v: &Value) -> Result<Artifact> {
    serde_json::from_value(v.clone()).map_err(|_| "Unvollständige Download-Metadaten".into())
}
fn text<'a>(v: &'a Value, k: &str) -> Result<&'a str> {
    v[k].as_str()
        .ok_or_else(|| format!("Minecraft-Metadaten: {k} fehlt"))
}
fn check_host() -> Result<()> {
    if !cfg!(target_arch = "x86_64") {
        return Err("Der experimentelle Adapter unterstützt derzeit nur x86_64. ARM64 muss separat getestet werden.".into());
    }
    Ok(())
}
#[derive(Clone)]
pub struct Installation {
    pub java: String,
    pub manifest: Value,
    pub classpath: Vec<PathBuf>,
    pub game: PathBuf,
    pub natives: PathBuf,
    pub assets: PathBuf,
    pub libraries: PathBuf,
}
pub async fn install(
    http: &Client,
    root: &Path,
    profile: &Profile,
    progress: &impl Fn(Transfer),
) -> Result<Installation> {
    check_host()?;
    profile.validate()?;
    let index = json(http, MANIFEST).await?;
    let version = index["versions"]
        .as_array()
        .and_then(|a| a.iter().find(|v| v["id"] == profile.version))
        .ok_or("Version fehlt im offiziellen Katalog")?;
    let url = text(version, "url")?;
    allowed_url(url)?;
    let response = http
        .get(url)
        .send()
        .await
        .map_err(|_| "Versionsmetadaten nicht erreichbar")?;
    if !response.status().is_success() {
        return Err("Versionsmetadaten konnten nicht geladen werden".into());
    }
    let bytes = bounded_body(response, 4 * 1024 * 1024).await?;
    if format!("{:x}", Sha1::digest(&bytes)) != text(version, "sha1")? {
        return Err("Versionsmetadaten: Integritätsprüfung fehlgeschlagen".into());
    }
    let manifest: Value =
        serde_json::from_slice(&bytes).map_err(|_| "Ungültige Versionsmetadaten")?;
    if manifest["id"] != profile.version
        || !matches!(
            manifest["javaVersion"]["majorVersion"].as_u64(),
            Some(8 | 16 | 17 | 21 | 25)
        )
    {
        return Err("Adapter unterstützt diese Metadaten nicht".into());
    }
    let game = root.join("profiles").join(&profile.id);
    let natives = game.join("natives");
    let assets = root.join("assets");
    for p in [
        &game,
        &natives,
        &game.join("mods"),
        &game.join("resourcepacks"),
        &game.join("shaderpacks"),
    ] {
        tokio::fs::create_dir_all(p)
            .await
            .map_err(|e| e.to_string())?;
    }
    let mut files: Vec<(Artifact, PathBuf)> = vec![];
    let mut classpath = vec![];
    let mut native_jars = vec![];
    for library in manifest["libraries"]
        .as_array()
        .ok_or("Bibliotheksliste fehlt")?
    {
        if !rule_allowed(library) {
            continue;
        }
        if library["downloads"]["artifact"].is_object() {
            let a = artifact(&library["downloads"]["artifact"])?;
            let path = safe_join(
                &root.join("libraries"),
                a.path.as_deref().ok_or("Bibliothekspfad fehlt")?,
            )?;
            classpath.push(path.clone());
            files.push((a, path));
        }
        if let Some(classifier) = library["natives"][os()].as_str() {
            let classifier = classifier.replace("${arch}", "64");
            let a = artifact(&library["downloads"]["classifiers"][&classifier])?;
            let path = safe_join(
                &root.join("libraries"),
                a.path.as_deref().ok_or("Native-Pfad fehlt")?,
            )?;
            native_jars.push(path.clone());
            files.push((a, path));
        }
    }
    let client = artifact(&manifest["downloads"]["client"])?;
    let client_path = root
        .join("versions")
        .join(&profile.version)
        .join("client.jar");
    classpath.push(client_path.clone());
    files.push((client, client_path));
    if manifest["logging"]["client"].is_object() {
        let config = artifact(&manifest["logging"]["client"]["file"])?;
        files.push((config, game.join("logging.xml")));
    }
    let ai = artifact(&manifest["assetIndex"])?;
    let asset_id = text(&manifest["assetIndex"], "id")?;
    if !asset_id
        .bytes()
        .all(|b| b.is_ascii_alphanumeric() || b == b'.' || b == b'-')
    {
        return Err("Unsichere Asset-ID".into());
    }
    let index_path = assets.join("indexes").join(format!("{asset_id}.json"));
    download(http, &ai, &index_path).await?;
    let asset_index: Value = serde_json::from_slice(
        &tokio::fs::read(&index_path)
            .await
            .map_err(|e| e.to_string())?,
    )
    .map_err(|_| "Ungültiger Asset-Index")?;
    for (name, obj) in asset_index["objects"]
        .as_object()
        .ok_or("Asset-Liste fehlt")?
        .iter()
    {
        // Validate every legacy mapping before downloads or filesystem writes.
        safe_join(&assets, name)?;
        let hash = text(obj, "hash")?;
        if hash.len() != 40 || !hash.bytes().all(|b| b.is_ascii_hexdigit()) {
            return Err("Ungültiger Asset-Hash".into());
        }
        let rel = format!("{}/{hash}", &hash[..2]);
        files.push((
            Artifact {
                url: format!("https://resources.download.minecraft.net/{rel}"),
                sha1: hash.into(),
                size: obj["size"].as_u64().ok_or("Asset-Größe fehlt")?,
                path: None,
            },
            assets.join("objects").join(rel),
        ));
    }
    // Older versions expect named assets, not only the content-addressed object store.
    if asset_index["virtual"] == true || asset_index["map_to_resources"] == true {
        let target = if asset_index["map_to_resources"] == true {
            game.join("resources")
        } else {
            assets.join("virtual").join(asset_id)
        };
        for (name, obj) in asset_index["objects"]
            .as_object()
            .ok_or("Asset-Liste fehlt")?
        {
            let hash = text(obj, "hash")?;
            files.push((
                Artifact {
                    url: format!(
                        "https://resources.download.minecraft.net/{}/{hash}",
                        &hash[..2]
                    ),
                    sha1: hash.into(),
                    size: obj["size"].as_u64().ok_or("Asset-Größe fehlt")?,
                    path: None,
                },
                safe_join(&target, name)?,
            ));
        }
    }
    // Bounded concurrency keeps first installation usable without flooding the provider.
    let total = files.len();
    let mut completed = 0;
    for batch in files.chunks(8) {
        let mut jobs = tokio::task::JoinSet::new();
        for (a, p) in batch {
            let http = http.clone();
            let a = a.clone();
            let p = p.clone();
            jobs.spawn(async move { download(&http, &a, &p).await });
        }
        let mut failure = None;
        while let Some(r) = jobs.join_next().await {
            match r {
                Ok(Ok(())) => {
                    completed += 1;
                    progress(Transfer {
                        stage: "Minecraft-Dateien prüfen und installieren".into(),
                        completed,
                        total,
                    });
                }
                Ok(Err(e)) => failure = Some(e),
                Err(_) => failure = Some("Download-Task fehlgeschlagen".into()),
            }
        }
        if let Some(e) = failure {
            return Err(e);
        }
    }
    let native_dir = natives.clone();
    tokio::task::spawn_blocking(move || extract_natives(&native_jars, &native_dir))
        .await
        .map_err(|_| "Entpacken fehlgeschlagen")??;
    progress(Transfer {
        stage: "Installation vollständig verifiziert".into(),
        completed: total,
        total,
    });
    Ok(Installation {
        java: profile.java_path.clone(),
        manifest,
        classpath,
        game,
        natives,
        assets,
        libraries: root.join("libraries"),
    })
}
fn extract_natives(jars: &[PathBuf], out: &Path) -> Result<()> {
    for path in jars {
        let mut zip = zip::ZipArchive::new(std::fs::File::open(path).map_err(|e| e.to_string())?)
            .map_err(|_| "Ungültiges Native-Archiv")?;
        for i in 0..zip.len() {
            let mut file = zip.by_index(i).map_err(|_| "Archivfehler")?;
            if file.is_dir() || file.name().starts_with("META-INF/") {
                continue;
            }
            let rel = file.enclosed_name().ok_or("Unsicherer Archivpfad")?;
            if file.size() > 128 * 1024 * 1024
                || file.unix_mode().is_some_and(|m| m & 0o170000 == 0o120000)
            {
                return Err("Unsicherer Archiveintrag".into());
            }
            let target = safe_join(out, rel.to_str().ok_or("Ungültiger Archivpfad")?)?;
            if let Some(p) = target.parent() {
                std::fs::create_dir_all(p).map_err(|e| e.to_string())?;
            }
            let mut dest = std::fs::File::create(target).map_err(|e| e.to_string())?;
            std::io::copy(&mut file, &mut dest).map_err(|e| e.to_string())?;
        }
    }
    Ok(())
}
fn arguments(v: &Value, vars: &HashMap<String, String>) -> Result<Vec<String>> {
    let mut result = vec![];
    for arg in v.as_array().ok_or("Moderne Startargumente fehlen")? {
        let values = if let Some(s) = arg.as_str() {
            vec![s]
        } else if rule_allowed(arg) {
            if let Some(s) = arg["value"].as_str() {
                vec![s]
            } else {
                arg["value"]
                    .as_array()
                    .ok_or("Ungültiges Argument")?
                    .iter()
                    .map(|v| v.as_str().ok_or("Ungültiges Argument".to_string()))
                    .collect::<Result<Vec<_>>>()?
            }
        } else {
            continue;
        };
        for value in values {
            let mut s = value.to_string();
            for (k, v) in vars {
                s = s.replace(&format!("${{{k}}}"), v);
            }
            if s.contains("${") {
                return Err(format!("Nicht unterstütztes Startargument: {value}"));
            }
            result.push(s);
        }
    }
    Ok(result)
}
fn java_major(output: &str) -> Option<u64> {
    let version = output.split('"').nth(1)?;
    let mut parts = version.split('.');
    let first = parts.next()?.parse::<u64>().ok()?;
    if first == 1 {
        parts.next()?.parse().ok()
    } else {
        Some(first)
    }
}
pub(crate) async fn check_java_major(path: &str, required: u64) -> Result<()> {
    let mut command = Command::new(path);
    command
        .arg("-version")
        .stdin(std::process::Stdio::null())
        .kill_on_drop(true);
    #[cfg(target_os = "windows")]
    command.creation_flags(0x08000000);
    let output = tokio::time::timeout(Duration::from_secs(15), command.output())
        .await
        .map_err(|_| "Java-Prüfung hat zu lange gedauert")?
        .map_err(|_| format!("Java nicht gefunden. Dieses Profil benötigt Java {required}."))?;
    let combined = format!(
        "{} {}",
        String::from_utf8_lossy(&output.stderr),
        String::from_utf8_lossy(&output.stdout)
    );
    if !output.status.success() || java_major(&combined) != Some(required) {
        return Err(format!("Dieses Profil benötigt Java {required}. Bitte die passende Java-Datei in den erweiterten Profileinstellungen auswählen."));
    }
    Ok(())
}
pub async fn command(profile: &Profile, session: &Session, i: &Installation) -> Result<Command> {
    check_java_major(
        &i.java,
        i.manifest["javaVersion"]["majorVersion"]
            .as_u64()
            .ok_or("Java-Anforderung fehlt")?,
    )
    .await?;
    let cp = std::env::join_paths(&i.classpath)
        .map_err(|_| "Ungültiger Klassenpfad")?
        .into_string()
        .map_err(|_| "Klassenpfad nicht darstellbar")?;
    let mut vars = HashMap::new();
    for (k, v) in [
        ("auth_player_name", session.account.name.clone()),
        ("auth_uuid", session.account.id.clone()),
        ("auth_access_token", session.token.clone()),
        ("version_name", profile.version.clone()),
        ("game_directory", i.game.to_string_lossy().into()),
        ("assets_root", i.assets.to_string_lossy().into()),
        (
            "assets_index_name",
            text(&i.manifest["assetIndex"], "id")?.into(),
        ),
        ("user_type", "msa".into()),
        ("version_type", "release".into()),
        ("natives_directory", i.natives.to_string_lossy().into()),
        ("launcher_name", "Hera".into()),
        ("launcher_version", env!("CARGO_PKG_VERSION").into()),
        ("classpath", cp),
        ("library_directory", i.libraries.to_string_lossy().into()),
        (
            "classpath_separator",
            if cfg!(windows) { ";" } else { ":" }.into(),
        ),
        ("user_properties", "{}".into()),
        (
            "auth_session",
            format!("token:{}:{}", session.token, session.account.id),
        ),
        (
            "game_assets",
            (if i.manifest["assetIndex"]["id"] == "pre-1.6" {
                i.game.join("resources")
            } else {
                i.assets
                    .join("virtual")
                    .join(text(&i.manifest["assetIndex"], "id")?)
            })
            .to_string_lossy()
            .into(),
        ),
        ("clientid", String::new()),
        ("auth_xuid", String::new()),
    ] {
        vars.insert(k.to_string(), v);
    }
    let mut c = Command::new(&i.java);
    c.current_dir(&i.game)
        .arg(format!("-Xmx{}M", profile.memory_mb))
        .arg("-Xms512M");
    if let Some(logging) = i.manifest["logging"]["client"]["argument"].as_str() {
        let path = i.game.join("logging.xml");
        if !path.is_file() {
            return Err("Geprüfte Logging-Konfiguration fehlt".into());
        }
        c.arg(logging.replace("${path}", &path.to_string_lossy()));
    }
    if i.manifest["arguments"].is_object() {
        c.args(arguments(&i.manifest["arguments"]["jvm"], &vars)?)
            .arg(text(&i.manifest, "mainClass")?)
            .args(arguments(&i.manifest["arguments"]["game"], &vars)?);
    } else {
        c.arg(format!("-Djava.library.path={}", i.natives.display()))
            .arg("-cp")
            .arg(vars.get("classpath").ok_or("Klassenpfad fehlt")?)
            .arg(text(&i.manifest, "mainClass")?);
        let legacy = text(&i.manifest, "minecraftArguments")?;
        let values = Value::Array(
            legacy
                .split_whitespace()
                .map(|s| Value::String(s.into()))
                .collect(),
        );
        c.args(arguments(&values, &vars)?);
    }
    // Never print command arguments: they contain the Minecraft access token.
    c.stdin(std::process::Stdio::null())
        .stdout(std::process::Stdio::null())
        .stderr(std::process::Stdio::null())
        .kill_on_drop(true);
    #[cfg(target_os = "windows")]
    c.creation_flags(0x08000000);
    Ok(c)
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn checksum_buffer_does_not_enlarge_async_stack() {
        let artifact = Artifact {
            url: String::new(),
            sha1: String::new(),
            size: 0,
            path: None,
        };
        let future = valid(Path::new("unused"), &artifact);
        assert!(std::mem::size_of_val(&future) < 4096);
    }
    #[test]
    fn java_generations_are_parsed() {
        assert_eq!(java_major("java version \"1.8.0_402\""), Some(8));
        assert_eq!(java_major("openjdk version \"17.0.12\""), Some(17));
        assert_eq!(java_major("openjdk version \"25.0.1\""), Some(25));
        assert_eq!(java_major("invalid"), None);
    }
    #[tokio::test]
    #[ignore = "Downloads official legacy and current game files"]
    async fn version_matrix_install() {
        let root = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../../.local/version-matrix");
        let client = http().unwrap();
        for version in ["rd-132211", "1.0", "1.8.9", "1.12.2", "1.20.1", "26.3"] {
            let p = Profile {
                id: uuid::Uuid::new_v4().to_string(),
                name: "Matrix".into(),
                version: version.into(),
                loader: "vanilla".into(),
                memory_mb: 4096,
                java_path: "java".into(),
            };
            let i = install(&client, &root, &p, &|_| {})
                .await
                .unwrap_or_else(|e| panic!("{version}: {e}"));
            assert!(i.classpath.iter().all(|p| p.is_file()));
            // Exercise authenticated argument construction without obtaining credentials or launching a process.
            let vars = HashMap::from([
                ("auth_player_name".into(), "Test Player".into()),
                ("auth_session".into(), "test-session".into()),
                ("auth_uuid".into(), "test-uuid".into()),
                ("auth_access_token".into(), "test-token".into()),
                ("version_name".into(), version.into()),
                ("game_directory".into(), "C:/Test Path".into()),
                ("assets_root".into(), "assets".into()),
                ("assets_index_name".into(), "index".into()),
                ("game_assets".into(), "assets".into()),
                ("user_properties".into(), "{}".into()),
                ("user_type".into(), "msa".into()),
                ("version_type".into(), "release".into()),
                ("clientid".into(), "".into()),
                ("auth_xuid".into(), "".into()),
            ]);
            let game_args = if i.manifest["arguments"].is_object() {
                i.manifest["arguments"]["game"].clone()
            } else {
                Value::Array(
                    text(&i.manifest, "minecraftArguments")
                        .unwrap()
                        .split_whitespace()
                        .map(|s| Value::String(s.into()))
                        .collect(),
                )
            };
            arguments(&game_args, &vars).unwrap_or_else(|e| panic!("{version}: {e}"));
            println!(
                "{version}: installation verified ({} classpath entries)",
                i.classpath.len()
            );
        }
    }
    #[test]
    fn path_traversal_rejected() {
        for p in [
            "../escape",
            "/root",
            "C:/escape",
            "ok/../../bad",
            "a\\b",
            "",
        ] {
            assert!(safe_join(Path::new("data"), p).is_err(), "{p}");
        }
        assert!(safe_join(Path::new("data"), "org/library.jar").is_ok());
    }
    #[test]
    fn host_allowlist() {
        assert!(allowed_url("https://libraries.minecraft.net/a.jar").is_ok());
        assert!(allowed_url("http://libraries.minecraft.net/a.jar").is_err());
        assert!(allowed_url("https://libraries.minecraft.net.evil.test/a").is_err());
    }
    #[test]
    fn argument_rules() {
        let v = serde_json::json!({"rules":[{"action":"allow","features":{"is_demo_user":true}}]});
        assert!(!rule_allowed(&v));
        let v = serde_json::json!({"rules":[{"action":"allow","os":{"name":os()}}]});
        assert!(rule_allowed(&v));
        let v = serde_json::json!({"rules":[{"action":"allow"},{"action":"disallow","os":{"version":"unknown-regex"}}]});
        assert!(!rule_allowed(&v));
    }
    #[test]
    fn substitutions_fail_closed() {
        let mut vars = HashMap::new();
        vars.insert("name".into(), "A B".into());
        assert_eq!(
            arguments(&serde_json::json!(["${name}"]), &vars).unwrap(),
            vec!["A B"]
        );
        assert!(arguments(&serde_json::json!(["${unknown}"]), &vars).is_err());
    }
    #[tokio::test]
    async fn detects_corrupt_cache() {
        let path = std::env::temp_dir().join(format!("hera-test-{}", uuid::Uuid::new_v4()));
        tokio::fs::write(&path, b"valid").await.unwrap();
        let a = Artifact {
            url: String::new(),
            sha1: format!("{:x}", Sha1::digest(b"valid")),
            size: 5,
            path: None,
        };
        assert!(valid(&path, &a).await);
        tokio::fs::write(&path, b"wrong").await.unwrap();
        assert!(!valid(&path, &a).await);
        tokio::fs::remove_file(path).await.unwrap();
    }
    #[tokio::test]
    #[ignore = "Downloads real Minecraft files; opt-in integration test"]
    async fn official_install_smoke() {
        let root = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../../.local/install-smoke");
        let p = Profile {
            id: "f1854701-8b0d-4eba-a288-c080932a5667".into(),
            name: "Installation test".into(),
            version: "1.21.1".into(),
            loader: "vanilla".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        };
        let client = http().unwrap();
        let first = install(&client, &root, &p, &|_| {}).await.unwrap();
        assert!(first.game.is_dir());
        assert!(first.classpath.iter().all(|p| p.is_file()));
        let target = first.classpath.last().unwrap();
        tokio::fs::write(target, b"intentional-test-corruption")
            .await
            .unwrap();
        install(&client, &root, &p, &|_| {}).await.unwrap();
        assert!(tokio::fs::metadata(target).await.unwrap().len() > 1000);
    }
}
