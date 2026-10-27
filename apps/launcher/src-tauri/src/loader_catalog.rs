use crate::{
    minecraft,
    storage::{valid_version_id, Result},
};
use serde::Serialize;
#[derive(Serialize)]
pub struct Options {
    pub fabric: Option<String>,
    pub forge: Option<String>,
}
fn coordinate(value: &str) -> bool {
    !value.is_empty()
        && value.len() < 120
        && value
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b"._+-".contains(&b))
}
pub fn forge_versions(xml: &str, game: &str) -> Vec<String> {
    let prefix = format!("{game}-");
    xml.split("<version>")
        .skip(1)
        .filter_map(|s| s.split_once("</version>").map(|v| v.0))
        .filter(|s| s.starts_with(&prefix) && coordinate(s))
        .map(str::to_string)
        .collect()
}
fn numeric(value: &str) -> Vec<u64> {
    value
        .split(|c: char| !c.is_ascii_digit())
        .filter_map(|p| p.parse().ok())
        .collect()
}
pub async fn fabric(http: &reqwest::Client, game: &str) -> Result<Option<String>> {
    let r = http
        .get(format!(
            "https://meta.fabricmc.net/v2/versions/loader/{game}"
        ))
        .send()
        .await
        .map_err(|_| "Fabric-Katalog nicht erreichbar")?;
    if r.status() == 404 {
        return Ok(None);
    }
    if !r.status().is_success() {
        return Err("Fabric-Katalog nicht verfügbar".into());
    }
    let data: serde_json::Value =
        serde_json::from_slice(&minecraft::bounded_body(r, 4 * 1024 * 1024).await?)
            .map_err(|_| "Fabric-Katalog beschädigt")?;
    Ok(data
        .as_array()
        .ok_or("Fabric-Versionen fehlen")?
        .iter()
        .find(|v| v["loader"]["stable"].as_bool() == Some(true))
        .and_then(|v| v["loader"]["version"].as_str())
        .filter(|v| coordinate(v))
        .map(str::to_string))
}
pub async fn forge(http: &reqwest::Client, game: &str) -> Result<Option<String>> {
    let r = http
        .get("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml")
        .send()
        .await
        .map_err(|_| "Forge-Katalog nicht erreichbar")?;
    if !r.status().is_success() {
        return Err("Forge-Katalog nicht verfügbar".into());
    }
    let xml = String::from_utf8(minecraft::bounded_body(r, 8 * 1024 * 1024).await?)
        .map_err(|_| "Forge-Katalog beschädigt")?;
    let mut versions = forge_versions(&xml, game);
    if versions.is_empty() {
        return Ok(None);
    }
    versions.sort_by_key(|v| numeric(v));
    let promos = minecraft::json(
        http,
        "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json",
    )
    .await?;
    let build = promos["promos"][format!("{game}-recommended")]
        .as_str()
        .or_else(|| promos["promos"][format!("{game}-latest")].as_str());
    let preferred = build
        .and_then(|b| {
            versions.iter().find(|v| {
                v.as_str() == format!("{game}-{b}") || v.starts_with(&format!("{game}-{b}-"))
            })
        })
        .cloned()
        .or_else(|| versions.last().cloned());
    if let Some(ref release) = preferred {
        let url=format!("https://maven.minecraftforge.net/net/minecraftforge/forge/{release}/forge-{release}-installer.jar");
        let r = http
            .head(url)
            .send()
            .await
            .map_err(|_| "Forge-Installer nicht erreichbar")?;
        if r.status() == 404 {
            return Ok(None);
        }
        if !r.status().is_success() {
            return Err("Forge-Installer nicht verfügbar".into());
        }
    }
    Ok(preferred)
}
pub async fn options(http: &reqwest::Client, game: &str) -> Result<Options> {
    if !valid_version_id(game) {
        return Err("Ungültige Minecraft-Version".into());
    }
    let (fabric, forge) = tokio::try_join!(fabric(http, game), forge(http, game))?;
    Ok(Options { fabric, forge })
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn matches_exact_game_not_prefix() {
        let xml="<version>1.21.1-52.1.20</version><version>1.21.11-61.2.4</version><version>1.21.1-../../bad</version>";
        assert_eq!(forge_versions(xml, "1.21.1"), vec!["1.21.1-52.1.20"]);
    }
    #[test]
    fn sorts_build_numbers_numerically() {
        assert!(numeric("1.20.1-47.4.10") > numeric("1.20.1-47.4.9"));
    }
}
