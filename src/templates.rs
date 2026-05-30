//! Template loader + renderer — port of `templates/Templates.kt`.
//!
//! Loads the `.tmpl` files that ship in `src/main/resources/templates/`
//! (or `templates/kotlin/` / `templates/groovy/` per target language)
//! and substitutes the `{{mainPackageName}}`, `{{entityName}}`, etc.
//! placeholders. The template syntax is intentionally Kotlin-compatible
//! so the same `.tmpl` files work for both ports.

use std::fs;
use std::path::PathBuf;

use crate::constants;
use crate::entity::Entity;
use crate::globals::Globals;

/// Template engine handle. `resources_dir` should be the path that
/// contains the `templates/` subtree (typically `src/main/resources/`).
pub struct Templates {
    /// Root that contains `templates/{kotlin,groovy,*.tmpl}`.
    pub resources_dir: PathBuf,
}

impl Templates {
    /// Construct with the conventional `src/main/resources/` layout.
    pub fn from_resources_dir<P: Into<PathBuf>>(dir: P) -> Self {
        Self {
            resources_dir: dir.into(),
        }
    }

    /// Sub-folder under `templates/` for the active language.
    fn template_dir(&self) -> &'static str {
        if Globals::is_kotlin() {
            "templates/kotlin"
        } else if Globals::is_groovy() {
            "templates/groovy"
        } else {
            "templates"
        }
    }

    fn read(&self, file: &str) -> std::io::Result<String> {
        let path = self.resources_dir.join(file);
        fs::read_to_string(&path)
    }

    /// `rest.resource.tmpl` — REST controller.
    pub fn get_resource_template(
        &self,
        main_package: &str,
        entity_name: &str,
    ) -> std::io::Result<String> {
        let pkg = main_package.replace('/', ".");
        let path = format!("{}/rest.resource.tmpl", self.template_dir());
        Ok(self
            .read(&path)?
            .replace("{{mainPackageName}}", &pkg)
            .replace("{{entityName}}", entity_name)
            .replace("{{serviceName}}", &entity_name.to_lowercase())
            .replace("{{restServicePrefix}}", "GlobalConstants.CONTEXT_PATH"))
    }

    /// `dao.tmpl` — service / DAO layer for one entity.
    pub fn get_dao_template(
        &self,
        main_package: &str,
        entity_name: &str,
    ) -> std::io::Result<String> {
        let pkg = main_package.replace('/', ".");
        let camel_repo = {
            let mut chars = entity_name.chars();
            let first = chars
                .next()
                .map(|c| c.to_lowercase().to_string())
                .unwrap_or_default();
            format!("{first}{}Repository", chars.as_str())
        };
        let path = format!("{}/dao.tmpl", self.template_dir());
        Ok(self
            .read(&path)?
            .replace("{{mainPackageName}}", &pkg)
            .replace("{{entityName}}", entity_name)
            .replace("{{camelRepositoryName}}", &camel_repo))
    }

    /// Bare template with only `{{mainPackageName}}` substituted —
    /// used for `constants.tmpl` / `genericdao.tmpl`.
    pub fn get_file_template_by_name(
        &self,
        main_package: &str,
        file_name: &str,
    ) -> std::io::Result<String> {
        let pkg = main_package.replace('/', ".");
        let path = format!("{}/{file_name}.tmpl", self.template_dir());
        Ok(self.read(&path)?.replace("{{mainPackageName}}", &pkg))
    }

    /// `repository.tmpl` — Spring Data repository for one entity.
    pub fn get_repository_template(
        &self,
        main_package: &str,
        entity_name: &str,
    ) -> std::io::Result<String> {
        let pkg = main_package.replace('/', ".");
        let path = format!("{}/repository.tmpl", self.template_dir());
        Ok(self
            .read(&path)?
            .replace("{{mainPackageName}}", &pkg)
            .replace("{{entityName}}", entity_name)
            .replace("{{primaryKeyType}}", constants::PK_DATA_TYPE))
    }

    /// `entity.tmpl` + appended field bodies for one entity.
    pub fn get_entity_template(
        &self,
        entity: &Entity,
        main_package: &str,
    ) -> std::io::Result<String> {
        let pkg = main_package.replace('/', ".");
        let path = format!("{}/entity.tmpl", self.template_dir());
        let header = self
            .read(&path)?
            .replace("{{mainPackageName}}", &pkg)
            .replace("{{entityName}}", &entity.entity_name)
            .replace("{{tableName}}", &entity.table_name);

        let mut body = String::new();
        if Globals::is_kotlin() {
            build_kotlin_entity_fields(&mut body, entity);
        } else if Globals::is_groovy() {
            build_groovy_entity_fields(&mut body, entity);
        } else {
            build_java_entity_fields(&mut body, entity);
        }
        body.push('}');
        Ok(header + &body)
    }
}

fn build_java_entity_fields(sb: &mut String, entity: &Entity) {
    let indent = "    ";
    for col in &entity.columns {
        if let Some(id) = &col.database_id_type {
            sb.push_str(indent);
            sb.push_str(id);
            sb.push('\n');
        }
        if col
            .database_id_type
            .as_deref()
            .map(|s| s.eq_ignore_ascii_case("@ManyToOne"))
            .unwrap_or(false)
        {
            sb.push_str(indent);
            sb.push_str("@JoinColumn(name = \"");
            sb.push_str(col.database_column_name.as_deref().unwrap_or(""));
            sb.push_str("\")\n");
        } else {
            sb.push_str(indent);
            sb.push_str("@Column(name = \"");
            sb.push_str(col.database_column_name.as_deref().unwrap_or(""));
            sb.push_str("\")\n");
        }
        sb.push_str(indent);
        sb.push_str("private ");
        sb.push_str(col.java_type.as_deref().unwrap_or(""));
        sb.push(' ');
        sb.push_str(col.camel_case_field_name.as_deref().unwrap_or(""));
        sb.push_str(";\n\n");
    }
}

fn build_groovy_entity_fields(sb: &mut String, entity: &Entity) {
    let indent = "    ";
    for col in &entity.columns {
        if let Some(id) = &col.database_id_type {
            sb.push_str(indent);
            sb.push_str(id);
            sb.push('\n');
        }
        if col
            .database_id_type
            .as_deref()
            .map(|s| s.eq_ignore_ascii_case("@ManyToOne"))
            .unwrap_or(false)
        {
            sb.push_str(indent);
            sb.push_str("@JoinColumn(name = \"");
            sb.push_str(col.database_column_name.as_deref().unwrap_or(""));
            sb.push_str("\")\n");
        } else {
            sb.push_str(indent);
            sb.push_str("@Column(name = \"");
            sb.push_str(col.database_column_name.as_deref().unwrap_or(""));
            sb.push_str("\")\n");
        }
        sb.push_str(indent);
        sb.push_str(col.java_type.as_deref().unwrap_or(""));
        sb.push(' ');
        sb.push_str(col.camel_case_field_name.as_deref().unwrap_or(""));
        sb.push_str("\n\n");
    }
}

fn build_kotlin_entity_fields(sb: &mut String, entity: &Entity) {
    let indent = "    ";
    for col in &entity.columns {
        if let Some(id) = &col.database_id_type {
            sb.push_str(indent);
            sb.push_str(id);
            sb.push('\n');
        }
        if col
            .database_id_type
            .as_deref()
            .map(|s| s.eq_ignore_ascii_case("@ManyToOne"))
            .unwrap_or(false)
        {
            sb.push_str(indent);
            sb.push_str("@JoinColumn(name = \"");
            sb.push_str(col.database_column_name.as_deref().unwrap_or(""));
            sb.push_str("\")\n");
        } else {
            sb.push_str(indent);
            sb.push_str("@Column(name = \"");
            sb.push_str(col.database_column_name.as_deref().unwrap_or(""));
            sb.push_str("\")\n");
        }
        sb.push_str(indent);
        sb.push_str("var ");
        sb.push_str(col.camel_case_field_name.as_deref().unwrap_or(""));
        sb.push_str(": ");
        let java_type = col.java_type.as_deref().unwrap_or("");
        sb.push_str(java_type);
        let default = match java_type {
            "String" => " = \"\"",
            "Int" => " = 0",
            "Long" => " = 0L",
            "Float" => " = 0f",
            "Double" => " = 0.0",
            "Boolean" => " = false",
            "LocalDate" | "LocalTime" | "LocalDateTime" => "? = null",
            _ => "? = null",
        };
        sb.push_str(default);
        sb.push_str("\n\n");
    }
}

#[cfg(test)]
mod tests {
    //! Unit pins for the private per-language entity-field renderers.
    //! These functions emit the heart of every generated POJO / data class
    //! / Groovy class, so a wrong annotation or missing default-value path
    //! produces compile errors in user-facing output. parity_smoke covers
    //! end-to-end shape on real DDL fixtures but doesn't catch single-cell
    //! template drift (`var` vs `val`, `@Column` vs `@JoinColumn`, missing
    //! Kotlin defaults). These pins fill that gap.

    use super::*;
    use crate::entity::{ColumnToField, Entity};

    fn col(name: &str, jt: &str, id: Option<&str>, camel: &str) -> ColumnToField {
        ColumnToField {
            database_id_type: id.map(String::from),
            database_column_name: Some(name.into()),
            camel_case_field_name: Some(camel.into()),
            database_type: None,
            java_type: Some(jt.into()),
        }
    }

    fn entity_with(cols: Vec<ColumnToField>) -> Entity {
        Entity {
            table_name: "users".into(),
            entity_name: "Users".into(),
            columns: cols,
        }
    }

    // ---------------------------------------------------- Java renderer

    #[test]
    fn java_plain_column_renders_column_annotation_and_private_decl() {
        let e = entity_with(vec![col("name", "String", None, "name")]);
        let mut sb = String::new();
        build_java_entity_fields(&mut sb, &e);
        assert!(sb.contains("@Column(name = \"name\")"));
        assert!(sb.contains("private String name;"));
        // No @Id, no @JoinColumn for a plain column.
        assert!(!sb.contains("@Id"));
        assert!(!sb.contains("@JoinColumn"));
    }

    #[test]
    fn java_primary_key_renders_id_annotation_then_column() {
        let e = entity_with(vec![col("id", "Long", Some("@Id"), "id")]);
        let mut sb = String::new();
        build_java_entity_fields(&mut sb, &e);
        // @Id appears BEFORE @Column on the same field.
        let id_pos = sb.find("@Id").expect("@Id present");
        let col_pos = sb.find("@Column").expect("@Column present");
        assert!(id_pos < col_pos, "@Id must precede @Column");
        assert!(sb.contains("private Long id;"));
    }

    #[test]
    fn java_foreign_key_renders_many_to_one_and_join_column_not_column() {
        let e = entity_with(vec![col("user_id", "User", Some("@ManyToOne"), "user")]);
        let mut sb = String::new();
        build_java_entity_fields(&mut sb, &e);
        assert!(sb.contains("@ManyToOne"));
        assert!(sb.contains("@JoinColumn(name = \"user_id\")"));
        // FK path uses @JoinColumn, NOT @Column.
        assert!(!sb.contains("@Column(name = \"user_id\")"));
        assert!(sb.contains("private User user;"));
    }

    #[test]
    fn java_many_to_one_match_is_case_insensitive() {
        let e = entity_with(vec![col("user_id", "User", Some("@manytoone"), "user")]);
        let mut sb = String::new();
        build_java_entity_fields(&mut sb, &e);
        // Mixed-case `@manytoone` must still route to the JoinColumn branch.
        assert!(sb.contains("@JoinColumn(name = \"user_id\")"));
        assert!(!sb.contains("@Column(name = \"user_id\")"));
    }

    #[test]
    fn java_renders_columns_in_declaration_order() {
        let e = entity_with(vec![
            col("a", "String", None, "a"),
            col("b", "Long", None, "b"),
        ]);
        let mut sb = String::new();
        build_java_entity_fields(&mut sb, &e);
        let a_pos = sb.find("private String a").expect("a present");
        let b_pos = sb.find("private Long b").expect("b present");
        assert!(a_pos < b_pos, "columns must render in declaration order");
    }

    #[test]
    fn java_empty_entity_emits_nothing() {
        let e = entity_with(vec![]);
        let mut sb = String::new();
        build_java_entity_fields(&mut sb, &e);
        assert!(sb.is_empty(), "empty entity must emit no field text");
    }

    #[test]
    fn java_decl_ends_with_semicolon() {
        let e = entity_with(vec![col("x", "Integer", None, "x")]);
        let mut sb = String::new();
        build_java_entity_fields(&mut sb, &e);
        assert!(
            sb.contains("private Integer x;"),
            "Java field decl must end with `;`"
        );
    }

    // ---------------------------------------------------- Groovy renderer

    #[test]
    fn groovy_plain_column_omits_private_keyword_and_semicolon() {
        let e = entity_with(vec![col("name", "String", None, "name")]);
        let mut sb = String::new();
        build_groovy_entity_fields(&mut sb, &e);
        assert!(sb.contains("@Column(name = \"name\")"));
        assert!(sb.contains("String name\n"));
        // Groovy: no `private`, no trailing `;`.
        assert!(!sb.contains("private "));
        assert!(!sb.contains("name;"));
    }

    #[test]
    fn groovy_foreign_key_routes_to_join_column() {
        let e = entity_with(vec![col("user_id", "User", Some("@ManyToOne"), "user")]);
        let mut sb = String::new();
        build_groovy_entity_fields(&mut sb, &e);
        assert!(sb.contains("@ManyToOne"));
        assert!(sb.contains("@JoinColumn(name = \"user_id\")"));
    }

    // ---------------------------------------------------- Kotlin renderer

    #[test]
    fn kotlin_plain_column_uses_var_and_string_default() {
        let e = entity_with(vec![col("name", "String", None, "name")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("@Column(name = \"name\")"));
        assert!(sb.contains("var name: String = \"\""));
    }

    #[test]
    fn kotlin_int_column_has_zero_default() {
        let e = entity_with(vec![col("count", "Int", None, "count")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("var count: Int = 0"));
    }

    #[test]
    fn kotlin_long_column_has_zero_l_default() {
        let e = entity_with(vec![col("id", "Long", Some("@Id"), "id")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("var id: Long = 0L"));
    }

    #[test]
    fn kotlin_float_column_has_zero_f_default() {
        let e = entity_with(vec![col("ratio", "Float", None, "ratio")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("var ratio: Float = 0f"));
    }

    #[test]
    fn kotlin_double_column_has_zero_point_zero_default() {
        let e = entity_with(vec![col("rate", "Double", None, "rate")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("var rate: Double = 0.0"));
    }

    #[test]
    fn kotlin_boolean_column_has_false_default() {
        let e = entity_with(vec![col("active", "Boolean", None, "active")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("var active: Boolean = false"));
    }

    #[test]
    fn kotlin_local_date_column_is_nullable_with_null_default() {
        let e = entity_with(vec![col("dob", "LocalDate", None, "dob")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        // The `?` makes the type nullable + `= null` is the literal default.
        assert!(sb.contains("var dob: LocalDate? = null"));
    }

    #[test]
    fn kotlin_local_time_column_is_nullable() {
        let e = entity_with(vec![col("at", "LocalTime", None, "at")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("var at: LocalTime? = null"));
    }

    #[test]
    fn kotlin_local_date_time_column_is_nullable() {
        let e = entity_with(vec![col("ts", "LocalDateTime", None, "ts")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("var ts: LocalDateTime? = null"));
    }

    #[test]
    fn kotlin_unknown_type_falls_through_to_nullable_null_default() {
        // `User` is a custom entity type — must default to `? = null`.
        let e = entity_with(vec![col("user_id", "User", Some("@ManyToOne"), "user")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("var user: User? = null"));
    }

    #[test]
    fn kotlin_foreign_key_routes_to_join_column() {
        let e = entity_with(vec![col("user_id", "User", Some("@ManyToOne"), "user")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        assert!(sb.contains("@JoinColumn(name = \"user_id\")"));
        assert!(!sb.contains("@Column(name = \"user_id\")"));
    }

    #[test]
    fn kotlin_primary_key_emits_id_annotation_before_column() {
        let e = entity_with(vec![col("id", "Long", Some("@Id"), "id")]);
        let mut sb = String::new();
        build_kotlin_entity_fields(&mut sb, &e);
        let id_pos = sb.find("@Id").expect("@Id present");
        let col_pos = sb.find("@Column").expect("@Column present");
        assert!(id_pos < col_pos, "@Id must precede @Column in Kotlin output");
    }

    // ---------------------------------------- cross-language shape pins

    #[test]
    fn all_three_renderers_indent_with_four_spaces() {
        let e = entity_with(vec![col("name", "String", None, "name")]);
        let renderers: [(&str, fn(&mut String, &Entity)); 3] = [
            ("java", build_java_entity_fields),
            ("groovy", build_groovy_entity_fields),
            ("kotlin", build_kotlin_entity_fields),
        ];
        for (label, render) in renderers {
            let mut sb = String::new();
            render(&mut sb, &e);
            assert!(
                sb.contains("    @Column"),
                "{label} renderer must indent annotations with 4 spaces"
            );
        }
    }

    #[test]
    fn all_three_renderers_emit_blank_line_between_columns() {
        let e = entity_with(vec![
            col("a", "String", None, "a"),
            col("b", "String", None, "b"),
        ]);
        let renderers: [(&str, fn(&mut String, &Entity)); 3] = [
            ("java", build_java_entity_fields),
            ("groovy", build_groovy_entity_fields),
            ("kotlin", build_kotlin_entity_fields),
        ];
        for (label, render) in renderers {
            let mut sb = String::new();
            render(&mut sb, &e);
            assert!(
                sb.contains("\n\n"),
                "{label} renderer must separate columns with a blank line"
            );
        }
    }
}
