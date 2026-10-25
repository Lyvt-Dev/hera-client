use crate::{storage::Result, Native};
use serde::Serialize;
use std::sync::atomic::Ordering;
use tauri::{AppHandle, Emitter, State};
use tauri_plugin_updater::{Update, UpdaterExt};
use tokio::sync::Mutex;

#[derive(Default)]
pub struct Pending(pub Mutex<Option<Update>>);
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Status {
    configured: bool,
    current: String,
    version: Option<String>,
    notes: Option<String>,
}
fn endpoint() -> Result<Option<reqwest::Url>> {
    let Some(value) = option_env!("HERA_UPDATE_URL")
        .unwrap_or("https://github.com/Lyvt-Dev/hera-client/releases/latest/download/latest.json")
        .split_whitespace()
        .next()
    else {
        return Ok(None);
    };
    let url = reqwest::Url::parse(value).map_err(|_| "Ungültiger Update-Endpunkt")?;
    if url.scheme() != "https"
        || url.host_str().is_none()
        || !url.username().is_empty()
        || url.password().is_some()
    {
        return Err("Updates benötigen eine HTTPS-Adresse ohne Zugangsdaten".into());
    }
    Ok(Some(url))
}
#[tauri::command]
pub async fn check_update(app: AppHandle, pending: State<'_, Pending>) -> Result<Status> {
    let mut slot = pending
        .0
        .try_lock()
        .map_err(|_| "Update-Vorgang läuft bereits")?;
    *slot = None;
    let current = app.package_info().version.to_string();
    let Some(url) = endpoint()? else {
        return Ok(Status {
            configured: false,
            current,
            version: None,
            notes: None,
        });
    };
    let update = app
        .updater_builder()
        .endpoints(vec![url])
        .map_err(|e| e.to_string())?
        .timeout(std::time::Duration::from_secs(30))
        .build()
        .map_err(|e| e.to_string())?
        .check()
        .await
        .map_err(|e| format!("Update-Prüfung fehlgeschlagen: {e}"))?;
    let status = Status {
        configured: true,
        current,
        version: update.as_ref().map(|u| u.version.clone()),
        notes: update.as_ref().and_then(|u| u.body.clone()),
    };
    *slot = update;
    Ok(status)
}
#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct Progress {
    downloaded: u64,
    total: Option<u64>,
    stage: &'static str,
}
#[tauri::command]
pub async fn install_update(
    app: AppHandle,
    pending: State<'_, Pending>,
    native: State<'_, Native>,
) -> Result<()> {
    let _operation = native
        .operation
        .try_lock()
        .map_err(|_| "Installation oder Start läuft bereits")?;
    if native.pid.load(Ordering::SeqCst) != 0 {
        return Err("Minecraft vor dem Hera-Update beenden".into());
    }
    let mut slot = pending
        .0
        .try_lock()
        .map_err(|_| "Update-Vorgang läuft bereits")?;
    let update = slot.take().ok_or("Zuerst nach Updates suchen")?;
    let mut downloaded = 0;
    let result = update
        .download_and_install(
            |chunk, total| {
                downloaded += chunk as u64;
                let _ = app.emit(
                    "update-progress",
                    Progress {
                        downloaded,
                        total,
                        stage: "Update herunterladen",
                    },
                );
            },
            || {
                let _ = app.emit(
                    "update-progress",
                    Progress {
                        downloaded: 0,
                        total: None,
                        stage: "Signatur prüfen und installieren",
                    },
                );
            },
        )
        .await;
    if let Err(e) = result {
        return Err(format!(
            "Update fehlgeschlagen: {e}. Bitte erneut nach Updates suchen."
        ));
    }
    app.restart();
}
