//! Read-only Windows media bridge; no Spotify credentials, browser scraping or network API.
use serde::Serialize;
use std::{
    path::{Path, PathBuf},
    sync::{
        atomic::{AtomicU32, Ordering},
        Arc,
    },
    time::{Duration, SystemTime, UNIX_EPOCH},
};
#[derive(Default, Serialize)]
#[serde(rename_all = "camelCase")]
struct Snapshot {
    version: u8,
    available: bool,
    title: String,
    artist: String,
    source: String,
    playing: bool,
    position_ms: u64,
    duration_ms: u64,
    updated_at_ms: u64,
}
fn now() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64
}
fn clean(value: String, limit: usize) -> String {
    value
        .chars()
        .filter(|c| !c.is_control())
        .take(limit)
        .collect()
}
fn options(game: &Path) -> Option<bool> {
    for dir in [
        game.parent()?,
        game,
        &game.join("config"),
        &game.join("config/hera"),
    ] {
        if crate::bundled_mods::reject_link(dir).is_err() {
            return None;
        }
    }
    let file = game.join("config/hera/hud.json");
    if crate::bundled_mods::reject_link(&file).is_err()
        || std::fs::metadata(&file).ok()?.len() > 524288
    {
        return None;
    }
    let value: serde_json::Value = serde_json::from_slice(&std::fs::read(file).ok()?).ok()?;
    let module = value["modules"]
        .as_array()?
        .iter()
        .find(|m| m["id"] == "Spotify" && m["enabled"] == true)?;
    Some(module["mediaBrowser"].as_bool().unwrap_or(true))
}
fn write(game: &Path, value: &Snapshot) {
    let dir = game.join("config/hera");
    for path in [game.to_path_buf(), game.join("config"), dir.clone()] {
        if crate::bundled_mods::reject_link(&path).is_err() {
            return;
        }
    }
    let file = dir.join("media.json");
    if crate::bundled_mods::reject_link(&file).is_err() {
        return;
    }
    let Ok(bytes) = serde_json::to_vec(value) else {
        return;
    };
    let temporary = dir.join(format!(".media-{}.tmp", uuid::Uuid::new_v4()));
    if std::fs::create_dir_all(&dir).is_ok() && std::fs::write(&temporary, bytes).is_ok() {
        if std::fs::rename(&temporary, &file).is_err() {
            let _ = std::fs::remove_file(temporary);
        }
    }
}
pub fn start(root: PathBuf, id: String, running: Arc<AtomicU32>, pid: u32) {
    if uuid::Uuid::parse_str(&id).is_err() {
        return;
    }
    let game = root.join("profiles").join(id);
    let _ = std::thread::Builder::new()
        .name("hera-media".into())
        .spawn(move || {
            #[cfg(windows)]
            let initialized = unsafe {
                windows::Win32::System::WinRT::RoInitialize(
                    windows::Win32::System::WinRT::RO_INIT_MULTITHREADED,
                )
            }
            .is_ok();
            while running.load(Ordering::SeqCst) == pid {
                if let Some(browser) = options(&game) {
                    let value = read(browser);
                    write(&game, &value);
                }
                std::thread::sleep(Duration::from_secs(1));
            }
            write(
                &game,
                &Snapshot {
                    version: 1,
                    updated_at_ms: now(),
                    ..Default::default()
                },
            );
            #[cfg(windows)]
            if initialized {
                unsafe {
                    windows::Win32::System::WinRT::RoUninitialize();
                }
            }
        });
}
#[cfg(not(windows))]
fn read(_: bool) -> Snapshot {
    Snapshot {
        version: 1,
        updated_at_ms: now(),
        ..Default::default()
    }
}
#[cfg(windows)]
fn read(browser: bool) -> Snapshot {
    use windows::Media::Control::{
        GlobalSystemMediaTransportControlsSessionManager as Manager,
        GlobalSystemMediaTransportControlsSessionPlaybackStatus as Status,
    };
    let fallback = || Snapshot {
        version: 1,
        updated_at_ms: now(),
        ..Default::default()
    };
    let Ok(manager) = Manager::RequestAsync().and_then(|op| op.join()) else {
        return fallback();
    };
    let Ok(sessions) = manager.GetSessions() else {
        return fallback();
    };
    let mut best = None;
    let mut best_score = 0;
    for index in 0..sessions.Size().unwrap_or(0).min(16) {
        let Ok(session) = sessions.GetAt(index) else {
            continue;
        };
        let source = session
            .SourceAppUserModelId()
            .map(|s| s.to_string().to_lowercase())
            .unwrap_or_default();
        let spotify = source.contains("spotify");
        let is_browser = ["chrome", "msedge", "firefox", "brave", "opera"]
            .iter()
            .any(|name| source.contains(name));
        if !spotify && !(browser && is_browser) {
            continue;
        }
        let playing = session
            .GetPlaybackInfo()
            .and_then(|info| info.PlaybackStatus())
            .is_ok_and(|s| s == Status::Playing);
        let score = if playing { 100 } else { 0 } + if spotify { 50 } else { 10 };
        if score > best_score {
            best = Some((session, playing, spotify));
            best_score = score;
        }
    }
    let Some((session, playing, spotify)) = best else {
        return fallback();
    };
    let Ok(properties) = session
        .TryGetMediaPropertiesAsync()
        .and_then(|op| op.join())
    else {
        return fallback();
    };
    let title = clean(
        properties
            .Title()
            .map(|s| s.to_string())
            .unwrap_or_default(),
        160,
    );
    if title.is_empty() {
        return fallback();
    }
    let artist = clean(
        properties
            .Artist()
            .map(|s| s.to_string())
            .unwrap_or_default(),
        96,
    );
    let timestamp = now();
    let mut duration = 0;
    let mut position = 0;
    if let Ok(timeline) = session.GetTimelineProperties() {
        let start = timeline
            .StartTime()
            .map(|t| t.Duration / 10000)
            .unwrap_or(0);
        let end = timeline.EndTime().map(|t| t.Duration / 10000).unwrap_or(0);
        duration = (end - start).max(0) as u64;
        position =
            (timeline.Position().map(|t| t.Duration / 10000).unwrap_or(0) - start).max(0) as u64;
        if playing {
            let updated = timeline
                .LastUpdatedTime()
                .map(|t| t.UniversalTime / 10000 - 11644473600000)
                .unwrap_or(timestamp as i64);
            if updated > 0 && updated as u64 <= timestamp {
                position = position.saturating_add(timestamp - updated as u64);
            }
        }
        if duration > 0 {
            position = position.min(duration);
        } else {
            position = 0;
        }
    }
    Snapshot {
        version: 1,
        available: true,
        title,
        artist,
        source: if spotify { "Spotify" } else { "Browser" }.into(),
        playing,
        position_ms: position,
        duration_ms: duration,
        updated_at_ms: timestamp,
    }
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn title_is_bounded_and_control_characters_are_removed() {
        assert_eq!(clean("ab\ncd".into(), 3), "abc");
        assert_eq!(clean("äöü".into(), 2), "äö");
    }
    #[test]
    fn media_access_requires_explicit_enabled_module() {
        let root = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("../../../.local/media-tests")
            .join(uuid::Uuid::new_v4().to_string());
        let dir = root.join("config/hera");
        std::fs::create_dir_all(&dir).unwrap();
        let file = dir.join("hud.json");
        assert_eq!(options(&root), None);
        std::fs::write(&file, r#"{"modules":[{"id":"Spotify","enabled":false}]}"#).unwrap();
        assert_eq!(options(&root), None);
        std::fs::write(
            &file,
            r#"{"modules":[{"id":"Spotify","enabled":true,"mediaBrowser":false}]}"#,
        )
        .unwrap();
        assert_eq!(options(&root), Some(false));
        std::fs::write(&file, r#"{"modules":[{"id":"Spotify","enabled":true}]}"#).unwrap();
        assert_eq!(options(&root), Some(true));
    }
}
