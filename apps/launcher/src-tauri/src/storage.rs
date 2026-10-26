use rusqlite::{params, Connection};
use serde::{Deserialize, Serialize};
use std::{path::Path, sync::Mutex};

pub type Result<T> = std::result::Result<T, String>;
pub struct Store(pub Mutex<Connection>);
#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Profile {
    pub id: String,
    pub name: String,
    pub version: String,
    pub loader: String,
    pub memory_mb: u32,
    pub java_path: String,
}
pub fn valid_version_id(id: &str) -> bool {
    !id.is_empty()
        && id.len() <= 80
        && !id.contains("..")
        && id.as_bytes()[0].is_ascii_alphanumeric()
        && id
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b"._+ -".contains(&b))
}
impl Profile {
    pub fn validate(&self) -> Result<()> {
        uuid::Uuid::parse_str(&self.id).map_err(|_| "Ungültige Profil-ID")?;
        let java = Path::new(&self.java_path);
        let executable = java.file_name().and_then(|n| n.to_str()).unwrap_or("");
        if !["java", "java.exe"]
            .iter()
            .any(|n| executable.eq_ignore_ascii_case(n))
            || (!java.is_absolute() && !["java", "java.exe"].contains(&self.java_path.as_str()))
        {
            return Err("Java-Pfad muss java/java.exe oder ein absoluter Pfad zu Java sein".into());
        }
        if self.name.trim().is_empty()
            || self.name.chars().count() > 48
            || !valid_version_id(&self.version)
            || !["vanilla", "fabric", "forge"].contains(&self.loader.as_str())
            || !(2048..=16384).contains(&self.memory_mb)
            || self.java_path.is_empty()
            || self.java_path.len() > 1024
            || self.java_path.contains(['\0', '\n', '\r'])
        {
            return Err("Ungültige Profileinstellungen".into());
        }
        Ok(())
    }
}
#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Settings {
    pub reduced_motion: bool,
    pub accent: String,
    pub scale: f64,
}
impl Default for Settings {
    fn default() -> Self {
        Self {
            reduced_motion: false,
            accent: "orange".into(),
            scale: 1.0,
        }
    }
}
impl Store {
    pub fn open(path: &Path) -> Result<Self> {
        let c = Connection::open(path).map_err(|e| e.to_string())?;
        c.execute_batch("PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON; CREATE TABLE IF NOT EXISTS profiles(id TEXT PRIMARY KEY, data TEXT NOT NULL); CREATE TABLE IF NOT EXISTS settings(id INTEGER PRIMARY KEY CHECK(id=1), data TEXT NOT NULL); CREATE TABLE IF NOT EXISTS accounts(id TEXT PRIMARY KEY, name TEXT NOT NULL); CREATE TABLE IF NOT EXISTS configuration(key TEXT PRIMARY KEY, value TEXT NOT NULL); PRAGMA user_version=1;").map_err(|e|e.to_string())?;
        Ok(Self(Mutex::new(c)))
    }
    pub fn profiles(&self) -> Result<Vec<Profile>> {
        let c = self.0.lock().map_err(|_| "Datenbank gesperrt")?;
        let mut q = c
            .prepare("SELECT data FROM profiles ORDER BY rowid")
            .map_err(|e| e.to_string())?;
        let rows = q
            .query_map([], |r| r.get::<_, String>(0))
            .map_err(|e| e.to_string())?;
        rows.map(|r| {
            serde_json::from_str(&r.map_err(|e| e.to_string())?).map_err(|e| e.to_string())
        })
        .collect()
    }
    pub fn profile(&self, id: &str) -> Result<Profile> {
        self.profiles()?
            .into_iter()
            .find(|p| p.id == id)
            .ok_or_else(|| "Profil nicht gefunden".into())
    }
    pub fn save(&self, p: &Profile) -> Result<()> {
        p.validate()?;
        if self
            .profiles()?
            .iter()
            .any(|old| old.id == p.id && (old.loader != p.loader || old.version != p.version))
        {
            return Err("Version oder Loader eines bestehenden Profils kann nicht geändert werden. Neues Profil erstellen.".into());
        }
        self.0.lock().map_err(|_|"Datenbank gesperrt")?.execute("INSERT INTO profiles(id,data) VALUES(?1,?2) ON CONFLICT(id) DO UPDATE SET data=excluded.data",params![p.id,serde_json::to_string(p).map_err(|e|e.to_string())?]).map_err(|e|e.to_string())?;
        Ok(())
    }
    pub fn client_id(&self) -> Result<String> {
        use rusqlite::OptionalExtension;
        let value: Option<String> = self
            .0
            .lock()
            .map_err(|_| "Datenbank gesperrt")?
            .query_row(
                "SELECT value FROM configuration WHERE key='microsoft_client_id'",
                [],
                |r| r.get(0),
            )
            .optional()
            .map_err(|e| e.to_string())?;
        Ok(value.unwrap_or_else(|| {
            option_env!("HERA_MICROSOFT_CLIENT_ID")
                .unwrap_or("7a968ec3-4976-49b6-980f-4dcb76b3a95e")
                .into()
        }))
    }
    pub fn save_client_id(&self, id: &str) -> Result<()> {
        if !id.is_empty() {
            uuid::Uuid::parse_str(id)
                .map_err(|_| "Client-ID muss eine gültige Anwendungs-UUID sein")?;
        }
        let c = self.0.lock().map_err(|_| "Datenbank gesperrt")?;
        let count: i64 = c
            .query_row("SELECT COUNT(*) FROM accounts", [], |r| r.get(0))
            .map_err(|e| e.to_string())?;
        if count > 0 {
            return Err("Vor Änderung der Client-ID alle Accounts abmelden".into());
        }
        c.execute("INSERT INTO configuration(key,value) VALUES('microsoft_client_id',?1) ON CONFLICT(key) DO UPDATE SET value=excluded.value",[id]).map_err(|e|e.to_string())?;
        Ok(())
    }
    pub fn delete(&self, id: &str) -> Result<()> {
        self.0
            .lock()
            .map_err(|_| "Datenbank gesperrt")?
            .execute("DELETE FROM profiles WHERE id=?1", [id])
            .map_err(|e| e.to_string())?;
        Ok(())
    }
    pub fn settings(&self) -> Result<Settings> {
        use rusqlite::OptionalExtension;
        let raw: Option<String> = self
            .0
            .lock()
            .map_err(|_| "Datenbank gesperrt")?
            .query_row("SELECT data FROM settings WHERE id=1", [], |r| r.get(0))
            .optional()
            .map_err(|e| e.to_string())?;
        raw.map(|s| serde_json::from_str(&s).map_err(|e| e.to_string()))
            .unwrap_or_else(|| Ok(Settings::default()))
    }
    pub fn save_settings(&self, s: &Settings) -> Result<()> {
        if !["orange", "violet", "cyan"].contains(&s.accent.as_str())
            || !s.scale.is_finite()
            || !(0.85..=1.2).contains(&s.scale)
        {
            return Err("Ungültige Einstellungen".into());
        }
        self.0.lock().map_err(|_|"Datenbank gesperrt")?.execute("INSERT INTO settings(id,data) VALUES(1,?1) ON CONFLICT(id) DO UPDATE SET data=excluded.data",[serde_json::to_string(s).map_err(|e|e.to_string())?]).map_err(|e|e.to_string())?;
        Ok(())
    }
}
#[cfg(test)]
mod tests {
    use super::*;
    fn profile() -> Profile {
        Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Test".into(),
            version: "1.21.1".into(),
            loader: "vanilla".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        }
    }
    #[test]
    fn profiles_roundtrip_and_isolation() {
        let s = Store::open(Path::new(":memory:")).unwrap();
        let a = profile();
        let mut b = profile();
        b.name = "Andere Welt".into();
        s.save(&a).unwrap();
        s.save(&b).unwrap();
        assert_eq!(s.profiles().unwrap().len(), 2);
        s.delete(&a.id).unwrap();
        assert_eq!(s.profile(&b.id).unwrap().name, "Andere Welt");
    }
    #[test]
    fn version_ids_cannot_escape_directories() {
        for id in ["26.3", "1.8.9", "b1.7.3", "rd-132211"] {
            assert!(valid_version_id(id));
        }
        for id in [
            "../outside",
            "..",
            "C:/game",
            "/absolute",
            "a\\b",
            "",
            "a..b",
        ] {
            assert!(!valid_version_id(id));
        }
    }
    #[test]
    fn rejects_unsafe_profiles() {
        let mut p = profile();
        p.id = "../../escape".into();
        assert!(p.validate().is_err());
        p = profile();
        p.memory_mb = 1;
        assert!(p.validate().is_err());
        p = profile();
        p.java_path = "java\0--arg".into();
        assert!(p.validate().is_err());
        p = profile();
        p.java_path = "powershell.exe".into();
        assert!(p.validate().is_err());
    }
    #[test]
    fn sqlite_persists_across_reopen() {
        let path = std::env::temp_dir().join(format!("hera-store-{}.db", uuid::Uuid::new_v4()));
        let profile = profile();
        {
            let store = Store::open(&path).unwrap();
            store.save(&profile).unwrap();
            store
                .save_settings(&Settings {
                    reduced_motion: true,
                    accent: "cyan".into(),
                    scale: 1.1,
                })
                .unwrap();
        }
        {
            let store = Store::open(&path).unwrap();
            assert_eq!(store.profile(&profile.id).unwrap().name, "Test");
            assert!(store.settings().unwrap().reduced_motion);
        }
        std::fs::remove_file(&path).unwrap();
    }
    #[test]
    fn settings_validation() {
        let s = Store::open(Path::new(":memory:")).unwrap();
        let mut v = Settings::default();
        v.scale = f64::NAN;
        assert!(s.save_settings(&v).is_err());
    }
    #[test]
    fn client_id_validation_and_account_guard() {
        let s = Store::open(Path::new(":memory:")).unwrap();
        assert!(s.save_client_id("not-a-client-secret").is_err());
        let id = "f1854701-8b0d-4eba-a288-c080932a5667";
        s.save_client_id(id).unwrap();
        assert_eq!(s.client_id().unwrap(), id);
        s.0.lock()
            .unwrap()
            .execute("INSERT INTO accounts(id,name) VALUES('test','Player')", [])
            .unwrap();
        assert!(s.save_client_id("").is_err());
    }
    #[test]
    fn loader_is_immutable_after_profile_creation() {
        let s = Store::open(Path::new(":memory:")).unwrap();
        let mut p = profile();
        s.save(&p).unwrap();
        p.loader = "fabric".into();
        assert!(s.save(&p).is_err());
        p.id = uuid::Uuid::new_v4().to_string();
        assert!(s.save(&p).is_ok());
    }
}
