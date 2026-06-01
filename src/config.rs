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
            "rust-loco" | "loco" => "build/generated/loco/",
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

#[cfg(test)]
mod tests {
    //! Config-loader pins. These functions decide where generated code
    //! lands on disk, which language template tree gets used, and how
    //! defaults fill in for missing properties. A regression here would
    //! corrupt every downstream generation invocation; `parity_smoke`
    //! never exercises the properties-file loader so direct unit pins
    //! are the only coverage these paths get.

    use super::*;
    use std::collections::HashMap;

    fn props(pairs: &[(&str, &str)]) -> HashMap<String, String> {
        pairs
            .iter()
            .map(|(k, v)| ((*k).into(), (*v).into()))
            .collect()
    }

    // -------------------------------- default_folder_for_language

    #[test]
    fn default_folder_for_java_returns_java_main_folder() {
        assert_eq!(
            Configuration::default_folder_for_language("java"),
            "build/generated/src/main/java/"
        );
    }

    #[test]
    fn default_folder_for_kotlin_returns_kotlin_main_folder() {
        assert_eq!(
            Configuration::default_folder_for_language("kotlin"),
            "build/generated/src/main/kotlin/"
        );
    }

    #[test]
    fn default_folder_for_groovy_returns_groovy_main_folder() {
        assert_eq!(
            Configuration::default_folder_for_language("groovy"),
            "build/generated/src/main/groovy/"
        );
    }

    #[test]
    fn default_folder_for_unknown_falls_through_to_java() {
        // Fallback path keeps the generator safe for unknown language
        // tokens — matches the Kotlin port's `else` branch.
        assert_eq!(
            Configuration::default_folder_for_language(""),
            "build/generated/src/main/java/"
        );
        assert_eq!(
            Configuration::default_folder_for_language("rust"),
            "build/generated/src/main/java/"
        );
    }

    #[test]
    fn default_folder_for_language_is_case_sensitive() {
        // `Kotlin` (capitalised) falls through to java — pin this so
        // property files with stray capitalisation route consistently.
        assert_eq!(
            Configuration::default_folder_for_language("Kotlin"),
            "build/generated/src/main/java/"
        );
    }

    // ------------------------------------------ from_properties

    #[test]
    fn from_properties_empty_map_uses_all_defaults() {
        let cfg = Configuration::from_properties(&props(&[]));
        assert_eq!(cfg.language, "java");
        assert_eq!(cfg.database_type, "mysql");
        assert_eq!(cfg.src_folder, "build/generated/src/main/java/");
        assert_eq!(cfg.target_package, "");
        assert_eq!(cfg.file_name, "");
    }

    #[test]
    fn from_properties_explicit_kotlin_language_drives_folder_default() {
        let cfg = Configuration::from_properties(&props(&[("target.language", "kotlin")]));
        assert_eq!(cfg.language, "kotlin");
        // src_folder defaulted via default_folder_for_language(language).
        assert_eq!(cfg.src_folder, "build/generated/src/main/kotlin/");
    }

    #[test]
    fn from_properties_explicit_target_folder_overrides_language_default() {
        let cfg = Configuration::from_properties(&props(&[
            ("target.language", "kotlin"),
            ("target.folder", "out/custom/"),
        ]));
        // Explicit `target.folder` wins — language-default only fills when key absent.
        assert_eq!(cfg.src_folder, "out/custom/");
    }

    #[test]
    fn from_properties_explicit_db_type_propagates() {
        let cfg = Configuration::from_properties(&props(&[("database.type", "postgresql")]));
        assert_eq!(cfg.database_type, "postgresql");
    }

    #[test]
    fn from_properties_target_package_passes_through_verbatim() {
        let cfg = Configuration::from_properties(&props(&[("target.package", "com/foo/bar")]));
        // No path-normalisation here; `Templates` does the `/`→`.`.
        assert_eq!(cfg.target_package, "com/foo/bar");
    }

    #[test]
    fn from_properties_file_name_passes_through() {
        let cfg = Configuration::from_properties(&props(&[("file.name", "schema.sql")]));
        assert_eq!(cfg.file_name, "schema.sql");
    }

    #[test]
    fn from_properties_ignores_unknown_keys() {
        // Foreign keys must not pollute any Configuration field.
        let cfg = Configuration::from_properties(&props(&[("does.not.exist", "ignored")]));
        assert_eq!(cfg.language, "java");
        assert_eq!(cfg.database_type, "mysql");
    }

    #[test]
    fn from_properties_full_kotlin_config_round_trips_fields() {
        let cfg = Configuration::from_properties(&props(&[
            ("target.language", "kotlin"),
            ("target.folder", "k/out/"),
            ("target.package", "com/k"),
            ("file.name", "k.sql"),
            ("database.type", "mssql"),
        ]));
        assert_eq!(cfg.language, "kotlin");
        assert_eq!(cfg.src_folder, "k/out/");
        assert_eq!(cfg.target_package, "com/k");
        assert_eq!(cfg.file_name, "k.sql");
        assert_eq!(cfg.database_type, "mssql");
    }

    // ------------------------------------------------ to_globals

    #[test]
    fn to_globals_copies_all_fields_into_globals_inner() {
        let cfg = Configuration {
            src_folder: "out/".into(),
            target_package: "com/x".into(),
            file_name: "x.sql".into(),
            language: "groovy".into(),
            database_type: "sqlite".into(),
        };
        let g = cfg.to_globals();
        assert_eq!(g.package, "com/x");
        assert_eq!(g.src_folder, "out/");
        assert_eq!(g.file_name, "x.sql");
        assert_eq!(g.language, "groovy");
        assert_eq!(g.db_type, "sqlite");
    }

    #[test]
    fn to_globals_does_not_mutate_source_configuration() {
        // to_globals clones every field; calling it must leave the
        // source Configuration intact for repeat use.
        let cfg = Configuration {
            src_folder: "a/".into(),
            target_package: "b".into(),
            file_name: "c".into(),
            language: "java".into(),
            database_type: "mysql".into(),
        };
        let _ = cfg.to_globals();
        let _ = cfg.to_globals();
        assert_eq!(cfg.src_folder, "a/");
        assert_eq!(cfg.target_package, "b");
    }

    #[test]
    fn from_properties_then_to_globals_round_trips_through_globals_inner() {
        let cfg = Configuration::from_properties(&props(&[
            ("target.language", "kotlin"),
            ("target.folder", "k/"),
            ("target.package", "com/r"),
            ("file.name", "r.sql"),
            ("database.type", "postgresql"),
        ]));
        let g = cfg.to_globals();
        assert_eq!(g.language, "kotlin");
        assert_eq!(g.src_folder, "k/");
        assert_eq!(g.package, "com/r");
        assert_eq!(g.file_name, "r.sql");
        assert_eq!(g.db_type, "postgresql");
    }
}
