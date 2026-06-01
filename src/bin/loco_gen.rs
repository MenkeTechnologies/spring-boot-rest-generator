//! `loco-gen` — Rust CLI to generate a full [Loco](https://loco.rs) REST
//! API from a raw SQL DDL dump.
//!
//! Two subcommands:
//!
//! * `loco-gen new`      — runs `loco new`, generates entities + controllers
//!                          + migrations from the DDL, wires `src/app.rs`,
//!                          and (optionally) runs `cargo loco db migrate`.
//! * `loco-gen generate` — emits the Loco file tree into an existing project
//!                          directory (no scaffold, no wiring).
//!
//! Example:
//! ```bash
//! loco-gen new --ddl ./mysql_dump.sql --name myapi --db sqlite --wire --migrate
//! cd myapi && cargo loco start
//! ```

use std::fs;
use std::path::{Path, PathBuf};
use std::process::{Command, ExitCode};

use clap::{Parser, Subcommand, ValueEnum};

use api_rest_generator::entity::Entity;
use api_rest_generator::globals::Globals;
use api_rest_generator::loco;
use api_rest_generator::normalize::{
    normalize_mssql_words, normalize_postgresql_words, normalize_sqlite_words,
};
use api_rest_generator::parser::{get_words, parse_words};

#[derive(Parser, Debug)]
#[command(
    name = "loco-gen",
    version,
    about = "Generate a Loco (Axum + SeaORM) REST API from a SQL DDL dump",
    long_about = None,
)]
struct Cli {
    #[command(subcommand)]
    command: Cmd,
}

#[derive(Subcommand, Debug)]
enum Cmd {
    /// Scaffold a fresh Loco project with `loco new`, then generate entities,
    /// controllers, and migrations from the DDL dump.
    New(NewArgs),

    /// Emit Loco entities, controllers, and migrations into an existing
    /// project directory (no scaffold, no wiring).
    Generate(GenerateArgs),
}

#[derive(Parser, Debug)]
struct NewArgs {
    /// Path to the SQL DDL dump (e.g. mysqldump --no-data output).
    #[arg(short, long, value_name = "FILE")]
    ddl: PathBuf,

    /// Project name (will become the directory name and crate name).
    #[arg(short, long)]
    name: String,

    /// Directory in which to create the project. Defaults to current dir.
    #[arg(short, long, value_name = "DIR", default_value = ".")]
    out: PathBuf,

    /// DDL dialect.
    #[arg(long, value_enum, default_value_t = Db::Mysql)]
    db: Db,

    /// Database driver baked into the Loco project.
    #[arg(long, value_enum, default_value_t = LocoDb::Sqlite)]
    loco_db: LocoDb,

    /// Patch `src/app.rs` to register every generated controller's routes.
    #[arg(long)]
    wire: bool,

    /// Run `cargo loco db migrate` after generation.
    #[arg(long)]
    migrate: bool,

    /// Path to the `loco` CLI. Defaults to `loco` on PATH.
    #[arg(long, default_value = "loco")]
    loco_bin: String,
}

#[derive(Parser, Debug)]
struct GenerateArgs {
    /// Path to the SQL DDL dump.
    #[arg(short, long, value_name = "FILE")]
    ddl: PathBuf,

    /// Existing Loco project root.
    #[arg(short, long, value_name = "DIR")]
    out: PathBuf,

    /// DDL dialect.
    #[arg(long, value_enum, default_value_t = Db::Mysql)]
    db: Db,

    /// Patch `src/app.rs` to register every generated controller's routes.
    #[arg(long)]
    wire: bool,
}

#[derive(Copy, Clone, Debug, ValueEnum)]
enum Db {
    Mysql,
    Postgresql,
    Sqlite,
    Mssql,
}

impl Db {
    fn as_globals_str(self) -> &'static str {
        match self {
            Db::Mysql => "mysql",
            Db::Postgresql => "postgresql",
            Db::Sqlite => "sqlite",
            Db::Mssql => "mssql",
        }
    }
}

#[derive(Copy, Clone, Debug, ValueEnum)]
enum LocoDb {
    Sqlite,
    Postgres,
}

impl LocoDb {
    fn as_str(self) -> &'static str {
        match self {
            LocoDb::Sqlite => "sqlite",
            LocoDb::Postgres => "postgres",
        }
    }
}

fn main() -> ExitCode {
    let cli = Cli::parse();
    let res = match cli.command {
        Cmd::New(args) => run_new(args),
        Cmd::Generate(args) => run_generate(args),
    };
    match res {
        Ok(()) => ExitCode::SUCCESS,
        Err(e) => {
            eprintln!("loco-gen: {e}");
            ExitCode::FAILURE
        }
    }
}

fn run_new(args: NewArgs) -> std::io::Result<()> {
    fs::create_dir_all(&args.out)?;
    let project_root = args.out.join(&args.name);

    if project_root.exists() {
        return Err(std::io::Error::new(
            std::io::ErrorKind::AlreadyExists,
            format!(
                "target project directory already exists: {}",
                project_root.display()
            ),
        ));
    }

    eprintln!(
        "[1/4] Scaffolding Loco project '{}' in {} (db={}) ...",
        args.name,
        args.out.display(),
        args.loco_db.as_str()
    );
    let status = Command::new(&args.loco_bin)
        .arg("new")
        .arg("-n")
        .arg(&args.name)
        .arg("--db")
        .arg(args.loco_db.as_str())
        .arg("--bg")
        .arg("none")
        .arg("--assets")
        .arg("none")
        .current_dir(&args.out)
        .status()
        .map_err(|e| {
            std::io::Error::new(
                std::io::ErrorKind::Other,
                format!(
                    "failed to invoke '{}': {e}. Install loco-cli with `cargo install loco`.",
                    args.loco_bin
                ),
            )
        })?;
    if !status.success() {
        return Err(std::io::Error::new(
            std::io::ErrorKind::Other,
            format!("`{} new` failed with status {status}", args.loco_bin),
        ));
    }

    eprintln!("[2/4] Parsing DDL: {}", args.ddl.display());
    let entities = parse_ddl(&args.ddl, args.db)?;
    eprintln!("       Parsed {} entit{}.", entities.len(), plural(entities.len()));

    eprintln!("[3/4] Writing Loco entities, controllers, migrations into {} ...",
        project_root.display());
    loco::write_loco_project(&entities, &project_root)?;

    if args.wire {
        eprintln!("[3.5/4] Wiring routes into src/app.rs ...");
        wire_app_routes(&project_root, &entities)?;
    }

    if args.migrate {
        eprintln!("[4/4] Running `cargo loco db migrate` ...");
        let status = Command::new("cargo")
            .arg("loco")
            .arg("db")
            .arg("migrate")
            .current_dir(&project_root)
            .status()?;
        if !status.success() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::Other,
                format!("`cargo loco db migrate` failed with status {status}"),
            ));
        }
    } else {
        eprintln!("[4/4] Skipping migrations (pass --migrate to run them).");
    }

    print_summary(&project_root, &entities, args.wire, args.migrate);
    Ok(())
}

fn run_generate(args: GenerateArgs) -> std::io::Result<()> {
    if !args.out.exists() {
        return Err(std::io::Error::new(
            std::io::ErrorKind::NotFound,
            format!("project directory does not exist: {}", args.out.display()),
        ));
    }

    eprintln!("Parsing DDL: {}", args.ddl.display());
    let entities = parse_ddl(&args.ddl, args.db)?;
    eprintln!("Parsed {} entit{}.", entities.len(), plural(entities.len()));

    eprintln!("Writing Loco files into {} ...", args.out.display());
    loco::write_loco_project(&entities, &args.out)?;

    if args.wire {
        eprintln!("Wiring routes into src/app.rs ...");
        wire_app_routes(&args.out, &entities)?;
    }

    print_summary(&args.out, &entities, args.wire, false);
    Ok(())
}

fn parse_ddl(path: &Path, db: Db) -> std::io::Result<Vec<Entity>> {
    use api_rest_generator::config::Configuration;

    let mut props = std::collections::HashMap::new();
    props.insert("target.folder".to_string(), ".".to_string());
    props.insert("target.package".to_string(), "".to_string());
    props.insert(
        "file.name".to_string(),
        path.file_name()
            .and_then(|s| s.to_str())
            .unwrap_or("dump.sql")
            .to_string(),
    );
    props.insert("target.language".to_string(), "rust-loco".to_string());
    props.insert("database.type".to_string(), db.as_globals_str().to_string());
    let cfg = Configuration::from_properties(&props);
    Globals::set(cfg.to_globals());

    let file = fs::File::open(path)?;
    let mut words = get_words(file);
    match db {
        Db::Postgresql => normalize_postgresql_words(&mut words),
        Db::Sqlite => normalize_sqlite_words(&mut words),
        Db::Mssql => normalize_mssql_words(&mut words),
        Db::Mysql => {}
    }
    Ok(parse_words(&words))
}

/// Patch `src/app.rs` to register every generated controller's routes.
/// Idempotent: re-running re-overwrites the same block. Looks for the
/// `AppRoutes::with_default_routes()` call inside `fn routes(...)` and
/// inserts one `.add_route(controllers::<table>::routes())` per entity.
fn wire_app_routes(project_root: &Path, entities: &[Entity]) -> std::io::Result<()> {
    let app_rs = project_root.join("src/app.rs");
    if !app_rs.exists() {
        return Err(std::io::Error::new(
            std::io::ErrorKind::NotFound,
            format!("expected {} to exist (was the project scaffolded with `loco new`?)", app_rs.display()),
        ));
    }

    let original = fs::read_to_string(&app_rs)?;

    // Build the route-registration block.
    let begin = "        // BEGIN loco-gen routes (auto-generated, do not edit)";
    let end = "        // END loco-gen routes";
    let mut block = String::new();
    block.push_str(begin);
    block.push('\n');
    for e in entities {
        let snake = loco::snake_table_for(&e.table_name);
        block.push_str(&format!(
            "            .add_route(crate::controllers::{snake}::routes())\n"
        ));
    }
    block.push_str(end);

    // If a previous block exists, replace it; otherwise inject after
    // `AppRoutes::with_default_routes()`.
    let patched = if let (Some(start), Some(after)) =
        (original.find(begin), original.find(end))
    {
        let mut s = String::with_capacity(original.len() + block.len());
        s.push_str(&original[..start]);
        s.push_str(&block);
        // skip past the existing end-marker line
        let after_end = after + end.len();
        s.push_str(&original[after_end..]);
        s
    } else {
        let needle = "AppRoutes::with_default_routes()";
        let Some(idx) = original.find(needle) else {
            return Err(std::io::Error::new(
                std::io::ErrorKind::InvalidData,
                "could not find `AppRoutes::with_default_routes()` in src/app.rs",
            ));
        };
        // Find end of that line to inject after it.
        let line_end = original[idx..]
            .find('\n')
            .map(|n| idx + n + 1)
            .unwrap_or(original.len());
        let mut s = String::with_capacity(original.len() + block.len() + 1);
        s.push_str(&original[..line_end]);
        s.push_str(&block);
        s.push('\n');
        s.push_str(&original[line_end..]);
        s
    };

    if patched != original {
        fs::write(&app_rs, patched)?;
    }
    Ok(())
}

fn print_summary(project_root: &Path, entities: &[Entity], wired: bool, migrated: bool) {
    eprintln!();
    eprintln!("Done.");
    eprintln!("  Project:    {}", project_root.display());
    eprintln!("  Entities:   {}", entities.len());
    eprintln!("  Routes:     {} (5 per entity)", entities.len() * 5);
    eprintln!("  Wired:      {}", if wired { "yes" } else { "no — see src/app_routes.rs" });
    eprintln!("  Migrated:   {}", if migrated { "yes" } else { "no — run `cargo loco db migrate`" });
    eprintln!();
    eprintln!("Next:");
    eprintln!("  cd {}", project_root.display());
    if !migrated {
        eprintln!("  cargo loco db migrate");
    }
    eprintln!("  cargo loco start");
}

fn plural(n: usize) -> &'static str {
    if n == 1 { "y" } else { "ies" }
}
