//! Adversarial pins for table-name COLLISIONS in the Loco target.
//!
//! `snake_table_for` lowercases + naively pluralises by appending `s`.
//! That makes several distinct source-DDL table names collapse onto the
//! same module name, which the downstream renderers and the on-disk
//! writer do NOT detect. Two real-world collisions:
//!
//! 1. `"User"` vs `"users"`           → both → `"users"`
//! 2. `"category"` vs `"categories"`  → both → `"categories"` and `"categorys"`
//!    (the latter case collides only on the FIRST pair, but the
//!    `User`/`users` pair is the one DDL authors regularly hit).
//!
//! These tests target two failure modes the existing
//! `loco_edge_cases.rs` doesn't cover:
//!
//!  (a) `render_models_mod` / `render_controllers_mod` emit duplicate
//!      `pub mod foo;` lines when the collision is present — the
//!      resulting `mod.rs` will not compile (Rust E0428).
//!
//!  (b) `write_loco_project` writes each entity's `<snake>.rs` files
//!      in a `for` loop using `fs::write` (truncating). The second
//!      entity in the collision pair silently overwrites the first
//!      entity's `_entities/<snake>.rs`, `models/<snake>.rs`, and
//!      `controllers/<snake>.rs` — a data-loss bug that produces a
//!      project that compiles but is missing one of the user's tables.
//!
//!  (c) `merge_mod_rs` (private; reached via `write_loco_project`)
//!      checks idempotency against the file's pre-existing content,
//!      not the in-flight buffer. When the same module name needs to
//!      be appended twice in a single call (the collision case above
//!      OR a literal duplicate entity), the function emits the same
//!      `pub mod foo;` line TWICE — same E0428 outcome but reached
//!      via a different bug.
//!
//! Each test is a single-failure-mode pin: a regression in any one of
//! the three failure paths lands on a single failing test rather than
//! a diffuse multi-file shape break in `loco_smoke.rs`.

use api_rest_generator::entity::{ColumnToField, Entity};
use api_rest_generator::loco;

fn col(name: &str, db_type: Option<&str>, id_type: Option<&str>) -> ColumnToField {
    ColumnToField {
        database_id_type: id_type.map(String::from),
        database_column_name: Some(name.into()),
        camel_case_field_name: Some(name.into()),
        database_type: db_type.map(String::from),
        java_type: None,
    }
}

fn entity(table: &str, pk_col: &str) -> Entity {
    Entity {
        table_name: table.into(),
        entity_name: table.into(),
        columns: vec![
            col(pk_col, None, Some("@Id")),
            col("payload", Some("varchar(64)"), None),
        ],
    }
}

// -------------------------------------------------------- collision detection

#[test]
fn snake_table_for_collides_on_user_and_users_inputs() {
    // Pin the underlying collision FIRST so the downstream tests have
    // a documented invariant they're built on: distinct source table
    // names that map to the same snake module. If `snake_table_for`
    // ever gains collision-avoiding logic (e.g. preserve case-distinct
    // suffix), this test will be the first to flag the behaviour shift.
    assert_eq!(
        loco::snake_table_for("User"),
        loco::snake_table_for("users")
    );
    // Spell out the resolved name so the assert error message names it
    // when the equality flips.
    assert_eq!(loco::snake_table_for("User"), "users");
    assert_eq!(loco::snake_table_for("users"), "users");
}

// ----------------------------------------- render_models_mod duplicate emission

#[test]
fn render_models_mod_emits_duplicate_mod_decls_for_colliding_table_names() {
    // Two source entities (`User` and `users`) snake-resolve to the
    // same module. The aggregator iterates entities blindly so the
    // generated `pub mod users;` appears TWICE — the resulting file
    // would not compile (Rust E0428 "the name `users` is defined
    // multiple times"). This pin documents that the renderer has no
    // dedup pass.
    let entities = vec![entity("User", "user_id"), entity("users", "id")];
    let out = loco::render_models_mod(&entities);
    let count = out.matches("pub mod users;").count();
    assert_eq!(
        count, 2,
        "expected duplicate `pub mod users;` lines from collision; got count={count}, full output:\n{out}"
    );
}

#[test]
fn render_controllers_mod_emits_duplicate_mod_decls_for_colliding_table_names() {
    // Symmetric pin for the controllers aggregator.
    let entities = vec![entity("User", "user_id"), entity("users", "id")];
    let out = loco::render_controllers_mod(&entities);
    let count = out.matches("pub mod users;").count();
    assert_eq!(
        count, 2,
        "expected duplicate `pub mod users;` lines in controllers/mod.rs; got count={count}, full output:\n{out}"
    );
}

#[test]
fn render_entities_mod_emits_duplicate_mod_decls_for_colliding_table_names() {
    // And for `_entities/mod.rs` — same blind iteration over entities.
    let entities = vec![entity("User", "user_id"), entity("users", "id")];
    let out = loco::render_entities_mod(&entities);
    let count = out.matches("pub mod users;").count();
    assert_eq!(
        count, 2,
        "expected duplicate `pub mod users;` lines in _entities/mod.rs; got count={count}, full output:\n{out}"
    );
}

// --- write_loco_project rejects snake-collision (was: silent overwrite) ---

#[test]
fn write_loco_project_rejects_snake_collision_with_named_error() {
    // FIXED: pre-fix the writer used `fs::write` (truncate) per entity, so
    // two entities resolving to the same `<snake>.rs` file silently
    // overwrote each other and the merged `mod.rs` got a duplicate
    // `pub mod users;` (Rust E0428). Now `write_loco_project` calls
    // `check_no_snake_collision` upfront and returns an `io::Error` naming
    // BOTH offending input table names so the caller can rename.
    let tmp = tempfile::tempdir().expect("tempdir");
    let root = tmp.path();
    let entities = vec![entity("User", "user_id"), entity("users", "id")];
    let err = loco::write_loco_project(&entities, root)
        .expect_err("write_loco_project must reject snake-colliding entities");
    let msg = err.to_string();
    assert!(
        msg.contains("snake_table_for collision"),
        "error must name the collision class; got: {msg}"
    );
    assert!(
        msg.contains("User") && msg.contains("users"),
        "error must name BOTH colliding inputs so the caller knows which to rename; got: {msg}"
    );
    // And no file from the second entity should have been written —
    // the failure happens BEFORE any fs::write call.
    assert!(
        !root.join("src/models/_entities/users.rs").exists(),
        "no entity file should be written on collision-reject path"
    );
}

#[test]
fn write_loco_project_rejects_literal_duplicate_entities() {
    // Literal duplicate entities (same `table_name` twice) trigger the same
    // collision class; verify they're rejected by the same code path so a
    // future fix that dedups only the snake-normalized case but not literal
    // duplicates is caught.
    let tmp = tempfile::tempdir().expect("tempdir");
    let root = tmp.path();
    let e = entity("users", "id");
    let entities = vec![e.clone(), e];
    let err = loco::write_loco_project(&entities, root)
        .expect_err("write_loco_project must reject literal-duplicate entities");
    let msg = err.to_string();
    assert!(
        msg.contains("snake_table_for collision"),
        "error must name the collision class; got: {msg}"
    );
}

#[test]
fn write_loco_project_accepts_collision_free_entities() {
    // Sanity: the collision check must NOT false-positive on normal input.
    let tmp = tempfile::tempdir().expect("tempdir");
    let root = tmp.path();
    let entities = vec![entity("users", "id"), entity("orders", "id")];
    loco::write_loco_project(&entities, root).expect("collision-free input must succeed");
    assert!(root.join("src/models/_entities/users.rs").exists());
    assert!(root.join("src/models/_entities/orders.rs").exists());
}
