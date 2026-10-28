mod auth;
mod browser_login;
mod bundled_mods;
mod cape_service;
mod capes;
mod catalog_config;
mod java_runtime;
mod loader_catalog;
mod loaders;
mod media;
mod minecraft;
mod mods;
mod storage;
mod updates;
use auth::{Account, Auth};
use std::{
    path::PathBuf,
    sync::{
        atomic::{AtomicU32, Ordering},
        Arc,
    },
};
use storage::{Profile, Result, Settings, Store};
use tauri::{AppHandle, Emitter, Manager, State};
use tokio::sync::{oneshot, Mutex};

struct Native {
    store: Store,
    root: PathBuf,
    http: reqwest::Client,
    auth: Auth,
    operation: Mutex<()>,
    game: Mutex<Option<oneshot::Sender<()>>>,
    pid: Arc<AtomicU32>,
}
#[tauri::command]
fn catalog_config(s: State<Native>) -> Result<catalog_config::Config> {
    catalog_config::read(&s.root)
}
#[tauri::command]
fn save_catalog_config(s: State<Native>, config: catalog_config::Config) -> Result<()> {
    catalog_config::write(&s.root, config)
}
#[tauri::command]
async fn probe_catalog_config(s: State<'_, Native>, gateway_url: String) -> Result<bool> {
    let url = catalog_config::normalize(&gateway_url)?;
    if url.is_empty() {
        return Err("Gateway-Adresse fehlt".into());
    }
    let p = std::collections::HashMap::from([("source".into(), "curseforge".into())]);
    let value = mods::catalog_with_gateway(&s.http, "status", &p, Some(&url)).await?;
    value["curseforgeConfigured"]
        .as_bool()
        .ok_or_else(|| "Ungültige Gateway-Antwort".into())
}
#[tauri::command]
fn game_running(s: State<Native>) -> bool {
    s.pid.load(Ordering::SeqCst) != 0
}
#[tauri::command]
fn local_cape(s: State<Native>, id: String) -> Result<Option<capes::Cape>> {
    capes::read(&s.root, &s.store.profile(&id)?)
}
#[tauri::command]
async fn save_local_cape(
    s: State<'_, Native>,
    id: String,
    account_id: String,
    png_base64: String,
) -> Result<()> {
    let _guard = s
        .operation
        .try_lock()
        .map_err(|_| "Ein Vorgang läuft bereits")?;
    let account = auth::accounts(&s.store)?
        .into_iter()
        .find(|a| a.id == account_id)
        .ok_or("Bitte ein verbundenes Minecraft-Konto auswählen")?;
    let cape = capes::Cape {
        version: 1,
        owner: uuid::Uuid::parse_str(&account.id)
            .map_err(|_| "Konto-ID ungültig")?
            .simple()
            .to_string(),
        enabled: true,
        png_base64: capes::normalize_png(&png_base64)?,
    };
    capes::write(&s.root, &s.store.profile(&id)?, &cape)
}
#[tauri::command]
async fn set_local_cape_enabled(s: State<'_, Native>, id: String, enabled: bool) -> Result<()> {
    let _guard = s
        .operation
        .try_lock()
        .map_err(|_| "Ein Vorgang läuft bereits")?;
    let p = s.store.profile(&id)?;
    let mut cape = capes::read(&s.root, &p)?.ok_or("Zuerst eine Cape-PNG auswählen")?;
    cape.enabled = enabled;
    capes::write(&s.root, &p, &cape)
}
#[tauri::command]
async fn remove_local_cape(s: State<'_, Native>, id: String) -> Result<()> {
    let _guard = s
        .operation
        .try_lock()
        .map_err(|_| "Ein Vorgang läuft bereits")?;
    capes::remove(&s.root, &s.store.profile(&id)?)
}
#[tauri::command]
fn cape_service_config(s: State<Native>, id: String) -> Result<cape_service::Config> {
    cape_service::read(&s.root, &s.store.profile(&id)?)
}
#[tauri::command]
async fn save_cape_service(
    s: State<'_, Native>,
    id: String,
    config: cape_service::Config,
) -> Result<()> {
    let _guard = s
        .operation
        .try_lock()
        .map_err(|_| "Ein Vorgang läuft bereits")?;
    cape_service::save(&s.root, &s.store.profile(&id)?, config)
}
#[tauri::command]
async fn probe_cape_service(config: cape_service::Config) -> Result<String> {
    cape_service::probe(&config).await
}
#[tauri::command]
async fn publish_cape(
    s: State<'_, Native>,
    id: String,
    account_id: String,
    remove: bool,
) -> Result<()> {
    let _guard = s
        .operation
        .try_lock()
        .map_err(|_| "Ein Vorgang läuft bereits")?;
    let p = s.store.profile(&id)?;
    let session = s
        .auth
        .session(&s.http, &account_id, &s.store.client_id()?)
        .await?;
    cape_service::publish(&s.root, &p, &session, remove).await
}
#[tauri::command]
fn profiles(s: State<Native>) -> Result<Vec<Profile>> {
    s.store.profiles()
}
#[tauri::command]
fn save_profile(s: State<Native>, profile: Profile) -> Result<()> {
    s.store.save(&profile)
}
#[tauri::command]
fn delete_profile(s: State<Native>, id: String) -> Result<()> {
    s.store.delete(&id)
}
#[tauri::command]
fn settings(s: State<Native>) -> Result<Settings> {
    s.store.settings()
}
#[tauri::command]
fn save_settings(s: State<Native>, settings: Settings) -> Result<()> {
    s.store.save_settings(&settings)
}
#[derive(serde::Serialize)]
#[serde(rename_all = "camelCase")]
struct Runtime {
    java: Option<String>,
    platform: String,
    auth_configured: bool,
}
#[tauri::command]
async fn runtime(s: State<'_, Native>) -> Result<Runtime> {
    let mut java = None;
    let mut candidates = vec![];
    if let Some(home) = std::env::var_os("JAVA_HOME") {
        candidates.push(
            PathBuf::from(home)
                .join("bin")
                .join(if cfg!(windows) { "java.exe" } else { "java" })
                .to_string_lossy()
                .to_string(),
        );
    }
    candidates.push("java".into());
    for path in candidates {
        if tokio::process::Command::new(&path)
            .arg("-version")
            .output()
            .await
            .is_ok_and(|o| o.status.success())
        {
            java = Some(path);
            break;
        }
    }
    Ok(Runtime {
        java,
        platform: format!("{} / {}", std::env::consts::OS, std::env::consts::ARCH),
        auth_configured: !s.store.client_id()?.is_empty(),
    })
}
#[tauri::command]
async fn versions(s: State<'_, Native>) -> Result<serde_json::Value> {
    let index = minecraft::json(&s.http, minecraft::MANIFEST).await?;
    Ok(index["versions"].clone())
}
#[tauri::command]
fn accounts(s: State<Native>) -> Result<Vec<Account>> {
    auth::accounts(&s.store)
}
#[tauri::command]
async fn loader_options(s: State<'_, Native>, version: String) -> Result<loader_catalog::Options> {
    loader_catalog::options(&s.http, &version).await
}
#[tauri::command]
async fn begin_login(s: State<'_, Native>, app: AppHandle) -> Result<()> {
    s.auth.begin(&app, &s.store.client_id()?).await
}
#[tauri::command]
async fn cancel_login(s: State<'_, Native>, app: AppHandle) -> Result<()> {
    browser_login::close(&app);
    s.auth
        .generation
        .fetch_add(1, std::sync::atomic::Ordering::SeqCst);
    *s.auth.pending.lock().await = None;
    Ok(())
}
#[tauri::command]
async fn complete_login(s: State<'_, Native>, app: AppHandle) -> Result<Account> {
    s.auth.complete(&app, &s.http, &s.store).await
}
#[tauri::command]
fn logout(s: State<Native>, id: String) -> Result<()> {
    auth::logout(&s.store, &id)
}
#[tauri::command]
async fn install(s: State<'_, Native>, app: AppHandle, id: String) -> Result<()> {
    let _guard = s
        .operation
        .try_lock()
        .map_err(|_| "Installation oder Start läuft bereits")?;
    if s.pid.load(Ordering::SeqCst) != 0 {
        return Err("Spiel vor der Installation beenden".into());
    }
    let p = s.store.profile(&id)?;
    Box::pin(loaders::install(&s.http, &s.root, &p, &|t| {
        let _ = app.emit("transfer", t);
    }))
    .await?;
    Ok(())
}
#[tauri::command]
async fn launch(
    s: State<'_, Native>,
    app: AppHandle,
    id: String,
    account_id: String,
) -> Result<()> {
    let _guard = s
        .operation
        .try_lock()
        .map_err(|_| "Installation oder Start läuft bereits")?;
    if s.pid.load(Ordering::SeqCst) != 0 {
        return Err("Minecraft läuft bereits".into());
    }
    let p = s.store.profile(&id)?;
    // Authenticating precedes download, so missing ownership cannot become offline login.
    let session = s
        .auth
        .session(&s.http, &account_id, &s.store.client_id()?)
        .await?;
    let installed = Box::pin(loaders::install(&s.http, &s.root, &p, &|t| {
        let _ = app.emit("transfer", t);
    }))
    .await?;
    let _ = app.emit(
        "transfer",
        minecraft::Transfer {
            stage: "Spielprozess starten".into(),
            completed: 0,
            total: 0,
        },
    );
    let mut child = minecraft::command(&p, &session, &installed)
        .await?
        .spawn()
        .map_err(|_| "Minecraft-Prozess konnte nicht gestartet werden")?;
    let pid = child.id().ok_or("Prozess-ID fehlt")?;
    s.pid.store(pid, Ordering::SeqCst);
    media::start(s.root.clone(), p.id.clone(), s.pid.clone(), pid);
    let (tx, rx) = oneshot::channel();
    *s.game.lock().await = Some(tx);
    let running = s.pid.clone();
    tauri::async_runtime::spawn(async move {
        let result =
            tokio::select! {r=child.wait()=>r,_=rx=>{let _=child.kill().await;child.wait().await}};
        running.store(0, Ordering::SeqCst);
        let message=match result{Ok(status)if status.success()=>"Minecraft wurde beendet.".to_string(),Ok(status)=>format!("Minecraft wurde mit Status {status} beendet. Details stehen im Minecraft-Spielverzeichnis unter logs/latest.log."),Err(_)=>"Minecraft-Prozessüberwachung fehlgeschlagen.".into()};
        let _ = app.emit("game-exit", message);
    });
    Ok(())
}
#[tauri::command]
async fn stop(s: State<'_, Native>) -> Result<()> {
    if let Some(tx) = s.game.lock().await.take() {
        let _ = tx.send(());
    }
    Ok(())
}
#[tauri::command]
async fn catalog(
    s: State<'_, Native>,
    action: String,
    params: std::collections::HashMap<String, String>,
) -> Result<serde_json::Value> {
    let config = catalog_config::read(&s.root)?;
    mods::catalog_with_gateway(&s.http, &action, &params, Some(&config.gateway_url)).await
}
#[tauri::command]
async fn installed_mods(s: State<'_, Native>, id: String) -> Result<Vec<mods::Installed>> {
    let p = s.store.profile(&id)?;
    mods::installed(&s.root, &p).await
}
#[tauri::command]
async fn install_mod(
    s: State<'_, Native>,
    id: String,
    source: String,
    project_id: String,
    version_id: String,
) -> Result<()> {
    let _guard = s
        .operation
        .try_lock()
        .map_err(|_| "Eine Installation läuft bereits")?;
    if s.pid.load(Ordering::SeqCst) != 0 {
        return Err("Minecraft vor Mod-Änderungen beenden".into());
    }
    let p = s.store.profile(&id)?;
    mods::install(&s.http, &s.root, &p, &source, &project_id, &version_id).await
}
#[tauri::command]
fn client_id(s: State<Native>) -> Result<String> {
    s.store.client_id()
}
#[tauri::command]
async fn save_client_id(s: State<'_, Native>, client_id: String) -> Result<()> {
    let mut pending = s
        .auth
        .pending
        .try_lock()
        .map_err(|_| "Anmeldung läuft bereits")?;
    s.store.save_client_id(client_id.trim())?;
    *pending = None;
    Ok(())
}
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_updater::Builder::new().build())
        .plugin(tauri_plugin_opener::init())
        .manage(updates::Pending::default())
        .setup(|app| {
            let root = app.path().app_data_dir()?;
            std::fs::create_dir_all(&root)?;
            let store = Store::open(&root.join("hera.db")).map_err(std::io::Error::other)?;
            let http = minecraft::http().map_err(std::io::Error::other)?;
            app.manage(Native {
                store,
                root,
                http,
                auth: Auth {
                    pending: Mutex::new(None),
                    generation: std::sync::atomic::AtomicU64::new(0),
                },
                operation: Mutex::new(()),
                game: Mutex::new(None),
                pid: Arc::new(AtomicU32::new(0)),
            });
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            game_running,
            cape_service_config,
            save_cape_service,
            probe_cape_service,
            publish_cape,
            local_cape,
            save_local_cape,
            set_local_cape_enabled,
            remove_local_cape,
            profiles,
            save_profile,
            delete_profile,
            settings,
            save_settings,
            runtime,
            versions,
            loader_options,
            accounts,
            begin_login,
            cancel_login,
            complete_login,
            logout,
            install,
            launch,
            stop,
            catalog,
            catalog_config,
            save_catalog_config,
            probe_catalog_config,
            installed_mods,
            install_mod,
            client_id,
            save_client_id,
            updates::check_update,
            updates::install_update
        ])
        .run(tauri::generate_context!())
        .expect("Hera konnte nicht gestartet werden");
}
