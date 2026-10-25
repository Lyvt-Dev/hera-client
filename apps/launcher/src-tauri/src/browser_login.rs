//! Microsoft authorization-code login. Credentials stay inside the remote sign-in webview.
use crate::storage::Result;
use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine as _};
use sha2::{Digest, Sha256};
use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Manager, WebviewUrl, WebviewWindowBuilder};
use tokio::sync::oneshot;

pub const REDIRECT: &str = "https://login.microsoftonline.com/common/oauth2/nativeclient";
const WINDOW: &str = "microsoft-login";
pub struct Flow {
    pub client_id: String,
    pub verifier: String,
    pub result: oneshot::Receiver<Result<String>>,
}
fn challenge(verifier: &str) -> String {
    URL_SAFE_NO_PAD.encode(Sha256::digest(verifier.as_bytes()))
}
fn callback(url: &reqwest::Url, state: &str) -> Option<Result<String>> {
    if url.scheme() != "https"
        || url.host_str() != Some("login.microsoftonline.com")
        || url.path() != "/common/oauth2/nativeclient"
    {
        return None;
    }
    let query: Vec<_> = url.query_pairs().collect();
    let states: Vec<_> = query.iter().filter(|(key, _)| key == "state").collect();
    if states.len() != 1 || states[0].1 != state {
        return Some(Err(
            "Die Anmeldung konnte nicht sicher zugeordnet werden. Bitte erneut versuchen.".into(),
        ));
    }
    if query.iter().any(|(key, _)| key == "error") {
        return Some(Err(
            "Microsoft-Anmeldung wurde abgebrochen oder abgelehnt.".into()
        ));
    }
    let codes: Vec<_> = query.iter().filter(|(key, _)| key == "code").collect();
    Some(if codes.len() == 1 && !codes[0].1.is_empty() {
        Ok(codes[0].1.to_string())
    } else {
        Err("Microsoft hat die Anmeldung nicht bestätigt. Bitte erneut versuchen.".into())
    })
}
pub fn close(app: &AppHandle) {
    if let Some(window) = app.get_webview_window(WINDOW) {
        let _ = window.close();
    }
}
pub fn begin(app: &AppHandle, client_id: &str) -> Result<Flow> {
    if client_id.is_empty() {
        return Err("Die Microsoft-Anmeldung ist momentan nicht verfügbar.".into());
    }
    if let Some(window) = app.get_webview_window(WINDOW) {
        let _ = window.set_focus();
        return Err("Die Microsoft-Anmeldung ist bereits geöffnet.".into());
    }
    let verifier = format!(
        "{}{}",
        uuid::Uuid::new_v4().simple(),
        uuid::Uuid::new_v4().simple()
    );
    let state = uuid::Uuid::new_v4().simple().to_string();
    let mut url =
        reqwest::Url::parse("https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize")
            .map_err(|_| "Anmeldedienst nicht verfügbar")?;
    url.query_pairs_mut()
        .append_pair("client_id", client_id)
        .append_pair("response_type", "code")
        .append_pair("redirect_uri", REDIRECT)
        .append_pair("response_mode", "query")
        .append_pair("scope", "XboxLive.signin offline_access")
        .append_pair("state", &state)
        .append_pair("code_challenge", &challenge(&verifier))
        .append_pair("code_challenge_method", "S256")
        .append_pair("prompt", "select_account");
    let (tx, result) = oneshot::channel();
    let sender = Arc::new(Mutex::new(Some(tx)));
    let navigation_sender = sender.clone();
    let window = WebviewWindowBuilder::new(app, WINDOW, WebviewUrl::External(url))
        .title("Mit Microsoft anmelden – Hera")
        .inner_size(520.0, 720.0)
        .min_inner_size(420.0, 580.0)
        .center()
        .resizable(true)
        .on_navigation(move |url| {
            if let Some(result) = callback(url, &state) {
                if let Ok(mut sender) = navigation_sender.lock() {
                    if let Some(tx) = sender.take() {
                        let _ = tx.send(result);
                    }
                }
                return false;
            }
            url.scheme() == "https"
        })
        .build()
        .map_err(|_| {
            "Das Microsoft-Anmeldefenster konnte nicht geöffnet werden. Bitte Hera neu starten."
        })?;
    window.on_window_event(move |event| {
        if matches!(event, tauri::WindowEvent::Destroyed) {
            if let Ok(mut sender) = sender.lock() {
                if let Some(tx) = sender.take() {
                    let _ = tx.send(Err("Anmeldung abgebrochen".into()));
                }
            }
        }
    });
    let _ = window.set_focus();
    Ok(Flow {
        client_id: client_id.into(),
        verifier,
        result,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn pkce_matches_rfc7636_example() {
        assert_eq!(
            challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        );
    }
    #[test]
    fn callbacks_reject_forged_state_duplicate_codes_and_lookalike_origins() {
        let check = |suffix: &str| {
            callback(
                &reqwest::Url::parse(&format!("{REDIRECT}{suffix}")).unwrap(),
                "expected",
            )
            .unwrap()
        };
        assert_eq!(check("?state=expected&code=valid").unwrap(), "valid");
        assert!(check("?state=wrong&code=valid").is_err());
        assert!(check("?state=expected&state=wrong&code=valid").is_err());
        assert!(check("?state=expected&code=a&code=b").is_err());
        assert!(check("?state=expected&error=access_denied").is_err());
        assert!(callback(&reqwest::Url::parse("https://login.microsoftonline.com.evil.example/common/oauth2/nativeclient?state=expected&code=valid").unwrap(), "expected").is_none());
    }
}
