//! `Entity` and `ColumnToField` — port of `dto/Entity.kt` and `dto/ColumnToField.kt`.

/// Per-column metadata extracted from a `CREATE TABLE` statement.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ColumnToField {
    /// JPA id annotation: `@Id` for primary keys, `@ManyToOne` for foreign
    /// keys, `None` for plain columns.
    pub database_id_type: Option<String>,
    /// Raw column name as it appears in the DDL.
    pub database_column_name: Option<String>,
    /// snake_case → camelCase converted name (with trailing `Id`/`id`
    /// stripped on FK rewrites, mirroring the Kotlin behaviour).
    pub camel_case_field_name: Option<String>,
    /// Raw DDL data-type token (e.g. `varchar(255)`, `bigint`).
    pub database_type: Option<String>,
    /// Mapped JVM type (`String`, `Long`, `Integer`, `LocalDateTime`, …).
    pub java_type: Option<String>,
}

/// One table → one `Entity`. The columns list is mutated in-place during
/// parsing as later statements (ALTER TABLE ADD CONSTRAINT, inline PRIMARY
/// KEY clauses, FK REFERENCES) refine column shape.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Entity {
    /// Raw table name as it appears after `CREATE TABLE`.
    pub table_name: String,
    /// `PascalCase` entity class name derived from the table name.
    pub entity_name: String,
    /// Columns in declaration order.
    pub columns: Vec<ColumnToField>,
}
