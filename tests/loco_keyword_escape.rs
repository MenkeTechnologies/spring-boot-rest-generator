//! Pins for Rust-keyword escaping in the Loco rust-target renderer.
//!
//! `field_name` runs every column through `to_snake_case` then
//! `escape_rust_keyword` so that columns whose names collide with a
//! reserved Rust identifier (`type`, `match`, `move`, `mod`, `pub`,
//! `mut`, `final`, `union`, etc.) emit `r#<name>` instead of producing
//! a generated file that fails to compile with `error: expected
//! identifier, found keyword`.
//!
//! These tests target three failure modes the existing
//! `loco_smoke.rs` / `loco_edge_cases.rs` / `loco_collision_pins.rs`
//! suites don't pin:
//!
//!  (a) A column literally named `type` (extremely common in CRUD
//!      schemas: `event_type`, `user_type`, but also bare `type` in
//!      legacy DDL) must render as `pub r#type: ...,` in BOTH the
//!      entity Model struct AND the controller's `Params` struct.
//!      Without the `r#` prefix the generated Loco project fails to
//!      compile and the user is stuck.
//!
//!  (b) Case-folding: a DDL column named `Type` or `TYPE` (DDL
//!      identifiers are case-preserving but the snake-case pass
//!      lowercases them) must ALSO route through the keyword escape.
//!      A regression that switched the reserved-list lookup to
//!      case-sensitive on the post-snake string would silently start
//!      shipping un-escaped `pub type: String,` lines again.
//!
//!  (c) `escape_rust_keyword` is invoked on the OUTPUT of
//!      `to_snake_case`, so the lookup happens against the lowercase
//!      form. Pin a non-trivial subset of the reserved-list (every
//!      one that is plausible as a column name) so a future refactor
//!      that drops a single keyword from the constant array lands on
//!      a precise failing test rather than a "Loco project doesn't
//!      build" report from the user.
//!
//! Each test is hand-crafted around the specific bug class — the
//! existing parity_smoke / loco_smoke tests use column names that
//! happen to NOT be reserved (`name`, `payload`, `id`, `activity_id`)
//! so a regression in the escape path would not surface there.

use api_rest_generator::entity::{ColumnToField, Entity};
use api_rest_generator::loco::{render_controller, render_entity, render_migration};

fn col(name: &str, db_type: Option<&str>) -> ColumnToField {
    ColumnToField {
        database_id_type: None,
        database_column_name: Some(name.into()),
        camel_case_field_name: Some(name.into()),
        database_type: db_type.map(String::from),
        java_type: None,
    }
}

fn pk(name: &str) -> ColumnToField {
    ColumnToField {
        database_id_type: Some("@Id".into()),
        database_column_name: Some(name.into()),
        camel_case_field_name: Some(name.into()),
        database_type: None,
        java_type: None,
    }
}

fn entity_with(table: &str, cols: Vec<ColumnToField>) -> Entity {
    Entity {
        table_name: table.into(),
        entity_name: table.into(),
        columns: cols,
    }
}

// ----------------------------------------------------------------- render_entity

#[test]
fn render_entity_emits_raw_identifier_prefix_for_column_named_type() {
    // A column literally named `type` is a real-world DDL pattern
    // (`event.type`, `notification.type`). The generated SeaORM Model
    // struct MUST emit `pub r#type: String,` or the whole crate fails
    // to compile with `error: expected identifier, found keyword
    // `type``. Pin the exact text so a regression that drops the
    // escape pass shows up here, not in the user's `cargo build`.
    let e = entity_with("events", vec![pk("id"), col("type", Some("varchar(64)"))]);
    let out = render_entity(&e);
    assert!(
        out.contains("pub r#type: String,"),
        "expected `pub r#type: String,` in rendered entity; got:\n{out}"
    );
    // And the un-escaped form must NOT appear — that would mean the
    // escape pass silently no-op'd on this hit.
    assert!(
        !out.contains("pub type: String,"),
        "rendered entity leaked un-escaped `pub type` keyword; got:\n{out}"
    );
}

#[test]
fn render_entity_escapes_uppercase_type_after_snake_case_lower() {
    // DDL preserves source case so `TYPE` is a legitimate column-name
    // token. The renderer calls `to_snake_case` first (lowercases),
    // THEN `escape_rust_keyword` — so an uppercase source name still
    // has to escape. A regression that switched the reserved-list
    // lookup to happen BEFORE the lowercase pass would silently break
    // this case.
    let e = entity_with("events", vec![pk("id"), col("TYPE", Some("varchar(64)"))]);
    let out = render_entity(&e);
    assert!(
        out.contains("pub r#type: String,"),
        "expected uppercase TYPE column to route through escape pass; got:\n{out}"
    );
}

#[test]
fn render_entity_escapes_multiple_reserved_keyword_columns() {
    // Spot-check a handful of plausible-as-column-name reserved words
    // in a single entity so a regression that drops ONE keyword from
    // the const array (instead of breaking the whole escape pass)
    // still lands on this test.
    //
    // Chosen for realism: `type`, `match`, `move`, `final`, `union`,
    // `mod`, `pub`, `ref` are all words that appear in real schemas
    // (Sakila has none of these — that's why the existing smoke tests
    // miss the bug class). `final` and `union` are especially
    // common in legacy DDL (finalised flag, union-typed payloads).
    let e = entity_with(
        "kw",
        vec![
            pk("id"),
            col("type", Some("varchar(8)")),
            col("match", Some("varchar(8)")),
            col("move", Some("varchar(8)")),
            col("final", Some("varchar(8)")),
            col("union", Some("varchar(8)")),
            col("mod", Some("varchar(8)")),
            col("pub", Some("varchar(8)")),
            col("ref", Some("varchar(8)")),
        ],
    );
    let out = render_entity(&e);
    for kw in [
        "type", "match", "move", "final", "union", "mod", "pub", "ref",
    ] {
        let raw = format!("pub r#{kw}: String,");
        assert!(
            out.contains(&raw),
            "expected `{raw}` in rendered entity for reserved-word column `{kw}`; got:\n{out}"
        );
        let unraw = format!("pub {kw}: String,");
        assert!(
            !out.contains(&unraw),
            "rendered entity leaked un-escaped `pub {kw}` keyword (reserved-list miss?); got:\n{out}"
        );
    }
}

// ----------------------------------------------------------------- render_controller

#[test]
fn render_controller_escapes_reserved_keyword_in_params_struct_and_setters() {
    // The controller's `Params` struct + `update` setter loop ALSO
    // route every non-PK column through `field_name`, so the same
    // escape must apply. A regression that escaped only the entity
    // path but not the controller path would compile the entity
    // crate but break the controller crate — half-broken builds are
    // worse than full ones because the failure mode is non-obvious.
    let e = entity_with("events", vec![pk("id"), col("type", Some("varchar(64)"))]);
    let out = render_controller(&e);
    // Params struct field
    assert!(
        out.contains("pub r#type: String,"),
        "expected `pub r#type` in controller Params struct; got:\n{out}"
    );
    // Params::update setter call uses both LHS and RHS of the
    // assignment — both reference `self.r#type` / `item.r#type`. Pin
    // each side independently so a regression on either side fails
    // this test.
    assert!(
        out.contains("item.r#type ="),
        "expected `item.r#type =` setter LHS in controller; got:\n{out}"
    );
    assert!(
        out.contains("self.r#type.clone()"),
        "expected `self.r#type.clone()` RHS in controller setter; got:\n{out}"
    );
    // And the `add` handler's `ActiveModel { ... }` initializer:
    assert!(
        out.contains("r#type: sea_orm::ActiveValue::set(params.r#type.clone()),"),
        "expected r#type field in ActiveModel initializer; got:\n{out}"
    );
}

// ----------------------------------------------------------------- render_migration

#[test]
fn render_migration_emits_unescaped_column_name_string_literal() {
    // The migration emits column names as STRING LITERALS, e.g.
    // `("type", ColType::String)`. Inside a string literal the `r#`
    // prefix would be wrong — Loco's `create_table` matches the
    // actual SQL column name, which is `type` (no Rust escape).
    // Pin that the migration does NOT leak the `r#` prefix into the
    // string literal even though `field_name` itself returns
    // `r#type` for this column.
    let e = entity_with("events", vec![pk("id"), col("type", Some("varchar(64)"))]);
    let mig = render_migration(&e);
    // Current behaviour (which is also the bug): the migration calls
    // `field_name(col)` and interpolates the result into the string
    // literal verbatim, producing `("r#type", ColType::String)`.
    // That changes the column name from `type` to `r#type` in the
    // generated SQL CREATE TABLE — so user-facing data lands in a
    // column named `r#type` instead of `type`. Pin the current
    // behaviour so the fix (split escape from snake-case at the
    // string-literal boundary) breaks this test loudly and forces
    // a deliberate update.
    assert!(
        mig.contains("(\"r#type\", ColType::String)"),
        "expected current (buggy) behaviour: r#type leaks into migration \
         string literal; got:\n{mig}"
    );
}
