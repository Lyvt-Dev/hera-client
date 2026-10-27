//! Opt-in cape sharing. Minecraft tokens go only to Mojang, never to a cape host.
use crate::{
    auth::Session,
    capes,
    storage::{Profile, Result},
};
use base64::{engine::general_purpose::STANDARD, Engine};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use sha2::{Digest, Sha256};
use std::{net::IpAddr, path::Path, time::Duration};
#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Config {
    pub version: u8,
    pub enabled: bool,
    pub base_url: String,
}
impl Default for Config {
    fn default() -> Self {
        Self {
            version: 1,
            enabled: false,
            base_url: "http://127.0.0.1:3847".into(),
        }
    }
}
pub fn validate_url(value: &str) -> Result<String> {
    if value.len() > 512 {
        return Err("Cape-Dienst-Adresse zu lang".into());
    }
    let url = reqwest::Url::parse(value.trim()).map_err(|_| "Ungültige Cape-Dienst-Adresse")?;
    if !url.username().is_empty()
        || url.password().is_some()
        || url.query().is_some()
        || url.fragment().is_some()
        || url.path() != "/"
    {
        return Err("Nur die Basisadresse des Cape-Dienstes angeben".into());
    }
    let host = url.host_str().ok_or("Cape-Dienst benötigt einen Host")?;
    let local = host == "localhost"
        || host
            .trim_matches(['[', ']'])
            .parse::<IpAddr>()
            .is_ok_and(|ip| match ip {
                IpAddr::V4(v) => v.is_private() || v.is_loopback(),
                IpAddr::V6(v) => v.is_loopback(),
            });
    if url.scheme() != "https" && !(url.scheme() == "http" && local) {
        return Err(
            "HTTP ist nur für localhost/LAN-IP-Adressen erlaubt; sonst HTTPS verwenden".into(),
        );
    }
    Ok(url.as_str().trim_end_matches('/').into())
}
fn file(root: &Path, p: &Profile) -> Result<std::path::PathBuf> {
    let _ = capes::read(root, p)?;
    let f = root
        .join("profiles")
        .join(&p.id)
        .join("config/hera/cape-service.json");
    crate::bundled_mods::reject_link(&f)?;
    Ok(f)
}
pub fn read(root: &Path, p: &Profile) -> Result<Config> {
    let f = file(root, p)?;
    if !f.exists() {
        return Ok(Config::default());
    }
    if std::fs::metadata(&f)
        .map_err(|_| "Cape-Dienst-Einstellungen nicht lesbar")?
        .len()
        > 2048
    {
        return Err("Cape-Dienst-Einstellungen zu groß".into());
    }
    let mut c: Config = serde_json::from_slice(
        &std::fs::read(f).map_err(|_| "Cape-Dienst-Einstellungen nicht lesbar")?,
    )
    .map_err(|_| "Cape-Dienst-Einstellungen beschädigt")?;
    if c.version != 1 {
        return Err("Unbekannte Cape-Dienst-Einstellungen".into());
    }
    c.base_url = validate_url(&c.base_url)?;
    Ok(c)
}
pub fn save(root: &Path, p: &Profile, mut c: Config) -> Result<()> {
    let f = file(root, p)?;
    c.version = 1;
    c.base_url = validate_url(&c.base_url)?;
    let parent = f.parent().ok_or("Cape-Dienst-Ziel fehlt")?;
    std::fs::create_dir_all(parent).map_err(|_| "Cape-Dienst konnte nicht gespeichert werden")?;
    let tmp = parent.join(format!(".cape-service-{}.tmp", uuid::Uuid::new_v4()));
    let bytes = serde_json::to_vec(&c).map_err(|_| "Cape-Dienst ungültig")?;
    std::fs::write(&tmp, bytes).map_err(|_| "Cape-Dienst konnte nicht gespeichert werden")?;
    if std::fs::rename(&tmp, f).is_err() {
        let _ = std::fs::remove_file(tmp);
        return Err("Cape-Dienst konnte nicht gespeichert werden".into());
    }
    Ok(())
}
fn client() -> Result<reqwest::Client> {
    reqwest::Client::builder()
        .timeout(Duration::from_secs(15))
        .redirect(reqwest::redirect::Policy::none())
        .build()
        .map_err(|_| "Cape-Verbindung konnte nicht erstellt werden".into())
}
async fn response(mut r: reqwest::Response) -> Result<Value> {
    if !r.status().is_success() {
        return Err(format!("Cape-Dienst: HTTP {}", r.status().as_u16()));
    }
    let mut bytes = Vec::new();
    while let Some(chunk) = r
        .chunk()
        .await
        .map_err(|_| "Cape-Dienst-Antwort fehlgeschlagen")?
    {
        if bytes.len() + chunk.len() > 65536 {
            return Err("Cape-Dienst-Antwort zu groß".into());
        }
        bytes.extend_from_slice(&chunk);
    }
    serde_json::from_slice(&bytes).map_err(|_| "Ungültige Cape-Dienst-Antwort".into())
}
pub async fn probe(c: &Config) -> Result<String> {
    let base = validate_url(&c.base_url)?;
    let v = response(
        client()?
            .get(format!("{base}/health"))
            .send()
            .await
            .map_err(|_| "Cape-Dienst nicht erreichbar")?,
    )
    .await?;
    if v["service"] != "hera-capes" || v["version"] != 1 {
        return Err("Kein kompatibler Hera-Cape-Dienst".into());
    }
    Ok("Cape-Dienst erreichbar".into())
}
pub async fn publish(root: &Path, p: &Profile, session: &Session, remove: bool) -> Result<()> {
    let config = read(root, p)?;
    if !config.enabled {
        return Err("Cape-Dienst zuerst aktivieren und speichern".into());
    }
    let owner = uuid::Uuid::parse_str(&session.account.id)
        .map_err(|_| "Konto-ID ungültig")?
        .simple()
        .to_string();
    let encoded = if remove {
        String::new()
    } else {
        let cape = capes::read(root, p)?.ok_or("Zuerst ein Cape auswählen")?;
        if cape.owner != owner || !cape.enabled {
            return Err("Aktiviere ein lokales Cape für dieses Minecraft-Konto".into());
        }
        capes::normalize_png(&cape.png_base64)?
    };
    let png = STANDARD.decode(&encoded).map_err(|_| "Cape ungültig")?;
    let hash = format!("{:x}", Sha256::digest(&png));
    let http = client()?;
    let challenge=response(http.post(format!("{}/v1/challenges",config.base_url)).json(&json!({"owner":owner,"playerName":session.account.name,"action":if remove{"remove"}else{"publish"},"sha256":hash})).send().await.map_err(|_|"Cape-Dienst nicht erreichbar")?).await?;
    let id = challenge["id"]
        .as_str()
        .filter(|s| s.len() == 48 && s.bytes().all(|b| b.is_ascii_hexdigit()))
        .ok_or("Ungültiger Cape-Nachweis")?;
    let server_id = challenge["serverId"]
        .as_str()
        .filter(|s| s.len() == 40 && s.bytes().all(|b| b.is_ascii_hexdigit()))
        .ok_or("Ungültiger Cape-Nachweis")?;
    // Fixed official destination. This is the only request containing the Minecraft token.
    let joined = http
        .post("https://sessionserver.mojang.com/session/minecraft/join")
        .json(&json!({"accessToken":session.token,"selectedProfile":owner,"serverId":server_id}))
        .send()
        .await
        .map_err(|_| "Minecraft-Kontonachweis nicht erreichbar")?;
    if !joined.status().is_success() {
        return Err("Minecraft-Kontonachweis fehlgeschlagen; bitte erneut anmelden".into());
    }
    response(
        http.post(format!("{}/v1/capes", config.base_url))
            .json(&json!({"challengeId":id,"pngBase64":encoded}))
            .send()
            .await
            .map_err(|_| "Cape-Dienst nicht erreichbar")?,
    )
    .await?;
    Ok(())
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn endpoints_are_origins_and_plain_http_is_local_only() {
        for good in [
            "http://127.0.0.1:3847",
            "http://192.168.1.20:3847",
            "http://localhost:3847",
            "https://capes.example.com",
        ] {
            assert!(validate_url(good).is_ok(), "{good}");
        }
        for bad in [
            "http://example.com",
            "http://169.254.169.254",
            "file:///tmp/a",
            "http://user:pass@localhost",
            "http://localhost/path",
            "http://localhost?token=x",
        ] {
            assert!(validate_url(bad).is_err(), "{bad}");
        }
    }
    #[test]
    fn service_settings_are_opt_in_and_profile_scoped() {
        let root = std::path::PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("../../../.local/cape-service-native-tests");
        let mut p = Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Cape test".into(),
            version: "1.21.11".into(),
            loader: "fabric".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        };
        assert!(!read(&root, &p).unwrap().enabled);
        save(
            &root,
            &p,
            Config {
                version: 1,
                enabled: true,
                base_url: "http://192.168.1.20:3847/".into(),
            },
        )
        .unwrap();
        let loaded = read(&root, &p).unwrap();
        assert!(loaded.enabled);
        assert_eq!(loaded.base_url, "http://192.168.1.20:3847");
        p.id = uuid::Uuid::new_v4().to_string();
        assert!(!read(&root, &p).unwrap().enabled);
        p.id = "../escape".into();
        assert!(read(&root, &p).is_err());
    }
}
