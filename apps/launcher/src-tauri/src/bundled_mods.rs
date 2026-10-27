//! Release-pinned mod payloads. No web-provided paths or executable downloads.
use crate::{
    minecraft::Transfer,
    storage::{Profile, Result},
};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::path::Path;

pub const MOD_VERSION: &str = "0.6.1";
pub fn includes(p: &Profile) -> bool {
    p.loader == "fabric" && p.version == "1.21.11"
}
pub fn satisfies_api(p: &Profile, filename: &str, sha1: &str) -> bool {
    includes(p)
        && filename == PAYLOADS[1].0
        && sha1.eq_ignore_ascii_case(&format!("{:x}", sha1::Sha1::digest(PAYLOADS[1].1)))
}
const PAYLOADS: [(&str, &[u8]); 2] = [
    (
        "hera-fabric-1.21.11-0.6.1.jar",
        include_bytes!("../resources/hera-fabric-1.21.11-0.6.1.jar"),
    ),
    (
        "fabric-api-0.141.6+1.21.11.jar",
        include_bytes!("../resources/fabric-api-0.141.6+1.21.11.jar"),
    ),
];
#[derive(Clone, Serialize, Deserialize)]
struct Managed {
    filename: String,
    sha256: String,
    size: u64,
}
fn digest(bytes: &[u8]) -> String {
    format!("{:x}", Sha256::digest(bytes))
}
fn valid_name(name: &str) -> bool {
    name.len() < 140
        && name.ends_with(".jar")
        && !name.contains("..")
        && name
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b"._+-".contains(&b))
        && (name.starts_with("hera-fabric-1.21.11-")
            || name.starts_with("fabric-api-") && name.ends_with("+1.21.11.jar"))
}
pub(crate) fn reject_link(path: &Path) -> Result<()> {
    if let Ok(m) = std::fs::symlink_metadata(path) {
        if m.file_type().is_symlink() {
            return Err("Verknüpfungen im Mod-Verzeichnis sind nicht erlaubt".into());
        }
        #[cfg(windows)]
        {
            use std::os::windows::fs::MetadataExt;
            if m.file_attributes() & 0x400 != 0 {
                return Err("Verknüpfungen im Mod-Verzeichnis sind nicht erlaubt".into());
            }
        }
    }
    Ok(())
}
async fn matches(path: &Path, record: &Managed) -> bool {
    if reject_link(path).is_err() {
        return false;
    }
    let Ok(meta) = tokio::fs::metadata(path).await else {
        return false;
    };
    if !meta.is_file() || meta.len() != record.size || record.size > 128 * 1024 * 1024 {
        return false;
    }
    tokio::fs::read(path)
        .await
        .is_ok_and(|bytes| digest(&bytes) == record.sha256)
}
async fn atomic(path: &Path, bytes: &[u8]) -> Result<()> {
    reject_link(path)?;
    let parent = path.parent().ok_or("Mod-Ziel fehlt")?;
    let temporary = parent.join(format!(".hera-{}.tmp", uuid::Uuid::new_v4()));
    tokio::fs::write(&temporary, bytes)
        .await
        .map_err(|e| e.to_string())?;
    if let Err(e) = tokio::fs::rename(&temporary, path).await {
        let _ = tokio::fs::remove_file(&temporary).await;
        return Err(e.to_string());
    }
    Ok(())
}
pub async fn install(root: &Path, p: &Profile, progress: &impl Fn(Transfer)) -> Result<()> {
    p.validate()?;
    if p.loader != "fabric" || p.version != "1.21.11" {
        return Ok(());
    }
    let profiles = root.join("profiles");
    let game = profiles.join(&p.id);
    let mods = game.join("mods");
    for dir in [&profiles, &game, &mods] {
        reject_link(dir)?;
        tokio::fs::create_dir_all(dir)
            .await
            .map_err(|e| e.to_string())?;
    }
    let manifest = game.join("hera-bundled.json");
    reject_link(&manifest)?;
    let previous: Vec<Managed> = if manifest.exists() {
        if tokio::fs::metadata(&manifest)
            .await
            .map_err(|e| e.to_string())?
            .len()
            > 16384
        {
            return Err("Hera-Mod-Verzeichnisdaten sind beschädigt".into());
        }
        serde_json::from_slice(
            &tokio::fs::read(&manifest)
                .await
                .map_err(|e| e.to_string())?,
        )
        .map_err(|_| "Hera-Mod-Verzeichnisdaten sind beschädigt")?
    } else {
        vec![]
    };
    for old in &previous {
        if !valid_name(&old.filename)
            || old.sha256.len() != 64
            || !old.sha256.bytes().all(|b| b.is_ascii_hexdigit())
            || old.size > 128 * 1024 * 1024
        {
            return Err("Ungültige verwaltete Mod-Datei".into());
        }
    }
    let expected: Vec<Managed> =
        serde_json::from_str(include_str!("../resources/bundled-mods.json"))
            .map_err(|_| "Hera-Paketdaten sind beschädigt")?;
    if expected.len() != PAYLOADS.len() {
        return Err("Hera-Paketdaten sind unvollständig".into());
    }
    for (index, (name, bytes)) in PAYLOADS.iter().enumerate() {
        let record = &expected[index];
        if record.filename != *name
            || record.size != bytes.len() as u64
            || record.sha256 != digest(bytes)
        {
            return Err("Hera-Paketprüfung fehlgeschlagen".into());
        }
        let path = mods.join(name);
        reject_link(&path)?;
        if !matches(&path, record).await {
            atomic(&path, bytes).await?;
        }
        progress(Transfer {
            stage: format!("Hera {MOD_VERSION} und Fabric API einrichten"),
            completed: index + 1,
            total: 2,
        });
    }
    for old in previous {
        if PAYLOADS.iter().any(|(name, _)| *name == old.filename) {
            continue;
        }
        let path = mods.join(&old.filename);
        if path.exists() {
            if !matches(&path, &old).await {
                return Err("Eine frühere Hera-Mod-Datei wurde verändert. Bitte die Mod-Dateien dieses Profils prüfen.".into());
            }
            tokio::fs::remove_file(path)
                .await
                .map_err(|e| e.to_string())?;
        }
    }
    atomic(
        &manifest,
        &serde_json::to_vec_pretty(&expected).map_err(|e| e.to_string())?,
    )
    .await
}

#[cfg(test)]
mod tests {
    use super::*;
    fn profile() -> Profile {
        Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Bundled integration".into(),
            version: "1.21.11".into(),
            loader: "fabric".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        }
    }
    #[tokio::test]
    async fn installs_repairs_and_preserves_other_mods() {
        let root = std::env::temp_dir().join(format!("hera-bundle-{}", uuid::Uuid::new_v4()));
        let p = profile();
        install(&root, &p, &|_| {}).await.unwrap();
        let mods = root.join("profiles").join(&p.id).join("mods");
        tokio::fs::write(mods.join("user-mod.jar"), b"custom")
            .await
            .unwrap();
        tokio::fs::write(mods.join(PAYLOADS[0].0), b"corrupt")
            .await
            .unwrap();
        install(&root, &p, &|_| {}).await.unwrap();
        assert_eq!(
            tokio::fs::read(mods.join(PAYLOADS[0].0)).await.unwrap(),
            PAYLOADS[0].1
        );
        assert_eq!(
            tokio::fs::read(mods.join("user-mod.jar")).await.unwrap(),
            b"custom"
        );
        let resolved = root.canonicalize().unwrap();
        assert_eq!(
            resolved.parent().unwrap(),
            std::env::temp_dir().canonicalize().unwrap()
        );
        assert!(resolved
            .file_name()
            .unwrap()
            .to_string_lossy()
            .starts_with("hera-bundle-"));
        std::fs::remove_dir_all(resolved).unwrap();
    }
    #[tokio::test]
    async fn skips_vanilla_and_forge_profiles() {
        let root = std::env::temp_dir().join(format!("hera-skip-{}", uuid::Uuid::new_v4()));
        let mut p = profile();
        p.loader = "forge".into();
        install(&root, &p, &|_| {}).await.unwrap();
        assert!(!root.exists());
        p.loader = "vanilla".into();
        install(&root, &p, &|_| {}).await.unwrap();
        assert!(!root.exists());
    }
    #[test]
    fn managed_names_cannot_escape() {
        for name in [
            "../hera.jar",
            "C:/hera.jar",
            "hera-fabric-1.21.11-../../x.jar",
            "other.jar",
        ] {
            assert!(!valid_name(name));
        }
        assert!(valid_name(PAYLOADS[0].0));
    }
}
