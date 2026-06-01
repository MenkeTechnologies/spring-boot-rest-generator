//! Generator entry point — port of `Main.kt`.
//!
//! Reads `src/main/resources/config.properties`, parses the configured
//! DDL file, and writes the generated entities / controllers / DAOs /
//! repositories under `<target.folder>/<target.package>/`.

use std::fs;
use std::path::{Path, PathBuf};

use api_rest_generator::config::Configuration;
use api_rest_generator::entity::Entity;
use api_rest_generator::globals::Globals;
use api_rest_generator::loco;
use api_rest_generator::normalize::{
    normalize_mssql_words, normalize_postgresql_words, normalize_sqlite_words,
};
use api_rest_generator::parser::{get_words, parse_words};
use api_rest_generator::templates::Templates;

const RESOURCES_DIR: &str = "src/main/resources";

fn main() -> std::io::Result<()> {
    let resources = PathBuf::from(RESOURCES_DIR);
    let config_path = resources.join("config.properties");
    let props = Configuration::read_config(&config_path)?;
    let cfg = Configuration::from_properties(&props);
    Globals::set(cfg.to_globals());

    let dump_path = resources.join(&cfg.file_name);
    let file = fs::File::open(&dump_path)?;
    let mut words = get_words(file);

    if Globals::is_postgresql() {
        normalize_postgresql_words(&mut words);
    } else if Globals::is_sqlite() {
        normalize_sqlite_words(&mut words);
    } else if Globals::is_mssql() {
        normalize_mssql_words(&mut words);
    }

    let entities = parse_words(&words);

    if Globals::is_rust_loco() {
        write_loco(&entities, &cfg)?;
        eprintln!(
            "Generated {} Loco entit{} (models + controllers + migrations) under {}",
            entities.len(),
            if entities.len() == 1 { "y" } else { "ies" },
            cfg.src_folder,
        );
        return Ok(());
    }

    let templates = Templates::from_resources_dir(resources);
    write_templates(&templates, &entities, &cfg)?;

    eprintln!(
        "Generated {} entit{} under {}/{}",
        entities.len(),
        if entities.len() == 1 { "y" } else { "ies" },
        cfg.src_folder,
        cfg.target_package
    );
    Ok(())
}

fn write_loco(entities: &[Entity], cfg: &Configuration) -> std::io::Result<()> {
    let root = PathBuf::from(&cfg.src_folder);
    loco::write_loco_project(entities, &root)?;
    Ok(())
}

fn write_templates(
    templates: &Templates,
    entities: &[Entity],
    cfg: &Configuration,
) -> std::io::Result<()> {
    let ext = Globals::file_extension();
    for entity in entities {
        let entity_tmpl = templates.get_entity_template(entity, &cfg.target_package)?;
        write_file(
            cfg,
            "entity",
            &format!("{}{}", entity.entity_name, ext),
            &entity_tmpl,
        )?;

        let service_tmpl =
            templates.get_resource_template(&cfg.target_package, &entity.entity_name)?;
        write_file(
            cfg,
            "rest",
            &format!("{}Resource{}", entity.entity_name, ext),
            &service_tmpl,
        )?;

        let dao_tmpl = templates.get_dao_template(&cfg.target_package, &entity.entity_name)?;
        write_file(
            cfg,
            "dao",
            &format!("{}Dao{}", entity.entity_name, ext),
            &dao_tmpl,
        )?;

        let repo_tmpl =
            templates.get_repository_template(&cfg.target_package, &entity.entity_name)?;
        write_file(
            cfg,
            "repository",
            &format!("{}Repository{}", entity.entity_name, ext),
            &repo_tmpl,
        )?;
    }
    let constants_tmpl = templates.get_file_template_by_name(&cfg.target_package, "constants")?;
    write_file(
        cfg,
        "utils",
        &format!("GlobalConstants{}", ext),
        &constants_tmpl,
    )?;

    let generic_dao_tmpl =
        templates.get_file_template_by_name(&cfg.target_package, "genericdao")?;
    write_file(cfg, "dao", &format!("GenericDao{}", ext), &generic_dao_tmpl)?;
    Ok(())
}

fn write_file(cfg: &Configuration, folder: &str, file: &str, body: &str) -> std::io::Result<()> {
    let dir: PathBuf = Path::new(&cfg.src_folder)
        .join(&cfg.target_package)
        .join(folder);
    fs::create_dir_all(&dir)?;
    fs::write(dir.join(file), body)?;
    Ok(())
}
