//! Publisher gateway only. The CurseForge API key stays on the gateway server.
use crate::storage::Result;
use serde::{Deserialize, Serialize};
use std::path::Path;
#[derive(Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct Config {
    pub gateway_url: String,
}
pub fn normalize(value: &str) -> Result<String> {
    if value.trim().is_empty() {
        return Ok(String::new());
    }
    if value.len() > 512 {
        return Err("Gateway-Adresse zu lang".into());
    }
    let url = reqwest::Url::parse(value.trim()).map_err(|_| "Ungültige Gateway-Adresse")?;
    if !(url.scheme() == "https"
        || url.scheme() == "http"
            && matches!(url.host_str(), Some("localhost" | "127.0.0.1" | "[::1]")))
        || url.host_str().is_none()
        || !url.username().is_empty()
        || url.password().is_some()
        || url.query().is_some()
        || url.fragment().is_some()
    {
        return Err(
            "Gateway benötigt HTTPS oder lokales HTTP, ohne Zugangsdaten oder URL-Parameter".into(),
        );
    }
    Ok(url.as_str().trim_end_matches('/').into())
}
fn path(root: &Path) -> Result<std::path::PathBuf> {
    let p = root.join("catalog.json");
    if std::fs::symlink_metadata(&p).is_ok_and(|m| m.file_type().is_symlink()) {
        return Err("Ungültige Katalogkonfiguration".into());
    }
    Ok(p)
}
pub fn read(root: &Path) -> Result<Config> {
    let p = path(root)?;
    if !p.exists() {
        return Ok(Config::default());
    }
    if std::fs::metadata(&p)
        .map_err(|_| "Konfiguration nicht lesbar")?
        .len()
        > 4096
    {
        return Err("Konfiguration zu groß".into());
    }
    let data = std::fs::read(p).map_err(|_| "Konfiguration nicht lesbar")?;
    let c: Config = serde_json::from_slice(&data).map_err(|_| "Konfiguration beschädigt")?;
    Ok(Config {
        gateway_url: normalize(&c.gateway_url)?,
    })
}
pub fn write(root: &Path, c: Config) -> Result<()> {
    let p = path(root)?;
    let c = Config {
        gateway_url: normalize(&c.gateway_url)?,
    };
    std::fs::write(
        p,
        serde_json::to_vec(&c).map_err(|_| "Konfiguration ungültig")?,
    )
    .map_err(|_| "Katalogkonfiguration konnte nicht gespeichert werden".into())
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn rejects_credentials_and_remote_http() {
        for u in [
            "http://example.com/api/catalog",
            "https://user:key@example.com/api/catalog",
            "https://example.com/api/catalog?key=secret",
        ] {
            assert!(normalize(u).is_err());
        }
        assert_eq!(
            normalize("http://127.0.0.1:3101/api/catalog/").unwrap(),
            "http://127.0.0.1:3101/api/catalog"
        );
    }
    #[test]
    fn persists_only_gateway() {
        let root = std::env::temp_dir().join(uuid::Uuid::new_v4().to_string());
        std::fs::create_dir(&root).unwrap();
        write(
            &root,
            Config {
                gateway_url: "https://catalog.example/api/catalog".into(),
            },
        )
        .unwrap();
        assert_eq!(
            read(&root).unwrap().gateway_url,
            "https://catalog.example/api/catalog"
        );
        std::fs::remove_file(root.join("catalog.json")).unwrap();
        std::fs::remove_dir(root).unwrap();
    }
}
