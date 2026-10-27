//! Local-only, profile-scoped cosmetics. No arbitrary paths or account tokens.
use crate::{
    bundled_mods,
    storage::{Profile, Result},
};
use base64::{engine::general_purpose::STANDARD, Engine};
use serde::{Deserialize, Serialize};
use std::{
    io::Cursor,
    path::{Path, PathBuf},
};
const MAX_PNG: usize = 1024 * 1024;
#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Cape {
    pub version: u8,
    pub owner: String,
    pub enabled: bool,
    pub png_base64: String,
}
fn path(root: &Path, p: &Profile) -> Result<PathBuf> {
    p.validate()?;
    if !bundled_mods::includes(p) {
        return Err("Eigene Capes benötigen Hera mit Fabric 1.21.11".into());
    }
    let mut dir = root.to_path_buf();
    for part in ["profiles", &p.id, "config", "hera"] {
        dir.push(part);
        bundled_mods::reject_link(&dir)?;
    }
    let file = dir.join("cape.json");
    bundled_mods::reject_link(&file)?;
    Ok(file)
}
pub fn normalize_png(encoded: &str) -> Result<String> {
    if encoded.len() > MAX_PNG * 4 / 3 + 4 {
        return Err("Cape-PNG darf höchstens 1 MB groß sein".into());
    }
    let bytes = STANDARD
        .decode(encoded)
        .map_err(|_| "Ungültige Cape-Datei")?;
    if bytes.len() > MAX_PNG {
        return Err("Cape-Datei zu groß".into());
    }
    let mut decoder = png::Decoder::new(Cursor::new(bytes));
    decoder.set_limits(png::Limits { bytes: 4 * MAX_PNG });
    decoder.set_transformations(png::Transformations::EXPAND | png::Transformations::STRIP_16);
    let mut reader = decoder.read_info().map_err(|_| "Keine gültige PNG-Datei")?;
    let info = reader.info();
    if !(64..=512).contains(&info.width)
        || !info.width.is_power_of_two()
        || info.height != info.width / 2
        || info.animation_control.is_some()
    {
        return Err(
            "Cape-PNG muss 64×32, 128×64, 256×128 oder 512×256 Pixel haben (ohne Animation)".into(),
        );
    }
    let size = reader
        .output_buffer_size()
        .filter(|s| *s <= MAX_PNG)
        .ok_or("Cape-Bild zu groß")?;
    let mut pixels = vec![0; size];
    let frame = reader
        .next_frame(&mut pixels)
        .map_err(|_| "Cape-PNG ist beschädigt")?;
    if frame.bit_depth != png::BitDepth::Eight
        || !matches!(frame.color_type, png::ColorType::Rgb | png::ColorType::Rgba)
    {
        return Err("Cape bitte als RGB- oder RGBA-PNG exportieren".into());
    }
    let mut output = Vec::new();
    {
        let mut encoder = png::Encoder::new(&mut output, frame.width, frame.height);
        encoder.set_color(frame.color_type);
        encoder.set_depth(png::BitDepth::Eight);
        encoder
            .write_header()
            .map_err(|_| "Cape konnte nicht verarbeitet werden")?
            .write_image_data(&pixels[..frame.buffer_size()])
            .map_err(|_| "Cape konnte nicht verarbeitet werden")?;
    }
    if output.len() > MAX_PNG {
        return Err("Cape-Datei zu groß".into());
    }
    Ok(STANDARD.encode(output))
}
pub fn read(root: &Path, p: &Profile) -> Result<Option<Cape>> {
    let file = path(root, p)?;
    match std::fs::metadata(&file) {
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => return Ok(None),
        Err(_) => return Err("Cape konnte nicht gelesen werden".into()),
        Ok(m) if m.len() > 2 * MAX_PNG as u64 => return Err("Cape-Einstellungen zu groß".into()),
        _ => {}
    }
    let value: Cape = serde_json::from_slice(
        &std::fs::read(file).map_err(|_| "Cape konnte nicht gelesen werden")?,
    )
    .map_err(|_| "Cape-Einstellungen beschädigt")?;
    if value.version != 1 || uuid::Uuid::parse_str(&value.owner).is_err() {
        return Err("Cape-Einstellungen ungültig".into());
    }
    normalize_png(&value.png_base64)?;
    Ok(Some(value))
}
pub fn write(root: &Path, p: &Profile, cape: &Cape) -> Result<()> {
    let file = path(root, p)?;
    let parent = file.parent().ok_or("Cape-Ziel fehlt")?;
    std::fs::create_dir_all(parent).map_err(|_| "Cape-Verzeichnis konnte nicht erstellt werden")?;
    let temporary = parent.join(format!(".cape-{}.tmp", uuid::Uuid::new_v4()));
    let data = serde_json::to_vec(cape).map_err(|_| "Cape konnte nicht gespeichert werden")?;
    std::fs::write(&temporary, data).map_err(|_| "Cape konnte nicht gespeichert werden")?;
    if std::fs::rename(&temporary, &file).is_err() {
        let _ = std::fs::remove_file(temporary);
        return Err("Cape konnte nicht gespeichert werden".into());
    }
    Ok(())
}
pub fn remove(root: &Path, p: &Profile) -> Result<()> {
    match std::fs::remove_file(path(root, p)?) {
        Ok(()) => Ok(()),
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(()),
        Err(_) => Err("Cape konnte nicht entfernt werden".into()),
    }
}
#[cfg(test)]
mod tests {
    use super::*;
    fn image(w: u32, h: u32) -> String {
        let mut data = Vec::new();
        {
            let mut encoder = png::Encoder::new(&mut data, w, h);
            encoder.set_color(png::ColorType::Rgba);
            encoder.set_depth(png::BitDepth::Eight);
            encoder
                .write_header()
                .unwrap()
                .write_image_data(&vec![255; (w * h * 4) as usize])
                .unwrap();
        }
        STANDARD.encode(data)
    }
    #[test]
    fn accepts_cape_atlas_and_rejects_wrong_dimensions_or_corruption() {
        assert!(normalize_png(&image(64, 32)).is_ok());
        assert!(normalize_png(&image(128, 64)).is_ok());
        assert!(normalize_png(&image(64, 64)).is_err());
        assert!(normalize_png(&image(32, 16)).is_err());
        assert!(normalize_png("not-png").is_err());
        assert!(normalize_png(&"A".repeat(2 * MAX_PNG)).is_err());
        let mut bad = STANDARD.decode(image(64, 32)).unwrap();
        bad.truncate(40);
        assert!(normalize_png(&STANDARD.encode(bad)).is_err());
    }
    #[test]
    fn profile_paths_are_uuid_scoped() {
        let mut p = Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Cape".into(),
            version: "1.21.11".into(),
            loader: "fabric".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        };
        assert!(path(Path::new("unused"), &p)
            .unwrap()
            .ends_with("config/hera/cape.json"));
        p.id = "../escape".into();
        assert!(path(Path::new("unused"), &p).is_err());
    }
    #[test]
    fn cape_persists_toggles_and_removes_without_touching_other_profiles() {
        let root = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../../.local/cape-tests");
        let mut p = Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Cape".into(),
            version: "1.21.11".into(),
            loader: "fabric".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        };
        let mut cape = Cape {
            version: 1,
            owner: uuid::Uuid::new_v4().simple().to_string(),
            enabled: true,
            png_base64: normalize_png(&image(64, 32)).unwrap(),
        };
        write(&root, &p, &cape).unwrap();
        assert!(read(&root, &p).unwrap().unwrap().enabled);
        cape.enabled = false;
        write(&root, &p, &cape).unwrap();
        assert!(!read(&root, &p).unwrap().unwrap().enabled);
        let original = p.id.clone();
        p.id = uuid::Uuid::new_v4().to_string();
        assert!(read(&root, &p).unwrap().is_none());
        p.id = original;
        remove(&root, &p).unwrap();
        assert!(read(&root, &p).unwrap().is_none());
    }
}
