use crate::storage::{Result, Store};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::{
    sync::atomic::{AtomicU64, Ordering},
    time::Duration,
};
use tokio::sync::Mutex;

const LOGIN: &str = "https://login.microsoftonline.com/consumers/oauth2/v2.0";
pub struct Auth {
    pub pending: Mutex<Option<Pending>>,
    pub generation: AtomicU64,
}
pub struct Pending {
    flow: crate::browser_login::Flow,
    generation: u64,
}
#[derive(Serialize, Deserialize, Clone)]
pub struct Account {
    pub id: String,
    pub name: String,
}
pub struct Session {
    pub account: Account,
    pub token: String,
}
fn entry(id: &str) -> Result<keyring::Entry> {
    keyring::Entry::new("gg.hera.client", id)
        .map_err(|_| "Betriebssystem-Schlüsselspeicher nicht verfügbar".into())
}
async fn response(r: reqwest::Response, stage: &str) -> Result<Value> {
    if !r.status().is_success() {
        let status = r.status().as_u16();
        let body: Value = r.json().await.unwrap_or(Value::Null);
        return Err(provider_error(&body, stage, status));
    }
    r.json()
        .await
        .map_err(|_| format!("{stage}: Ungültige Antwort"))
}
fn provider_error(body: &Value, stage: &str, status: u16) -> String {
    if stage == "Minecraft-Anmeldung" && status == 403 {
        let message = body["errorMessage"]
            .as_str()
            .unwrap_or("")
            .to_ascii_lowercase();
        if message.contains("invalid app registration") {
            return "Microsoft und Xbox haben die Anmeldung bestätigt. Minecraft hat die Hera-App jedoch noch nicht für seinen Anmeldedienst freigegeben (MC_APP_NOT_APPROVED). Der Herausgeber muss die Minecraft-App-Freigabe beantragen. Erneutes Einloggen behebt diese Sperre nicht.".into();
        }
        return "Microsoft und Xbox haben die Anmeldung bestätigt. Minecraft lehnt die Kontoübernahme ab (MC_AUTH_FORBIDDEN, HTTP 403). Die Antwort bestätigt keine konkrete Ursache; die App-Freigabe muss geprüft werden.".into();
    }
    let codes = body["error_codes"].as_array();
    let has = |code: u64| codes.is_some_and(|list| list.iter().any(|v| v.as_u64() == Some(code)));
    if has(70002) || has(7000218) {
        return "Microsoft hat die Hera-Anmeldung noch nicht zugelassen (AADSTS70002). Bitte den Hera-Support kontaktieren.".into();
    }
    if has(700016) {
        return "Die Microsoft-App wurde nicht gefunden (AADSTS700016). Bitte die Anwendungs-ID in den erweiterten Einstellungen prüfen.".into();
    }
    if has(50194) {
        return "Die Microsoft-App muss persönliche Microsoft-Konten zulassen (AADSTS50194)."
            .into();
    }
    format!("{stage} fehlgeschlagen (HTTP {status}). Bitte erneut versuchen. Falls der Fehler bleibt, wende dich an den Hera-Support.")
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn distinguishes_app_approval_from_unknown_forbidden_without_leaking_payload() {
        let known = provider_error(
            &json!({"errorMessage":"Invalid app registration, see https://aka.ms/AppRegInfo", "access_token":"private-token"}),
            "Minecraft-Anmeldung",
            403,
        );
        assert!(known.contains("MC_APP_NOT_APPROVED"));
        assert!(!known.contains("private-token"));
        let unknown = provider_error(
            &json!({"errorMessage":"private account detail"}),
            "Minecraft-Anmeldung",
            403,
        );
        assert!(unknown.contains("MC_AUTH_FORBIDDEN"));
        assert!(!unknown.contains("MC_APP_NOT_APPROVED"));
        assert!(!unknown.contains("private account detail"));
        assert!(!provider_error(
            &json!({"errorMessage":"Invalid app registration"}),
            "Minecraft-Besitzprüfung",
            403
        )
        .contains("MC_APP_NOT_APPROVED"));
    }
    #[test]
    fn explains_public_client_configuration_without_echoing_provider_payload() {
        let body = json!({"error_codes":[70002],"error_description":"secret diagnostic payload", "access_token":"must-not-leak"});
        let message = provider_error(&body, "Microsoft-Anmeldung", 401);
        assert!(message.contains("AADSTS70002"));
        assert!(!message.contains("secret"));
        assert!(!message.contains("must-not-leak"));
        assert!(!provider_error(
            &json!({"error_description":"private payload"}),
            "Microsoft",
            500
        )
        .contains("private payload"));
    }
}
fn string<'a>(v: &'a Value, key: &str) -> Result<&'a str> {
    v[key]
        .as_str()
        .ok_or_else(|| format!("Authentifizierung: Feld {key} fehlt"))
}
impl Auth {
    pub async fn begin(&self, app: &tauri::AppHandle, client_id: &str) -> Result<()> {
        let mut pending = self
            .pending
            .try_lock()
            .map_err(|_| "Anmeldung läuft bereits")?;
        let flow = crate::browser_login::begin(app, client_id)?;
        *pending = Some(Pending {
            flow,
            generation: self.generation.fetch_add(1, Ordering::SeqCst) + 1,
        });
        Ok(())
    }
    pub async fn complete(
        &self,
        app: &tauri::AppHandle,
        http: &Client,
        store: &Store,
    ) -> Result<Account> {
        let p = self
            .pending
            .lock()
            .await
            .take()
            .ok_or("Keine aktive Anmeldung")?;
        let received = tokio::time::timeout(Duration::from_secs(600), p.flow.result).await;
        crate::browser_login::close(app);
        let code = received
            .map_err(|_| "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")?
            .map_err(|_| "Anmeldung abgebrochen")??;
        if self.generation.load(Ordering::SeqCst) != p.generation {
            return Err("Anmeldung abgebrochen".into());
        }
        let r = http
            .post(format!("{LOGIN}/token"))
            .timeout(Duration::from_secs(30))
            .form(&[
                ("client_id", p.flow.client_id.as_str()),
                ("grant_type", "authorization_code"),
                ("code", code.as_str()),
                ("redirect_uri", crate::browser_login::REDIRECT),
                ("code_verifier", p.flow.verifier.as_str()),
            ])
            .send()
            .await
            .map_err(|_| {
                "Microsoft ist nicht erreichbar. Bitte deine Internetverbindung prüfen."
            })?;
        let v = response(r, "Microsoft-Anmeldung").await?;
        let session = minecraft_session(http, string(&v, "access_token")?).await?;
        if self.generation.load(Ordering::SeqCst) != p.generation {
            return Err("Anmeldung abgebrochen".into());
        }
        entry(&session.account.id)?
            .set_password(string(&v, "refresh_token")?)
            .map_err(|_| "Anmeldung konnte nicht sicher gespeichert werden")?;
        if let Err(e) = save_account(store, &session.account) {
            let _ = entry(&session.account.id)?.delete_credential();
            return Err(e);
        }
        Ok(session.account)
    }
    pub async fn session(&self, http: &Client, id: &str, client_id: &str) -> Result<Session> {
        uuid::Uuid::parse_str(id).map_err(|_| "Ungültige Account-ID")?;
        let refresh = entry(id)?
            .get_password()
            .map_err(|_| "Bitte erneut mit Microsoft anmelden")?;
        let r = http
            .post(format!("{LOGIN}/token"))
            .form(&[
                ("client_id", client_id),
                ("grant_type", "refresh_token"),
                ("refresh_token", refresh.as_str()),
                ("scope", "XboxLive.signin offline_access"),
            ])
            .send()
            .await
            .map_err(|_| "Microsoft nicht erreichbar")?;
        let v = response(r, "Token-Erneuerung").await?;
        // Persist rotated credentials before later network operations can fail.
        if let Some(refresh) = v["refresh_token"].as_str() {
            entry(id)?
                .set_password(refresh)
                .map_err(|_| "Token-Erneuerung konnte nicht sicher gespeichert werden")?;
        }
        let session = minecraft_session(http, string(&v, "access_token")?).await?;
        if session.account.id != id {
            return Err("Account-Zuordnung stimmt nicht überein".into());
        }
        Ok(session)
    }
}
async fn minecraft_session(http: &Client, ms_token: &str) -> Result<Session> {
    let r=http.post("https://user.auth.xboxlive.com/user/authenticate").header("x-xbl-contract-version","1").json(&json!({"Properties":{"AuthMethod":"RPS","SiteName":"user.auth.xboxlive.com","RpsTicket":format!("d={ms_token}")},"RelyingParty":"http://auth.xboxlive.com","TokenType":"JWT"})).send().await.map_err(|_|"Xbox-Anmeldung nicht erreichbar")?;
    let xbox = response(r, "Xbox-Anmeldung").await?;
    let r=http.post("https://xsts.auth.xboxlive.com/xsts/authorize").header("x-xbl-contract-version","1").json(&json!({"Properties":{"SandboxId":"RETAIL","UserTokens":[string(&xbox,"Token")?]},"RelyingParty":"rp://api.minecraftservices.com/","TokenType":"JWT"})).send().await.map_err(|_|"Xbox-Autorisierung nicht erreichbar")?;
    let xsts = response(r, "Xbox-Autorisierung (ggf. Familieneinstellungen prüfen)").await?;
    let uhs = xsts["DisplayClaims"]["xui"][0]["uhs"]
        .as_str()
        .ok_or("Xbox-Kennung fehlt")?;
    let r = http
        .post("https://api.minecraftservices.com/authentication/login_with_xbox")
        .json(&json!({"identityToken":format!("XBL3.0 x={uhs};{}",string(&xsts,"Token")?)}))
        .send()
        .await
        .map_err(|_| "Minecraft-Anmeldung nicht erreichbar")?;
    let minecraft = response(r, "Minecraft-Anmeldung").await?;
    let token = string(&minecraft, "access_token")?.to_string();
    let r = http
        .get("https://api.minecraftservices.com/entitlements/mcstore")
        .bearer_auth(&token)
        .send()
        .await
        .map_err(|_| "Besitzprüfung nicht erreichbar")?;
    let entitlements = response(r, "Minecraft-Besitzprüfung").await?;
    if !entitlements["items"].as_array().is_some_and(|a| {
        a.iter().any(|v| {
            matches!(
                v["name"].as_str(),
                Some("game_minecraft" | "product_minecraft")
            )
        })
    }) {
        return Err("Dieses Konto besitzt keine nachgewiesene Minecraft Java Edition".into());
    }
    let r = http
        .get("https://api.minecraftservices.com/minecraft/profile")
        .bearer_auth(&token)
        .send()
        .await
        .map_err(|_| "Minecraft-Profil nicht erreichbar")?;
    let p = response(r, "Minecraft-Profil").await?;
    let id = string(&p, "id")?.to_string();
    uuid::Uuid::parse_str(&id).map_err(|_| "Ungültige Minecraft-UUID")?;
    Ok(Session {
        account: Account {
            id,
            name: string(&p, "name")?.to_string(),
        },
        token,
    })
}
fn save_account(store: &Store, a: &Account) -> Result<()> {
    store.0.lock().map_err(|_|"Datenbank gesperrt")?.execute("INSERT INTO accounts(id,name) VALUES(?1,?2) ON CONFLICT(id) DO UPDATE SET name=excluded.name",rusqlite::params![a.id,a.name]).map_err(|e|e.to_string())?;
    Ok(())
}
pub fn accounts(store: &Store) -> Result<Vec<Account>> {
    let c = store.0.lock().map_err(|_| "Datenbank gesperrt")?;
    let mut q = c
        .prepare("SELECT id,name FROM accounts ORDER BY rowid")
        .map_err(|e| e.to_string())?;
    let rows = q
        .query_map([], |r| {
            Ok(Account {
                id: r.get(0)?,
                name: r.get(1)?,
            })
        })
        .map_err(|e| e.to_string())?;
    rows.map(|r| r.map_err(|e| e.to_string())).collect()
}
pub fn logout(store: &Store, id: &str) -> Result<()> {
    match entry(id)?.delete_credential() {
        Ok(()) | Err(keyring::Error::NoEntry) => {}
        Err(_) => {
            return Err("Anmeldung konnte nicht aus dem Schlüsselspeicher entfernt werden".into())
        }
    }
    store
        .0
        .lock()
        .map_err(|_| "Datenbank gesperrt")?
        .execute("DELETE FROM accounts WHERE id=?1", [id])
        .map_err(|e| e.to_string())?;
    Ok(())
}
