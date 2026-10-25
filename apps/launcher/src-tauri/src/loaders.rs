//! Loader adapters use official metadata/installers, never mod-provided processors.
use crate::{
    minecraft::{self, Artifact, Installation, Transfer},
    storage::{Profile, Result},
};
use reqwest::Client;
use serde_json::Value;
use std::{
    path::{Path, PathBuf},
    time::Duration,
};

const FABRIC: &str = "0.16.14";
const FORGE: &str = "52.1.20";
pub async fn install(
    http: &Client,
    root: &Path,
    p: &Profile,
    progress: &impl Fn(Transfer),
) -> Result<Installation> {
    let mut base = minecraft::install(http, root, p, progress).await?;
    match p.loader.as_str() {
        "vanilla" => {}
        "fabric" => fabric(http, root, p, &mut base).await?,
        "forge" => forge(http, root, p, &mut base).await?,
        _ => return Err("Unbekannter Loader".into()),
    }
    Ok(base)
}
fn maven_path(name: &str) -> Result<String> {
    let parts = name.split(':').collect::<Vec<_>>();
    if parts.len() != 3
        || parts.iter().any(|p| {
            p.is_empty()
                || !p
                    .bytes()
                    .all(|b| b.is_ascii_alphanumeric() || b"._+-".contains(&b))
        })
    {
        return Err("Nicht unterstützte Maven-Koordinate".into());
    }
    Ok(format!(
        "{}/{}/{}/{}-{}.jar",
        parts[0].replace('.', "/"),
        parts[1],
        parts[2],
        parts[1],
        parts[2]
    ))
}
fn merge(
    base: &mut Installation,
    overlay: &Value,
    paths: Vec<PathBuf>,
    library_root: PathBuf,
) -> Result<()> {
    if overlay["inheritsFrom"] != "1.21.1" {
        return Err("Loader erbt von einer nicht unterstützten Version".into());
    }
    let main = overlay["mainClass"]
        .as_str()
        .ok_or("Loader-Hauptklasse fehlt")?;
    let override_names = overlay["libraries"]
        .as_array()
        .ok_or("Loader-Bibliotheken fehlen")?
        .iter()
        .filter_map(|v| v["name"].as_str())
        .filter_map(|name| name.rsplit_once(':').map(|(key, _)| key.to_string()))
        .collect::<Vec<_>>();
    for old in base.manifest["libraries"]
        .as_array()
        .ok_or("Basisbibliotheken fehlen")?
    {
        if old["name"]
            .as_str()
            .and_then(|n| n.rsplit_once(':'))
            .is_some_and(|(key, _)| override_names.iter().any(|n| n == key))
        {
            if let Some(path) = old["downloads"]["artifact"]["path"].as_str() {
                base.classpath
                    .retain(|p| !p.to_string_lossy().replace('\\', "/").ends_with(path));
            }
        }
    }
    base.classpath.extend(paths);
    base.manifest["mainClass"] = Value::String(main.into());
    for kind in ["jvm", "game"] {
        if let Some(args) = overlay["arguments"][kind].as_array() {
            base.manifest["arguments"][kind]
                .as_array_mut()
                .ok_or("Basisargumente fehlen")?
                .extend(args.iter().cloned());
        }
    }
    base.libraries = library_root;
    Ok(())
}
async fn fabric(http: &Client, root: &Path, p: &Profile, base: &mut Installation) -> Result<()> {
    let meta = minecraft::json(
        http,
        &format!(
            "https://meta.fabricmc.net/v2/versions/loader/{}/{FABRIC}/profile/json",
            p.version
        ),
    )
    .await?;
    if meta["mainClass"] != "net.fabricmc.loader.impl.launch.knot.KnotClient" {
        return Err("Unerwartete Fabric-Hauptklasse".into());
    }
    let lib = root.join("libraries");
    let mut paths = vec![];
    for item in meta["libraries"]
        .as_array()
        .ok_or("Fabric-Bibliotheken fehlen")?
    {
        if item["url"] != "https://maven.fabricmc.net/" {
            return Err("Unerwartete Fabric-Quelle".into());
        }
        let rel = maven_path(item["name"].as_str().ok_or("Fabric-Koordinate fehlt")?)?;
        let url = format!("https://maven.fabricmc.net/{rel}");
        let sha1 = if let Some(s) = item["sha1"].as_str() {
            s.into()
        } else {
            checksum(http, &format!("{url}.sha1")).await?
        };
        let size = if let Some(n) = item["size"].as_u64() {
            n
        } else {
            content_length(http, &url).await?
        };
        let path = minecraft::safe_join(&lib, &rel)?;
        minecraft::download(
            http,
            &Artifact {
                url,
                sha1,
                size,
                path: None,
            },
            &path,
        )
        .await?;
        paths.push(path);
    }
    merge(base, &meta, paths, lib)
}
async fn checksum(http: &Client, url: &str) -> Result<String> {
    let r = http
        .get(url)
        .send()
        .await
        .map_err(|_| "Loader-Prüfsumme nicht erreichbar")?;
    if !r.status().is_success() {
        return Err("Loader-Prüfsumme fehlt".into());
    }
    let bytes = minecraft::bounded_body(r, 256).await?;
    let raw = String::from_utf8(bytes).map_err(|_| "Ungültige Prüfsumme")?;
    let hash = raw.split_whitespace().next().unwrap_or("").to_lowercase();
    if hash.len() != 40 || !hash.bytes().all(|b| b.is_ascii_hexdigit()) {
        return Err("Ungültige Loader-Prüfsumme".into());
    }
    Ok(hash)
}
async fn content_length(http: &Client, url: &str) -> Result<u64> {
    let r = http
        .head(url)
        .send()
        .await
        .map_err(|_| "Loader-Datei nicht erreichbar")?;
    if !r.status().is_success() {
        return Err("Loader-Datei nicht verfügbar".into());
    }
    r.headers()
        .get(reqwest::header::CONTENT_LENGTH)
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.parse().ok())
        .ok_or("Loader-Dateigröße fehlt".into())
}
async fn forge(http: &Client, root: &Path, p: &Profile, base: &mut Installation) -> Result<()> {
    let release = format!("{}-{FORGE}", p.version);
    let runtime = root.join("runtimes").join(format!("forge-{release}"));
    let installer = root
        .join("installers")
        .join(format!("forge-{release}-installer.jar"));
    let url=format!("https://maven.minecraftforge.net/net/minecraftforge/forge/{release}/forge-{release}-installer.jar");
    minecraft::download(
        http,
        &Artifact {
            sha1: checksum(http, &format!("{url}.sha1")).await?,
            size: content_length(http, &url).await?,
            url,
            path: None,
        },
        &installer,
    )
    .await?;
    let game_version = runtime.join("versions").join(&p.version);
    tokio::fs::create_dir_all(&game_version)
        .await
        .map_err(|e| e.to_string())?;
    let client = base
        .classpath
        .iter()
        .find(|p| p.file_name().is_some_and(|n| n == "client.jar"))
        .ok_or("Vanilla-Client fehlt")?;
    tokio::fs::copy(client, game_version.join(format!("{}.jar", p.version)))
        .await
        .map_err(|e| e.to_string())?;
    tokio::fs::write(
        game_version.join(format!("{}.json", p.version)),
        serde_json::to_vec(&base.manifest).map_err(|e| e.to_string())?,
    )
    .await
    .map_err(|e| e.to_string())?;
    let launcher = runtime.join("launcher_profiles.json");
    if !launcher.exists() {
        tokio::fs::write(&launcher, b"{\"profiles\":{}}")
            .await
            .map_err(|e| e.to_string())?;
    }
    let version_id = format!("{}-forge-{FORGE}", p.version);
    let version_file = runtime
        .join("versions")
        .join(&version_id)
        .join(format!("{version_id}.json"));
    if !version_file.exists() {
        minecraft::check_java(&p.java_path).await?;
        let mut command = tokio::process::Command::new(&p.java_path);
        command
            .current_dir(&runtime)
            .arg("-jar")
            .arg(&installer)
            .arg("--installClient")
            .arg(&runtime)
            .stdin(std::process::Stdio::null())
            .stdout(std::process::Stdio::null())
            .stderr(std::process::Stdio::null())
            .kill_on_drop(true);
        #[cfg(target_os = "windows")]
        command.creation_flags(0x08000000);
        let status = tokio::time::timeout(Duration::from_secs(600), command.status())
            .await
            .map_err(|_| "Forge-Installation hat das Zeitlimit überschritten")?
            .map_err(|_| "Offizieller Forge-Installer konnte nicht gestartet werden")?;
        if !status.success() {
            return Err("Offizieller Forge-Installer fehlgeschlagen. Details in der Installer-Logdatei im Forge-Runtime-Verzeichnis.".into());
        }
    }
    let bytes = tokio::fs::read(&version_file)
        .await
        .map_err(|_| "Forge-Versionsmetadaten fehlen nach Installation")?;
    if bytes.len() > 4 * 1024 * 1024 {
        return Err("Forge-Metadaten zu groß".into());
    }
    let meta: Value = serde_json::from_slice(&bytes).map_err(|_| "Ungültige Forge-Metadaten")?;
    if meta["id"] != version_id
        || meta["mainClass"] != "net.minecraftforge.bootstrap.ForgeBootstrap"
    {
        return Err("Nicht unterstützte Forge-Startkonfiguration".into());
    }
    let lib = runtime.join("libraries");
    let mut paths = vec![];
    for item in meta["libraries"]
        .as_array()
        .ok_or("Forge-Bibliotheken fehlen")?
    {
        if !minecraft::rule_allowed(item) {
            continue;
        }
        let a = &item["downloads"]["artifact"];
        let rel = if let Some(path) = a["path"].as_str() {
            path.to_string()
        } else {
            maven_path(item["name"].as_str().ok_or("Forge-Koordinate fehlt")?)?
        };
        let path = minecraft::safe_join(&lib, &rel)?;
        let artifact: Artifact =
            serde_json::from_value(a.clone()).map_err(|_| "Forge-Dateiintegritätsdaten fehlen")?;
        if !minecraft::valid(&path, &artifact).await {
            if artifact.url.is_empty() {
                return Err("Generierte Forge-Datei fehlt oder ist beschädigt. Forge-Runtime muss neu installiert werden.".into());
            }
            minecraft::download(http, &artifact, &path).await?;
        }
        paths.push(path);
    }
    merge(base, &meta, paths, lib)
}
#[cfg(test)]
mod tests {
    use super::*;
    #[tokio::test]
    #[ignore = "Installs official Fabric/Forge loaders and executes the verified Forge installer"]
    async fn official_loaders_smoke() {
        let root = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../../.local/install-smoke");
        let mut p = Profile {
            id: "f1854701-8b0d-4eba-a288-c080932a5667".into(),
            name: "Loader integration".into(),
            version: "1.21.1".into(),
            loader: "vanilla".into(),
            memory_mb: 4096,
            java_path: std::env::var("HERA_TEST_JAVA").expect("HERA_TEST_JAVA required"),
        };
        let http = minecraft::http().unwrap();
        let base = minecraft::install(&http, &root, &p, &|_| {}).await.unwrap();
        p.loader = "fabric".into();
        let mut f = base.clone();
        fabric(&http, &root, &p, &mut f).await.unwrap();
        assert_eq!(
            f.manifest["mainClass"],
            "net.fabricmc.loader.impl.launch.knot.KnotClient"
        );
        assert!(f.classpath.iter().all(|p| p.exists()));
        p.loader = "forge".into();
        let mut f = base;
        forge(&http, &root, &p, &mut f).await.unwrap();
        assert_eq!(
            f.manifest["mainClass"],
            "net.minecraftforge.bootstrap.ForgeBootstrap"
        );
        assert!(f.classpath.iter().all(|p| p.exists()));
    }
    #[test]
    fn validates_maven_coordinates() {
        assert_eq!(
            maven_path("net.fabricmc:fabric-loader:0.16.14").unwrap(),
            "net/fabricmc/fabric-loader/0.16.14/fabric-loader-0.16.14.jar"
        );
        for n in ["x:../escape:v", "x:y:v:extra", "https://bad"] {
            assert!(maven_path(n).is_err());
        }
    }
}
