//! Properties-file loader — port of `utils/Configuration.kt`.

use std::fs::File;
use std::io::BufReader;
use std::path::Path;

use crate::globals::GlobalsInner;

/// One parsed `config.properties` set.
#[derive(Debug, Clone, Default)]
pub struct Configuration {
    /// `target.folder` — emit-target directory root.
    pub src_folder: String,
    /// `target.package` — Java/Kotlin/Groovy package path (`com/example/...`).
    pub target_package: String,
    /// `file.name` — DDL source file (resolved against the resources dir).
    pub file_name: String,
    /// `target.language` — `java` | `kotlin` | `groovy` (default `java`).
    pub language: String,
    /// `database.type` — `mysql` | `postgresql` | `sqlite` | `mssql` (default `mysql`).
    pub database_type: String,
}

impl Configuration {
    /// Default `target.folder` for a given language when the key is absent.
    pub fn default_folder_for_language(language: &str) -> &'static str {
        match language {
            "kotlin" => "build/generated/src/main/kotlin/",
            "groovy" => "build/generated/src/main/groovy/",
            _ => "build/generated/src/main/java/",
        }
    }

    /// Build a `Configuration` from an already-parsed properties map.
    pub fn from_properties(props: &std::collections::HashMap<String, String>) -> Self {
        let language = props
            .get("target.language")
            .cloned()
            .unwrap_or_else(|| "java".into());
        let src_folder = props
            .get("target.folder")
            .cloned()
            .unwrap_or_else(|| Self::default_folder_for_language(&language).to_string());
        let target_package = props.get("target.package").cloned().unwrap_or_default();
        let file_name = props.get("file.name").cloned().unwrap_or_default();
        let database_type = props
            .get("database.type")
            .cloned()
            .unwrap_or_else(|| "mysql".into());
        Self {
            src_folder,
            target_package,
            file_name,
            language,
            database_type,
        }
    }

    /// Read a `.properties` file from disk and parse it.
    pub fn read_config<P: AsRef<Path>>(
        path: P,
    ) -> std::io::Result<std::collections::HashMap<String, String>> {
        let reader = BufReader::new(File::open(path)?);
        java_properties::read(reader).map_err(std::io::Error::other)
    }

    /// Convert this config into a [`GlobalsInner`] snapshot suitable for
    /// `Globals::set`.
    pub fn to_globals(&self) -> GlobalsInner {
        GlobalsInner {
            package: self.target_package.clone(),
            src_folder: self.src_folder.clone(),
            file_name: self.file_name.clone(),
            language: self.language.clone(),
            db_type: self.database_type.clone(),
        }
    }
}
