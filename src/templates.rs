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
