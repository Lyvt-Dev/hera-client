//! Minecraft-published runtimes, installed locally with all original legal files.
use crate::{
    minecraft::{self, Artifact, Transfer},
    storage::{Profile, Result},
};
use serde_json::Value;
use std::path::Path;
const INDEX:&str="https://piston-meta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";
fn major(name: &str) -> Option<u64> {
    let name = name.strip_prefix("1.").unwrap_or(name);
    name.chars()
        .take_while(|c| c.is_ascii_digit())
        .collect::<String>()
        .parse()
        .ok()
}
fn select(index: &Value, component: &str, required: u64) -> Result<Artifact> {
    let platform = index["windows-x64"]
        .as_object()
        .ok_or("Windows-Java-Katalog fehlt")?;
    let candidates = platform
        .get(component)
        .and_then(Value::as_array)
        .into_iter()
        .flatten()
        .chain(platform.values().filter_map(Value::as_array).flatten());
    for item in candidates {
        if major(item["version"]["name"].as_str().unwrap_or("")) == Some(required) {
            return serde_json::from_value(item["manifest"].clone())
                .map_err(|_| "Java-Prüfdaten fehlen".into());
        }
    }
    Err(format!("Java {required} ist im Minecraft-Laufzeitkatalog nicht verfügbar. Bitte eine eigene Java-Datei auswählen."))
}
pub async fn resolve(
    http: &reqwest::Client,
    root: &Path,
    p: &Profile,
    manifest: &Value,
    progress: &impl Fn(Transfer),
) -> Result<String> {
    let required = manifest["javaVersion"]["majorVersion"]
        .as_u64()
        .ok_or("Java-Anforderung fehlt")?;
    if p.java_path != "java" && p.java_path != "java.exe" {
        minecraft::check_java_major(&p.java_path, required).await?;
        return Ok(p.java_path.clone());
    }
    if let Some(home) = std::env::var_os("JAVA_HOME") {
        let path = std::path::PathBuf::from(home)
            .join("bin")
            .join(if cfg!(windows) { "java.exe" } else { "java" });
        if minecraft::check_java_major(&path.to_string_lossy(), required)
            .await
            .is_ok()
        {
            return Ok(path.to_string_lossy().into());
        }
    }
    if minecraft::check_java_major(&p.java_path, required)
        .await
        .is_ok()
    {
        return Ok(p.java_path.clone());
    }
    if !cfg!(all(target_os = "windows", target_arch = "x86_64")) {
        return Err(format!(
            "Java {required} fehlt. Bitte eine passende Java-Datei auswählen."
        ));
    }
    let index = minecraft::json(http, INDEX).await?;
    let component = manifest["javaVersion"]["component"].as_str().unwrap_or("");
    let artifact = select(&index, component, required)?;
    if artifact.sha1.len() != 40 || artifact.size > 8 * 1024 * 1024 {
        return Err("Java-Manifestprüfung fehlt".into());
    }
    let runtimes = root.join("runtimes");
    crate::bundled_mods::reject_link(&runtimes)?;
    tokio::fs::create_dir_all(&runtimes)
        .await
        .map_err(|e| e.to_string())?;
    let metadata = runtimes.join(format!("java-{required}-manifest.json"));
    crate::bundled_mods::reject_link(&metadata)?;
    minecraft::download(http, &artifact, &metadata).await?;
    let data = tokio::fs::read(&metadata)
        .await
        .map_err(|e| e.to_string())?;
    let data: Value = serde_json::from_slice(&data).map_err(|_| "Java-Manifest beschädigt")?;
    let files = data["files"].as_object().ok_or("Java-Dateiliste fehlt")?;
    if files.len() > 5000 {
        return Err("Java-Dateiliste zu groß".into());
    }
    let target = runtimes.join(format!("java-{required}-{}", &artifact.sha1[..12]));
    crate::bundled_mods::reject_link(&target)?;
    let mut downloads = vec![];
    let mut total_bytes = 0u64;
    for (relative, entry) in files {
        let path = minecraft::safe_join(&target, relative)?;
        let mut cursor = target.clone();
        for part in std::path::Path::new(relative).components() {
            cursor.push(part);
            crate::bundled_mods::reject_link(&cursor)?;
        }
        match entry["type"].as_str() {
            Some("directory") => tokio::fs::create_dir_all(&path)
                .await
                .map_err(|e| e.to_string())?,
            Some("file") => {
                let file: Artifact = serde_json::from_value(entry["downloads"]["raw"].clone())
                    .map_err(|_| "Java-Dateiprüfung fehlt")?;
                total_bytes = total_bytes
                    .checked_add(file.size)
                    .ok_or("Java-Paket zu groß")?;
                if total_bytes > 1024 * 1024 * 1024 {
                    return Err("Java-Paket zu groß".into());
                }
                downloads.push((file, path));
            }
            _ => return Err("Nicht unterstützter Java-Dateityp".into()),
        }
    }
    let total = downloads.len();
    let mut completed = 0;
    for batch in downloads.chunks(8) {
        let mut jobs = tokio::task::JoinSet::new();
        for (file, path) in batch {
            let http = http.clone();
            let file = file.clone();
            let path = path.clone();
            jobs.spawn(async move { minecraft::download(&http, &file, &path).await });
        }
        while let Some(result) = jobs.join_next().await {
            result.map_err(|_| "Java-Download fehlgeschlagen")??;
            completed += 1;
            progress(Transfer {
                stage: format!("Java {required} vorbereiten"),
                completed,
                total,
            });
        }
    }
    let executable = target.join("bin/java.exe");
    minecraft::check_java_major(&executable.to_string_lossy(), required).await?;
    Ok(executable.to_string_lossy().into())
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn parses_catalog_versions() {
        assert_eq!(major("21.0.7"), Some(21));
        assert_eq!(major("8u51-cacert"), Some(8));
        assert_eq!(major("1.8.0_51"), Some(8));
    }
    #[test]
    fn never_selects_wrong_runtime_major() {
        let catalog = serde_json::json!({"windows-x64":{"java-runtime-delta":[{"version":{"name":"17.0.1"},"manifest":{}}]}});
        assert!(select(&catalog, "java-runtime-delta", 21).is_err());
    }
    #[tokio::test]
    #[ignore = "Downloads and checks official Java 21, without launching Minecraft"]
    async fn java21_install() {
        let root = std::path::PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("../../../.local/install-smoke");
        let p = Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Java smoke".into(),
            version: "1.21.11".into(),
            loader: "fabric".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        };
        let manifest =
            serde_json::json!({"javaVersion":{"majorVersion":21,"component":"java-runtime-delta"}});
        let path = resolve(&minecraft::http().unwrap(), &root, &p, &manifest, &|_| {})
            .await
            .unwrap();
        minecraft::check_java_major(&path, 21).await.unwrap();
        println!("Java 21 resolved and executed -version successfully");
    }
}
