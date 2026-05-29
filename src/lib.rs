//! Rust port of the Kotlin Spring Boot REST generator.
//!
//! Reads a SQL DDL dump (MySQL, PostgreSQL, SQLite, or MSSQL), parses it
//! into [`entity::Entity`] structs, and emits a fully-wired Spring Boot REST API
//! (entities, controllers, DAOs, repositories) using the templates that
//! ship in `src/main/resources/templates/` — same layout the Kotlin
//! generator uses, so all four template families (Java, Kotlin, Groovy)
//! are unchanged.
//!
//! The original Kotlin source remains in
//! `src/main/kotlin/com/jakobmenke/bootrestgenerator/` for reference.

pub mod config;
pub mod constants;
pub mod entity;
pub mod globals;
pub mod normalize;
pub mod parser;
pub mod templates;
