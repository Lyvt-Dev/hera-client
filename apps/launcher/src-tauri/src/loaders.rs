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

pub async fn install(
    http: &Client,
    root: &Path,
    p: &Profile,
    progress: &impl Fn(Transfer),
) -> Result<Installation> {
    let mut base = minecraft::install(http, root, p, progress).await?;
    let mut effective = p.clone();
    effective.java_path =
        crate::java_runtime::resolve(http, root, p, &base.manifest, progress).await?;
    base.java = effective.java_path.clone();
    let p = &effective;
    match p.loader.as_str() {
        "vanilla" => {}
        "fabric" => fabric(http, root, p, &mut base).await?,
        "forge" => forge(http, root, p, &mut base).await?,
        _ => return Err("Unbekannter Loader".into()),
    }
    crate::bundled_mods::install(root, p, progress).await?;
    Ok(base)
}
fn maven_path(name: &str) -> Result<String> {
    let parts = name.split(':').collect::<Vec<_>>();
    if !(3..=4).contains(&parts.len())
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
        "{}/{}/{}/{}-{}{}.jar",
        parts[0].replace('.', "/"),
        parts[1],
        parts[2],
        parts[1],
        parts[2],
        parts.get(3).map(|v| format!("-{v}")).unwrap_or_default()
    ))
}
fn merge(
    base: &mut Installation,
    overlay: &Value,
    paths: Vec<PathBuf>,
    library_root: PathBuf,
    expected_version: &str,
) -> Result<()> {
    if overlay["inheritsFrom"] != expected_version && overlay["jar"] != expected_version {
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
    if let Some(legacy) = overlay["minecraftArguments"].as_str() {
        base.manifest["minecraftArguments"] = Value::String(legacy.into());
    }
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
    let loader = crate::loader_catalog::fabric(http, &p.version)
        .await?
        .ok_or("Für diese Minecraft-Version wurde kein Fabric-Loader veröffentlicht")?;
    let meta = minecraft::json(
        http,
        &format!(
            "https://meta.fabricmc.net/v2/versions/loader/{}/{loader}/profile/json",
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
    merge(base, &meta, paths, lib, &p.version)
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
fn installer_metadata(path: &Path, game: &str) -> Result<(String, Value)> {
    use std::io::Read;
    fn read(zip: &mut zip::ZipArchive<std::fs::File>, name: &str) -> Result<Value> {
        let mut file = zip
            .by_name(name)
            .map_err(|_| "Forge-Installer enthält keine Versionsmetadaten")?;
        if file.size() > 4 * 1024 * 1024 {
            return Err("Forge-Metadaten zu groß".into());
        }
        let mut data = vec![];
        file.read_to_end(&mut data).map_err(|e| e.to_string())?;
        serde_json::from_slice(&data).map_err(|_| "Forge-Installer-Metadaten beschädigt".into())
    }
    let mut zip = zip::ZipArchive::new(std::fs::File::open(path).map_err(|e| e.to_string())?)
        .map_err(|_| "Forge-Installer beschädigt")?;
    let install = read(&mut zip, "install_profile.json")?;
    let mut meta = if install["versionInfo"].is_object() {
        install["versionInfo"].clone()
    } else {
        let entry = install["json"]
            .as_str()
            .unwrap_or("version.json")
            .trim_start_matches('/');
        if entry.contains("..") {
            return Err("Unsichere Forge-Metadaten".into());
        }
        read(&mut zip, entry)?
    };
    let expected = install["minecraft"]
        .as_str()
        .or_else(|| meta["inheritsFrom"].as_str())
        .or_else(|| meta["jar"].as_str())
        .ok_or("Forge-Spielversion fehlt")?;
    if expected != game {
        return Err("Forge-Installer gehört zu einer anderen Minecraft-Version".into());
    }
    if meta["inheritsFrom"].is_null() {
        meta["inheritsFrom"] = Value::String(game.into());
    }
    let id = meta["id"]
        .as_str()
        .ok_or("Forge-Versions-ID fehlt")?
        .to_string();
    if !crate::storage::valid_version_id(&id) {
        return Err("Unsichere Forge-Versions-ID".into());
    }
    Ok((id, meta))
}
fn legacy_embedded_artifact(installer: &Path, coordinate: &str) -> Result<Option<Artifact>> {
    use sha1::Digest;
    use std::io::Read;
    let mut zip = zip::ZipArchive::new(std::fs::File::open(installer).map_err(|e| e.to_string())?)
        .map_err(|_| "Forge-Archiv beschädigt")?;
    let data = {
        let mut entry = zip
            .by_name("install_profile.json")
            .map_err(|_| "Forge-Installationsdaten fehlen")?;
        if entry.size() > 4 * 1024 * 1024 {
            return Err("Forge-Installationsdaten zu groß".into());
        }
        let mut data = vec![];
        entry.read_to_end(&mut data).map_err(|e| e.to_string())?;
        data
    };
    let profile: Value =
        serde_json::from_slice(&data).map_err(|_| "Forge-Installationsdaten beschädigt")?;
    if profile["install"]["path"] != coordinate {
        return Ok(None);
    }
    let name = profile["install"]["filePath"]
        .as_str()
        .ok_or("Eingebettete Forge-Datei fehlt")?;
    let mut file = zip
        .by_name(name)
        .map_err(|_| "Eingebettete Forge-Datei fehlt")?;
    if file.size() > 128 * 1024 * 1024 {
        return Err("Forge-Datei zu groß".into());
    }
    let mut data = vec![];
    file.read_to_end(&mut data).map_err(|e| e.to_string())?;
    Ok(Some(Artifact {
        url: String::new(),
        sha1: format!("{:x}", sha1::Sha1::digest(&data)),
        size: data.len() as u64,
        path: None,
    }))
}
fn extract_legacy_install(
    installer: &Path,
    runtime: &Path,
    version_file: &Path,
    meta: &Value,
) -> Result<bool> {
    use std::io::Read;
    let mut zip = zip::ZipArchive::new(std::fs::File::open(installer).map_err(|e| e.to_string())?)
        .map_err(|_| "Forge-Archiv beschädigt")?;
    let profile: Value = {
        let mut file = zip
            .by_name("install_profile.json")
            .map_err(|_| "Forge-Metadaten fehlen")?;
        if file.size() > 4 * 1024 * 1024 {
            return Err("Forge-Metadaten zu groß".into());
        }
        let mut bytes = vec![];
        file.read_to_end(&mut bytes).map_err(|e| e.to_string())?;
        serde_json::from_slice(&bytes).map_err(|_| "Forge-Metadaten beschädigt")?
    };
    if !profile["versionInfo"].is_object() {
        return Ok(false);
    }
    let coordinate = profile["install"]["path"]
        .as_str()
        .ok_or("Legacy-Forge-Koordinate fehlt")?;
    let source = profile["install"]["filePath"]
        .as_str()
        .ok_or("Legacy-Forge-Datei fehlt")?;
    let target = minecraft::safe_join(&runtime.join("libraries"), &maven_path(coordinate)?)?;
    crate::bundled_mods::reject_link(&target)?;
    let mut file = zip
        .by_name(source)
        .map_err(|_| "Legacy-Forge-Datei fehlt im geprüften Archiv")?;
    if file.size() > 128 * 1024 * 1024 {
        return Err("Legacy-Forge-Datei zu groß".into());
    }
    let mut bytes = vec![];
    file.read_to_end(&mut bytes).map_err(|e| e.to_string())?;
    std::fs::create_dir_all(target.parent().ok_or("Forge-Ziel fehlt")?)
        .map_err(|e| e.to_string())?;
    std::fs::write(target, bytes).map_err(|e| e.to_string())?;
    std::fs::create_dir_all(version_file.parent().ok_or("Forge-Version fehlt")?)
        .map_err(|e| e.to_string())?;
    std::fs::write(
        version_file,
        serde_json::to_vec(meta).map_err(|e| e.to_string())?,
    )
    .map_err(|e| e.to_string())?;
    Ok(true)
}
async fn forge(http: &Client, root: &Path, p: &Profile, base: &mut Installation) -> Result<()> {
    let release = crate::loader_catalog::forge(http, &p.version)
        .await?
        .ok_or("Für diese Minecraft-Version ist kein offizieller Forge-Installer verfügbar")?;
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
    let (version_id, installer_meta) = installer_metadata(&installer, &p.version)?;
    let version_file = runtime
        .join("versions")
        .join(&version_id)
        .join(format!("{version_id}.json"));
    if !version_file.exists()
        && !extract_legacy_install(&installer, &runtime, &version_file, &installer_meta)?
    {
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
    let mut meta: Value =
        serde_json::from_slice(&bytes).map_err(|_| "Ungültige Forge-Metadaten")?;
    if meta["inheritsFrom"].is_null() {
        meta["inheritsFrom"] = installer_meta["inheritsFrom"].clone();
    }
    if meta["id"] != version_id
        || !matches!(
            meta["mainClass"].as_str(),
            Some(
                "net.minecraftforge.bootstrap.ForgeBootstrap"
                    | "cpw.mods.bootstraplauncher.BootstrapLauncher"
                    | "cpw.mods.modlauncher.Launcher"
                    | "net.minecraft.launchwrapper.Launch"
            )
        )
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
        let embedded = if !a.is_object() {
            legacy_embedded_artifact(&installer, item["name"].as_str().unwrap_or(""))?
        } else {
            None
        };
        let artifact: Artifact = if let Some(artifact) = embedded {
            artifact
        } else if a.is_object() {
            serde_json::from_value(a.clone()).map_err(|_| "Forge-Dateiintegritätsdaten fehlen")?
        } else {
            let repository = item["url"].as_str().unwrap_or(
                if item["name"]
                    .as_str()
                    .unwrap_or("")
                    .starts_with("net.minecraftforge:")
                {
                    "https://maven.minecraftforge.net/"
                } else {
                    "https://libraries.minecraft.net/"
                },
            );
            let repository = repository
                .replace(
                    "http://files.minecraftforge.net/maven/",
                    "https://maven.minecraftforge.net/",
                )
                .replace(
                    "http://libraries.minecraft.net/",
                    "https://libraries.minecraft.net/",
                );
            let url = format!("{}/{rel}", repository.trim_end_matches('/'));
            let hash = match checksum(http, &format!("{url}.sha1")).await {
                Ok(hash) => hash,
                Err(_) => item["checksums"]
                    .as_array()
                    .and_then(|values| {
                        values.iter().filter_map(Value::as_str).find(|hash| {
                            hash.len() == 40 && hash.bytes().all(|b| b.is_ascii_hexdigit())
                        })
                    })
                    .map(str::to_lowercase)
                    .ok_or("Veröffentlichte Prüfsumme für eine Legacy-Forge-Bibliothek fehlt")?,
            };
            let size = content_length(http, &url).await?;
            Artifact {
                url,
                sha1: hash,
                size,
                path: Some(rel.clone()),
            }
        };
        if !minecraft::valid(&path, &artifact).await {
            if artifact.url.is_empty() {
                return Err("Generierte Forge-Datei fehlt oder ist beschädigt. Forge-Runtime muss neu installiert werden.".into());
            }
            minecraft::download(http, &artifact, &path).await?;
        }
        paths.push(path);
    }
    merge(base, &meta, paths, lib, &p.version)
}
#[cfg(test)]
mod tests {
    use super::*;
    #[tokio::test]
    #[ignore = "Installs official legacy Forge 1.8.9; does not start gameplay"]
    async fn forge_189_install() {
        let root = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../../.local/install-smoke");
        let p = Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Forge 1.8.9 integration".into(),
            version: "1.8.9".into(),
            loader: "forge".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        };
        let i = install(&minecraft::http().unwrap(), &root, &p, &|_| {})
            .await
            .unwrap();
        assert_eq!(
            i.manifest["mainClass"],
            "net.minecraft.launchwrapper.Launch"
        );
        println!(
            "Legacy Forge 1.8.9 installed with {} verified classpath entries",
            i.classpath.len()
        );
    }
    #[tokio::test]
    #[ignore = "Downloads and installs multiple official loaders; does not enter authenticated gameplay"]
    async fn loader_version_matrix() {
        let root = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../../.local/install-smoke");
        let http = minecraft::http().unwrap();
        for (loader, game) in [
            ("fabric", "1.16.5"),
            ("forge", "1.12.2"),
            ("forge", "1.20.1"),
            ("forge", "1.21.11"),
        ] {
            println!("Installing {loader} {game}");
            let p = Profile {
                id: uuid::Uuid::new_v4().to_string(),
                name: "Loader matrix".into(),
                version: game.into(),
                loader: loader.into(),
                memory_mb: 4096,
                java_path: "java".into(),
            };
            let installed = install(&http, &root, &p, &|_| {})
                .await
                .unwrap_or_else(|error| panic!("{loader} {game}: {error}"));
            assert!(installed.classpath.iter().all(|path| path.exists()));
            println!(
                "Installed {loader} {game} with {} verified classpath entries",
                installed.classpath.len()
            );
        }
    }
    #[tokio::test]
    #[ignore = "Downloads official Minecraft/Fabric files; does not authenticate or launch gameplay"]
    async fn fabric_1211_bundled_install() {
        let root = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../../.local/install-smoke");
        let p = Profile {
            id: "b631051d-0a9a-435e-a957-62415106e711".into(),
            name: "Hera 1.21.11 integration".into(),
            version: "1.21.11".into(),
            loader: "fabric".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        };
        let http = minecraft::http().unwrap();
        let installation = install(&http, &root, &p, &|_| {}).await.unwrap();
        assert_eq!(
            installation.manifest["mainClass"],
            "net.fabricmc.loader.impl.launch.knot.KnotClient"
        );
        assert!(installation.classpath.iter().all(|path| path.exists()));
        assert!(installation
            .game
            .join("mods")
            .join(format!(
                "hera-fabric-1.21.11-{}.jar",
                crate::bundled_mods::MOD_VERSION
            ))
            .exists());
        assert!(installation
            .game
            .join("mods/fabric-api-0.141.6+1.21.11.jar")
            .exists());
        println!("Fabric 1.21.11 plus bundled Hera and Fabric API installed; authenticated gameplay not exercised.");
    }
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
        for n in ["x:../escape:v", "x:y:v:extra:bad", "https://bad"] {
            assert!(maven_path(n).is_err());
        }
    }
}
