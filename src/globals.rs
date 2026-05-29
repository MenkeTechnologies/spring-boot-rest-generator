//! Mutable global state — port of `utils/Globals.kt`.
//!
//! The Kotlin generator uses a `Globals` object as a process-wide
//! configuration sink; mirroring that here keeps the Util / Templates
//! ports straightforward (each peeks at the active language / DB type
//! without threading config through every call site).
//!
//! Reset between runs via [`Globals::set`].

use std::sync::RwLock;

use once_cell::sync::Lazy;

use crate::constants;

/// One snapshot of the generator's global config.
#[derive(Debug, Clone, Default)]
pub struct GlobalsInner {
    /// Target Java package, e.g. `com/example/users`.
    pub package: String,
    /// Output folder root, e.g. `build/generated/src/main/java`.
    pub src_folder: String,
    /// DDL source file (classpath-resource style filename).
    pub file_name: String,
    /// Target language: `java` | `kotlin` | `groovy`.
    pub language: String,
    /// Source database flavour: `mysql` | `postgresql` | `sqlite` | `mssql`.
    pub db_type: String,
}

static G: Lazy<RwLock<GlobalsInner>> = Lazy::new(|| {
    RwLock::new(GlobalsInner {
        language: "java".into(),
        db_type: "mysql".into(),
        ..Default::default()
    })
});

/// Static façade matching the Kotlin `Globals` object.
pub struct Globals;

impl Globals {
    /// Snapshot the current globals.
    pub fn get() -> GlobalsInner {
        G.read().expect("globals read lock").clone()
    }

    /// Replace the current globals.
    pub fn set(new: GlobalsInner) {
        *G.write().expect("globals write lock") = new;
    }

    /// True if the current target language is Kotlin.
    pub fn is_kotlin() -> bool {
        Self::get().language.eq_ignore_ascii_case("kotlin")
    }
    /// True if the current target language is Groovy.
    pub fn is_groovy() -> bool {
        Self::get().language.eq_ignore_ascii_case("groovy")
    }
    /// True if the source DDL is PostgreSQL.
    pub fn is_postgresql() -> bool {
        Self::get().db_type.eq_ignore_ascii_case("postgresql")
    }
    /// True if the source DDL is SQLite.
    pub fn is_sqlite() -> bool {
        Self::get().db_type.eq_ignore_ascii_case("sqlite")
    }
    /// True if the source DDL is MSSQL.
    pub fn is_mssql() -> bool {
        Self::get().db_type.eq_ignore_ascii_case("mssql")
    }

    /// Identifier-escape character for the current source DB.
    pub fn escape_character() -> &'static str {
        if Self::is_postgresql() || Self::is_sqlite() {
            constants::PG_DB_ESCAPE_CHARACTER
        } else if Self::is_mssql() {
            constants::MSSQL_DB_ESCAPE_OPEN
        } else {
            constants::DB_ESCAPE_CHARACTER
        }
    }

    /// File extension for the current target language.
    pub fn file_extension() -> &'static str {
        if Self::is_kotlin() {
            ".kt"
        } else if Self::is_groovy() {
            ".groovy"
        } else {
            ".java"
        }
    }
}
