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

// ------------------------------------- write_loco_project silent overwrite

#[test]
fn write_loco_project_silently_overwrites_on_table_name_collision() {
    // The on-disk writer uses `fs::write` (truncate) per entity, so when
    // two entities resolve to the same `<snake>.rs` file the SECOND
    // entity wins and the first is lost without any error or warning.
    //
    // Concretely:
    //   - First entity:  `User`  with pk `user_id`
    //   - Second entity: `users` with pk `id`
    //
    // Both map to `src/models/_entities/users.rs`. After the call, the
    // file on disk should match `users` (the second entity, with `id`
    // as the PK column) — proving the first entity's content was lost.
    let tmp = tempfile::tempdir().expect("tempdir");
    let root = tmp.path();
    let entities = vec![entity("User", "user_id"), entity("users", "id")];
    let _ = loco::write_loco_project(&entities, root).expect("write_loco_project");

    let entity_file = root.join("src/models/_entities/users.rs");
    let contents = std::fs::read_to_string(&entity_file).unwrap_or_else(|e| {
        panic!("expected {entity_file:?} to exist after write_loco_project: {e}")
    });

    // Second entity (`users`) declares its PK as `id`. The first entity
    // (`User`) declared `user_id`. If the writer were collision-safe,
    // we'd see BOTH PK field names; with the silent-overwrite bug, only
    // the second wins.
    assert!(
        contents.contains("pub id: i32"),
        "expected the second entity's pk field `id` to survive overwrite, missing in:\n{contents}"
    );
    assert!(
        !contents.contains("pub user_id: i32"),
        "first entity's PK field `user_id` should have been overwritten; \
         the writer silently dropped it — instead found:\n{contents}"
    );
}

// ---------------------------------------- merge_mod_rs in-call duplicate bug

#[test]
fn write_loco_project_merge_mod_rs_emits_duplicate_lines_for_in_call_collision() {
    // `merge_mod_rs` checks idempotency against the file's PRE-EXISTING
    // content (`existing.lines()`) — never against the buffer it's
    // currently appending to (`out`). So if two entities-to-write
    // resolve to the same snake name within a single call, the line
    // gets emitted twice. The resulting `mod.rs` will not compile.
    //
    // We trigger the bug with the same `User` / `users` pair used above.
    let tmp = tempfile::tempdir().expect("tempdir");
    let root = tmp.path();
    let entities = vec![entity("User", "user_id"), entity("users", "id")];
    let _ = loco::write_loco_project(&entities, root).expect("write_loco_project");

    // Controllers `mod.rs` is created via `merge_mod_rs`. Read it back
    // and count the `pub mod users;` lines.
    let mod_path = root.join("src/controllers/mod.rs");
    let mod_text = std::fs::read_to_string(&mod_path)
        .unwrap_or_else(|e| panic!("expected {mod_path:?} after write_loco_project: {e}"));
    let count = mod_text.matches("pub mod users;").count();
    assert_eq!(
        count, 2,
        "merge_mod_rs should emit `pub mod users;` twice when two entities collide in one call; \
         got count={count}, full file:\n{mod_text}"
    );
}

// ----------------------------------------- merge_mod_rs duplicate-entity bug

#[test]
fn write_loco_project_merge_mod_rs_emits_duplicate_lines_for_literal_duplicate_entities() {
    // Literal duplicate entities (same `table_name` twice) trigger the
    // same in-call duplicate emission — pin it separately so a fix that
    // adds dedup to ONE path (collision) but not the other (literal dup)
    // is caught.
    let tmp = tempfile::tempdir().expect("tempdir");
    let root = tmp.path();
    let e = entity("users", "id");
    let entities = vec![e.clone(), e];
    let _ = loco::write_loco_project(&entities, root).expect("write_loco_project");

    let mod_path = root.join("src/controllers/mod.rs");
    let mod_text = std::fs::read_to_string(&mod_path)
        .unwrap_or_else(|e| panic!("expected {mod_path:?} after write_loco_project: {e}"));
    let count = mod_text.matches("pub mod users;").count();
    assert_eq!(
        count, 2,
        "merge_mod_rs duplicates `pub mod users;` for literal-duplicate entities; \
         got count={count}, full file:\n{mod_text}"
    );
}
