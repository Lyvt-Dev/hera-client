use crate::{
    minecraft::{self, Artifact},
    storage::{valid_version_id, Profile, Result},
};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::{
    collections::{HashMap, VecDeque},
    path::Path,
};

#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct File {
    pub filename: String,
    pub url: String,
    pub size: u64,
    pub sha1: String,
}
#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Dependency {
    pub project_id: Option<String>,
    pub version_id: Option<String>,
    pub kind: String,
}
#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Version {
    pub id: String,
    pub project_id: String,
    pub name: String,
    pub number: String,
    pub game_versions: Vec<String>,
    pub loaders: Vec<String>,
    pub release: String,
    pub file: Option<File>,
    pub dependencies: Vec<Dependency>,
}
#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Installed {
    pub source: String,
    pub project_id: String,
    pub version_id: String,
    pub title: String,
    pub version: String,
    pub filename: String,
    pub sha1: String,
    pub size: u64,
}
pub fn id(value: &str) -> Result<()> {
    if value.is_empty()
        || value.len() > 100
        || !value
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b == b'-' || b == b'_')
    {
        return Err("Ungültige Mod-ID".into());
    }
    Ok(())
}
pub fn filename(name: &str) -> Result<()> {
    if !name.ends_with(".jar")
        || name.len() > 180
        || !name
            .as_bytes()
            .first()
            .is_some_and(|b| b.is_ascii_alphanumeric())
        || !name
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b"._+-".contains(&b))
    {
        return Err("Unsicherer Mod-Dateiname".into());
    }
    Ok(())
}
async fn get(http: &Client, url: reqwest::Url) -> Result<Value> {
    let mut r = http
        .get(url)
        .send()
        .await
        .map_err(|_| "Mod-Katalog nicht erreichbar")?;
    if !r.status().is_success() {
        return Err(format!(
            "Mod-Katalog: HTTP {}. Bei CurseForge Gateway und API-Schlüssel prüfen.",
            r.status()
        ));
    }
    let mut bytes = Vec::new();
    while let Some(chunk) = r.chunk().await.map_err(|_| "Kataloganfrage unterbrochen")? {
        if bytes.len() + chunk.len() > 8 * 1024 * 1024 {
            return Err("Mod-Metadaten zu groß".into());
        }
        bytes.extend_from_slice(&chunk);
    }
    serde_json::from_slice(&bytes).map_err(|_| "Ungültige Mod-Metadaten".into())
}
fn str_field<'a>(v: &'a Value, k: &str) -> Result<&'a str> {
    v[k].as_str()
        .ok_or_else(|| format!("Mod-Metadaten: {k} fehlt"))
}
fn project(v: &Value) -> Result<Value> {
    let id = v["project_id"]
        .as_str()
        .or_else(|| v["id"].as_str())
        .ok_or("Projekt-ID fehlt")?;
    let slug = str_field(v, "slug")?;
    Ok(
        json!({"id":id,"slug":slug,"title":str_field(v,"title")?,"description":str_field(v,"description")?,"author":v["author"].as_str().unwrap_or(""),"downloads":v["downloads"].as_u64().unwrap_or(0),"iconUrl":v["icon_url"],"url":format!("https://modrinth.com/mod/{slug}")}),
    )
}
fn version(v: &Value) -> Result<Version> {
    let files = v["files"].as_array().ok_or("Mod-Dateien fehlen")?;
    let file = files
        .iter()
        .find(|f| f["primary"] == true)
        .or_else(|| files.first())
        .map(|f| -> Result<File> {
            Ok(File {
                filename: str_field(f, "filename")?.into(),
                url: str_field(f, "url")?.into(),
                sha1: str_field(&f["hashes"], "sha1")?.into(),
                size: f["size"].as_u64().ok_or("Dateigröße fehlt")?,
            })
        })
        .transpose()?;
    let dependencies = v["dependencies"]
        .as_array()
        .ok_or("Abhängigkeiten fehlen")?
        .iter()
        .map(|d| Dependency {
            project_id: d["project_id"].as_str().map(String::from),
            version_id: d["version_id"].as_str().map(String::from),
            kind: d["dependency_type"].as_str().unwrap_or("unknown").into(),
        })
        .collect();
    Ok(Version {
        id: str_field(v, "id")?.into(),
        project_id: str_field(v, "project_id")?.into(),
        name: str_field(v, "name")?.into(),
        number: str_field(v, "version_number")?.into(),
        release: str_field(v, "version_type")?.into(),
        game_versions: serde_json::from_value(v["game_versions"].clone())
            .map_err(|_| "Spielversionen fehlen")?,
        loaders: serde_json::from_value(v["loaders"].clone()).map_err(|_| "Loader fehlen")?,
        file,
        dependencies,
    })
}
#[cfg(test)]
pub async fn catalog(http: &Client, action: &str, p: &HashMap<String, String>) -> Result<Value> {
    catalog_with_gateway(http, action, p, None).await
}
pub async fn catalog_with_gateway(
    http: &Client,
    action: &str,
    p: &HashMap<String, String>,
    gateway: Option<&str>,
) -> Result<Value> {
    if !["search", "project", "versions", "version", "status"].contains(&action) {
        return Err("Unbekannte Katalogfunktion".into());
    }
    let source = p.get("source").map(String::as_str).unwrap_or("modrinth");
    if !["modrinth", "curseforge"].contains(&source) {
        return Err("Unbekannte Mod-Plattform".into());
    }
    if source == "curseforge" {
        let configured=gateway.filter(|u|!u.is_empty()).map(String::from).or_else(||std::env::var("HERA_CATALOG_URL").ok()).or_else(||option_env!("HERA_CATALOG_URL").map(String::from)).filter(|v|!v.is_empty()).ok_or("CurseForge-Gateway nicht konfiguriert. In den erweiterten Einstellungen einrichten.")?;
        let mut url =
            reqwest::Url::parse(&format!("{}/{action}", configured.trim_end_matches('/')))
                .map_err(|_| "Ungültige Gateway-URL")?;
        if !(url.scheme() == "https"
            || (url.scheme() == "http"
                && matches!(url.host_str(), Some("127.0.0.1" | "localhost" | "[::1]"))))
            || !url.username().is_empty()
            || url.password().is_some()
        {
            return Err("Gateway benötigt HTTPS (außer lokalem Entwicklungsserver)".into());
        }
        url.query_pairs_mut().extend_pairs(p);
        let local = Client::builder()
            .user_agent("HeraClient/0.1.0")
            .redirect(reqwest::redirect::Policy::none())
            .timeout(std::time::Duration::from_secs(25))
            .build()
            .map_err(|e| e.to_string())?;
        return get(&local, url).await;
    }
    let (loader, game_version) = catalog_target(p)?;
    let query = p.get("query").cloned().unwrap_or_default();
    if query.len() > 400 {
        return Err("Suchanfrage zu lang".into());
    }
    let item_id = p.get("id").map(String::as_str).unwrap_or("");
    if action != "search" {
        id(item_id)?;
    }
    let mut url = reqwest::Url::parse("https://api.modrinth.com/v2/").unwrap();
    match action {
        "search" => {
            let offset = p
                .get("offset")
                .and_then(|s| s.parse::<u32>().ok())
                .unwrap_or(0);
            if offset > 10000 {
                return Err("Ungültige Katalogseite".into());
            }
            url.set_path("/v2/search");
            url.query_pairs_mut()
                .append_pair("query", &query)
                .append_pair("limit", "20")
                .append_pair("offset", &offset.to_string())
                .append_pair("index", "downloads")
                .append_pair(
                    "facets",
                    &json!([
                        ["project_type:mod"],
                        [format!("versions:{game_version}")],
                        [format!("categories:{loader}")],
                        ["client_side:required", "client_side:optional"]
                    ])
                    .to_string(),
                );
            let body = get(http, url).await?;
            let hits = body["hits"]
                .as_array()
                .ok_or("Suchergebnisse fehlen")?
                .iter()
                .map(project)
                .collect::<Result<Vec<_>>>()?;
            return Ok(
                json!({"projects":hits,"hasMore":offset as u64+20<body["total_hits"].as_u64().unwrap_or(0)}),
            );
        }
        "project" => url.set_path(&format!("/v2/project/{item_id}")),
        "versions" => {
            url.set_path(&format!("/v2/project/{item_id}/version"));
            url.query_pairs_mut()
                .append_pair("game_versions", &json!([game_version]).to_string())
                .append_pair("loaders", &json!([loader]).to_string());
        }
        "version" => url.set_path(&format!("/v2/version/{item_id}")),
        _ => return Err("Unbekannte Katalogfunktion".into()),
    }
    let body = get(http, url).await?;
    match action {
        "project" => project(&body),
        "versions" => Ok(serde_json::to_value(
            body.as_array()
                .ok_or("Versionsliste fehlt")?
                .iter()
                .map(version)
                .collect::<Result<Vec<_>>>()?,
        )
        .map_err(|e| e.to_string())?),
        _ => serde_json::to_value(version(&body)?).map_err(|e| e.to_string()),
    }
}
fn catalog_target(p: &HashMap<String, String>) -> Result<(&str, &str)> {
    let loader = p.get("loader").map(String::as_str).unwrap_or("");
    let version = p.get("version").map(String::as_str).unwrap_or("");
    if !["fabric", "forge"].contains(&loader) || !valid_version_id(version) {
        return Err("Ungültiges Mod-Zielprofil".into());
    }
    Ok((loader, version))
}
fn params(source: &str, profile: &Profile, id: &str) -> HashMap<String, String> {
    [
        ("source", source),
        ("loader", profile.loader.as_str()),
        ("version", profile.version.as_str()),
        ("id", id),
    ]
    .into_iter()
    .map(|(k, v)| (k.into(), v.into()))
    .collect()
}
pub async fn installed(root: &Path, p: &Profile) -> Result<Vec<Installed>> {
    p.validate()?;
    let path = root.join("profiles").join(&p.id).join("hera-mods.json");
    match tokio::fs::read(path).await {
        Ok(bytes) => {
            if bytes.len() > 1024 * 1024 {
                return Err("Mod-Manifest zu groß".into());
            }
            let list: Vec<Installed> =
                serde_json::from_slice(&bytes).map_err(|_| "Mod-Manifest beschädigt")?;
            for m in &list {
                filename(&m.filename)?;
            }
            Ok(list)
        }
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(vec![]),
        Err(e) => Err(e.to_string()),
    }
}
pub async fn install(
    http: &Client,
    root: &Path,
    p: &Profile,
    source: &str,
    project_id: &str,
    version_id: &str,
) -> Result<()> {
    let gateway = crate::catalog_config::read(root)?.gateway_url;
    p.validate()?;
    id(project_id)?;
    id(version_id)?;
    if p.loader == "vanilla" {
        return Err("Vanilla-Profile können keine Mods laden".into());
    }
    let existing = installed(root, p).await?;
    crate::bundled_mods::install(root, p, &|_| {}).await?;
    let mut q = params(source, p, version_id);
    q.insert("projectId".into(), project_id.into());
    let first: Version =
        serde_json::from_value(catalog_with_gateway(http, "version", &q, Some(&gateway)).await?)
            .map_err(|_| "Ungültige Mod-Version")?;
    if first.project_id != project_id {
        return Err("Version gehört nicht zum gewählten Projekt".into());
    }
    let mut queue = VecDeque::from([first]);
    let mut chosen: HashMap<String, Version> = HashMap::new();
    let mut records = vec![];
    let mut files = vec![];
    while let Some(v) = queue.pop_front() {
        if let Some(prior) = chosen.get(&v.project_id) {
            if prior.id != v.id {
                return Err("Widersprüchliche Abhängigkeitsversionen".into());
            }
            continue;
        }
        if chosen.len() >= 64 {
            return Err("Zu viele Mod-Abhängigkeiten".into());
        }
        if !v.loaders.contains(&p.loader) || !v.game_versions.contains(&p.version) {
            return Err(format!("{} passt nicht zu diesem Profil", v.name));
        }
        let file = v
            .file
            .as_ref()
            .ok_or("Autor erlaubt keinen automatischen Dateidownload")?;
        filename(&file.filename)?;
        if crate::bundled_mods::satisfies_api(p, &file.filename, &file.sha1) {
            chosen.insert(v.project_id.clone(), v);
            continue;
        }
        if crate::bundled_mods::includes(p) && file.filename.starts_with("fabric-api-") {
            return Err("Fabric API wird von Hera verwaltet. Bitte eine Mod-Version wählen, die zur integrierten Fabric API 0.141.6 passt.".into());
        }
        let u = reqwest::Url::parse(&file.url).map_err(|_| "Ungültige Mod-URL")?;
        if (source == "modrinth" && u.host_str() != Some("cdn.modrinth.com"))
            || (source == "curseforge"
                && !matches!(
                    u.host_str(),
                    Some("edge.forgecdn.net" | "mediafilez.forgecdn.net" | "media.forgecdn.net")
                ))
        {
            return Err("Nicht freigegebene Mod-Downloadquelle".into());
        }
        if file.size > 128 * 1024 * 1024 {
            return Err("Mod-Datei zu groß".into());
        }
        if let Some(old) = existing
            .iter()
            .find(|m| m.source == source && m.project_id == v.project_id)
        {
            if old.version_id != v.id {
                return Err(format!(
                    "{} ist bereits in einer anderen Version installiert",
                    old.title
                ));
            }
        } else {
            let project = catalog_with_gateway(
                http,
                "project",
                &params(source, p, &v.project_id),
                Some(&gateway),
            )
            .await?;
            records.push(Installed {
                source: source.into(),
                project_id: v.project_id.clone(),
                version_id: v.id.clone(),
                title: str_field(&project, "title")?.into(),
                version: v.number.clone(),
                filename: file.filename.clone(),
                sha1: file.sha1.clone(),
                size: file.size,
            });
            files.push(file.clone());
        }
        for d in v.dependencies.iter().filter(|d| d.kind == "required") {
            let dep = if let Some(version_id) = &d.version_id {
                let mut q = params(source, p, version_id);
                q.insert("projectId".into(), d.project_id.clone().unwrap_or_default());
                serde_json::from_value::<Version>(
                    catalog_with_gateway(http, "version", &q, Some(&gateway)).await?,
                )
                .map_err(|_| "Ungültige Abhängigkeitsversion")?
            } else if let Some(project_id) = &d.project_id {
                let list: Vec<Version> = serde_json::from_value(
                    catalog_with_gateway(
                        http,
                        "versions",
                        &params(source, p, project_id),
                        Some(&gateway),
                    )
                    .await?,
                )
                .map_err(|_| "Ungültige Abhängigkeiten")?;
                list.iter()
                    .find(|v| {
                        v.file.as_ref().is_some_and(|f| {
                            crate::bundled_mods::satisfies_api(p, &f.filename, &f.sha1)
                        })
                    })
                    .or_else(|| list.iter().find(|v| v.release == "release"))
                    .or_else(|| list.first())
                    .cloned()
                    .ok_or("Keine passende Abhängigkeit verfügbar")?
            } else {
                return Err("Abhängigkeits-ID fehlt".into());
            };
            if d.project_id
                .as_ref()
                .is_some_and(|id| id != &dep.project_id)
            {
                return Err("Falsches Abhängigkeitsprojekt".into());
            }
            queue.push_back(dep);
        }
        chosen.insert(v.project_id.clone(), v);
    }
    let all = existing.iter().chain(records.iter()).collect::<Vec<_>>();
    for v in chosen.values() {
        for d in v.dependencies.iter().filter(|d| d.kind == "incompatible") {
            if chosen.values().any(|m| {
                d.project_id.as_ref().is_none_or(|id| id == &m.project_id)
                    && d.version_id.as_ref().is_none_or(|id| id == &m.id)
            }) || all.iter().any(|m| {
                m.source == source
                    && d.project_id.as_ref().is_none_or(|id| id == &m.project_id)
                    && d.version_id.as_ref().is_none_or(|id| id == &m.version_id)
            }) {
                return Err("Inkompatible Mods im Profil erkannt".into());
            }
        }
    }
    let mut names = std::collections::HashSet::new();
    for m in &all {
        if !names.insert(m.filename.to_lowercase()) {
            return Err("Kollision von Mod-Dateinamen".into());
        }
    }
    let game = root.join("profiles").join(&p.id);
    let mods = game.join("mods");
    tokio::fs::create_dir_all(&mods)
        .await
        .map_err(|e| e.to_string())?;
    let stage = game.join(format!(".mods-stage-{}", uuid::Uuid::new_v4()));
    tokio::fs::create_dir(&stage)
        .await
        .map_err(|e| e.to_string())?;
    let mut committed = vec![];
    let result = async {
        for f in &files {
            if tokio::fs::try_exists(mods.join(&f.filename))
                .await
                .map_err(|e| e.to_string())?
            {
                return Err("Eine Mod-Datei mit diesem Namen existiert bereits".into());
            }
            minecraft::download(
                http,
                &Artifact {
                    url: f.url.clone(),
                    sha1: f.sha1.clone(),
                    size: f.size,
                    path: None,
                },
                &stage.join(&f.filename),
            )
            .await?;
        }
        let manifest =
            serde_json::to_vec_pretty(&existing.iter().chain(records.iter()).collect::<Vec<_>>())
                .map_err(|e| e.to_string())?;
        tokio::fs::write(stage.join("hera-mods.json"), manifest)
            .await
            .map_err(|e| e.to_string())?;
        for f in &files {
            let dest = mods.join(&f.filename);
            tokio::fs::rename(stage.join(&f.filename), &dest)
                .await
                .map_err(|e| e.to_string())?;
            committed.push(dest);
        }
        let old = game.join("hera-mods.json");
        let backup = stage.join("previous.json");
        if old.exists() {
            tokio::fs::rename(&old, &backup)
                .await
                .map_err(|e| e.to_string())?;
        }
        if let Err(e) = tokio::fs::rename(stage.join("hera-mods.json"), &old).await {
            if backup.exists() {
                let _ = tokio::fs::rename(&backup, &old).await;
            }
            return Err(e.to_string());
        }
        Ok(())
    }
    .await;
    if result.is_err() {
        for file in committed {
            let _ = tokio::fs::remove_file(file).await;
        }
    }
    // stage is generated beneath the validated UUID profile; never supplied through IPC.
    let _ = tokio::fs::remove_dir_all(stage).await;
    result
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn catalog_accepts_profile_versions_and_rejects_invalid_targets() {
        for (loader, version) in [
            ("fabric", "1.21.11"),
            ("forge", "1.20.1"),
            ("fabric", "26.3"),
        ] {
            let p = HashMap::from([
                ("loader".into(), loader.into()),
                ("version".into(), version.into()),
            ]);
            assert_eq!(catalog_target(&p).unwrap(), (loader, version));
        }
        for (loader, version) in [
            ("vanilla", "1.21.11"),
            ("fabric", "../escape"),
            ("forge", ""),
        ] {
            let p = HashMap::from([
                ("loader".into(), loader.into()),
                ("version".into(), version.into()),
            ]);
            assert!(catalog_target(&p).is_err());
        }
    }
    #[tokio::test]
    #[ignore = "Downloads a real Modrinth mod into an isolated test profile"]
    async fn modrinth_install_smoke() {
        let root =
            std::path::PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../../.local/mod-smoke");
        let p = Profile {
            id: uuid::Uuid::new_v4().to_string(),
            name: "Mod test".into(),
            version: "1.21.11".into(),
            loader: "fabric".into(),
            memory_mb: 4096,
            java_path: "java".into(),
        };
        let http = minecraft::http().unwrap();
        let mut search = params("modrinth", &p, "");
        search.insert("query".into(), "sodium".into());
        let results = catalog(&http, "search", &search).await.unwrap();
        assert!(!results["projects"].as_array().unwrap().is_empty());
        let list: Vec<Version> = serde_json::from_value(
            catalog(&http, "versions", &params("modrinth", &p, "sodium"))
                .await
                .unwrap(),
        )
        .unwrap();
        let v = &list[0];
        install(&http, &root, &p, "modrinth", &v.project_id, &v.id)
            .await
            .unwrap();
        let records = installed(&root, &p).await.unwrap();
        assert!(records.iter().any(|m| m.project_id == v.project_id));
        let file = v.file.as_ref().unwrap();
        assert!(
            minecraft::valid(
                &root
                    .join("profiles")
                    .join(&p.id)
                    .join("mods")
                    .join(&file.filename),
                &Artifact {
                    url: file.url.clone(),
                    sha1: file.sha1.clone(),
                    size: file.size,
                    path: None
                }
            )
            .await
        );
        install(&http, &root, &p, "modrinth", &v.project_id, &v.id)
            .await
            .unwrap();
        assert_eq!(installed(&root, &p).await.unwrap().len(), records.len());
    }
    #[test]
    fn rejects_unsafe_names() {
        for n in [
            "../evil.jar",
            "a\\b.jar",
            "run.exe",
            ".hidden.jar",
            "C:evil.jar",
        ] {
            assert!(filename(n).is_err());
        }
        assert!(filename("sodium-1.0+mc1.21.1.jar").is_ok());
    }
    #[test]
    fn rejects_unsafe_ids() {
        for i in ["../x", "bad/id", ""] {
            assert!(id(i).is_err());
        }
    }
}
