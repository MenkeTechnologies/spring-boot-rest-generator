//! DDL → `Vec<Entity>` parser — port of the bulk of `utils/Util.kt`.

use std::io::{BufRead, BufReader, Read};

use once_cell::sync::Lazy;
use regex::Regex;

use crate::constants;
use crate::entity::{ColumnToField, Entity};
use crate::globals::Globals;

static ID_SUFFIX: Lazy<Regex> = Lazy::new(|| Regex::new(r"[iI]d$").expect("id-suffix regex"));

/// Strip blank-bordered lines and split the rest on spaces. Drops `#` and
/// `--` line comments (matches the Kotlin tokenizer in `Util.getWords`).
pub fn get_words<R: Read>(reader: R) -> Vec<String> {
    let mut out = Vec::new();
    for line in BufReader::new(reader).lines().map_while(Result::ok) {
        let trimmed = line.trim_start();
        if trimmed.starts_with('#') || trimmed.starts_with("--") {
            continue;
        }
        // Split on any ASCII whitespace (space OR tab) so tabbed indents
        // in dumps like Northwind don't leave `\t` glued to identifiers.
        out.extend(line.split_ascii_whitespace().map(String::from));
    }
    out
}

fn trim_punct(s: &str) -> &str {
    s.trim_end_matches([';', ',', '(', ')'])
}

fn strip_schema_and_escape(name: &str, escape_char: &str) -> String {
    let mut clean = name.replace(escape_char, "");
    if Globals::is_mssql() {
        clean = clean.replace(constants::MSSQL_DB_ESCAPE_CLOSE, "");
    }
    let clean = trim_punct(&clean).to_string();
    if let Some(idx) = clean.rfind('.') {
        clean[idx + 1..].to_string()
    } else {
        clean
    }
}

fn first_letter_to_caps(s: &str) -> String {
    if s.is_empty() {
        return String::new();
    }
    let mut chars = s.chars();
    let first = chars.next().unwrap().to_uppercase().to_string();
    first + chars.as_str()
}

fn camel_name(s: &str) -> String {
    let lower = s.to_lowercase();
    let mut out = String::with_capacity(lower.len());
    let mut upper_next = false;
    for c in lower.chars() {
        if c == '_' {
            upper_next = true;
            continue;
        }
        if upper_next {
            out.extend(c.to_uppercase());
            upper_next = false;
        } else {
            out.push(c);
        }
    }
    out
}

fn entity_name_from_table(table: &str) -> String {
    table
        .split(constants::UNDERSCORE)
        .map(|w| {
            let mut chars = w.chars();
            let first = chars
                .next()
                .map(|c| c.to_uppercase().to_string())
                .unwrap_or_default();
            let rest: String = chars.collect();
            format!("{first}{}", rest.to_lowercase())
        })
        .collect::<Vec<_>>()
        .join("")
}

fn camelize_column(name: &str, escape: &str) -> String {
    let stripped = name.replace(escape, "");
    let comps: Vec<String> = stripped
        .split('_')
        .map(|w| {
            let mut chars = w.chars();
            let first = chars
                .next()
                .map(|c| c.to_uppercase().to_string())
                .unwrap_or_default();
            let rest: String = chars.collect();
            format!("{first}{}", rest.to_lowercase())
        })
        .collect();
    let joined = comps.concat();
    // Lower-case the leading letter (camelCase per Kotlin).
    let mut chars = joined.chars();
    match chars.next() {
        Some(c) => c.to_lowercase().to_string() + chars.as_str(),
        None => String::new(),
    }
}

/// Map a DDL type token to its JVM/Java/Kotlin type name. Mirrors the
/// `Util.getJavaType` cascade exactly so the existing template rendering
/// continues to work unchanged.
fn java_type_for(datatype: &str) -> String {
    let is_kotlin = Globals::is_kotlin();
    let dt = datatype.to_lowercase();
    if constants::VARCHAR_PATTERN.is_match(&dt) {
        return "String".into();
    }
    if constants::MSSQL_NVARCHAR_PATTERN.is_match(&dt) {
        return "String".into();
    }
    if constants::PG_TEXT_PATTERN.is_match(&dt) {
        return "String".into();
    }
    if constants::MSSQL_UNIQUEIDENTIFIER_PATTERN.is_match(&dt) {
        return "String".into();
    }
    if constants::MSSQL_IMAGE_PATTERN.is_match(&dt) {
        return "String".into();
    }
    if constants::PG_BIGINT_PATTERN.is_match(&dt) {
        return "Long".into();
    }
    if constants::BIGINT_PATTERN.is_match(&dt) {
        return "Long".into();
    }
    if constants::INT_PATTERN.is_match(&dt) {
        return if is_kotlin { "Int" } else { "Integer" }.into();
    }
    if constants::PG_INTEGER_PATTERN.is_match(&dt) {
        return if is_kotlin { "Int" } else { "Integer" }.into();
    }
    if constants::MSSQL_DATETIME2_PATTERN.is_match(&dt) {
        return "LocalDateTime".into();
    }
    if constants::DATETIME_PATTERN.is_match(&dt) {
        return "LocalDate".into();
    }
    if constants::PG_DATE_PATTERN.is_match(&dt) {
        return "LocalDate".into();
    }
    if constants::PG_BOOLEAN_PATTERN.is_match(&dt) {
        return if is_kotlin { "Boolean" } else { "String" }.into();
    }
    if constants::BIT_PATTERN.is_match(&dt) {
        return if is_kotlin { "Boolean" } else { "String" }.into();
    }
    if constants::FLOAT_PATTERN.is_match(&dt) {
        return "Float".into();
    }
    if constants::PG_REAL_PATTERN.is_match(&dt) {
        return "Float".into();
    }
    if constants::DOUBLE_PATTERN.is_match(&dt) {
        return "Double".into();
    }
    if constants::PG_NUMERIC_PATTERN.is_match(&dt) {
        return "Double".into();
    }
    if constants::MSSQL_DECIMAL_PATTERN.is_match(&dt) {
        return "Double".into();
    }
    if constants::MSSQL_MONEY_PATTERN.is_match(&dt) {
        return "Double".into();
    }
    if constants::TIME_PATTERN.is_match(&dt) {
        return "LocalTime".into();
    }
    if constants::TIMESTAMP_PATTERN.is_match(&dt) {
        return "LocalDateTime".into();
    }
    "String".into()
}

fn build_id_from_match_text(key: &str, escape: &str) -> ColumnToField {
    let mut camel: Option<String> = None;
    let mut db_name: Option<String> = None;
    let java_type: String;
    let id_type: String;

    if let Some(caps) = constants::FOREIGN_KEY_REFERENCES_PATTERN.captures(key) {
        let foreign_key = caps.get(1).map(|m| m.as_str()).unwrap_or_default();
        let other_table = caps.get(2).map(|m| m.as_str()).unwrap_or_default();
        let primary_other = caps.get(3).map(|m| m.as_str()).unwrap_or_default();
        let clean_table = strip_schema_and_escape(other_table, escape);
        let _ = first_letter_to_caps(&camel_name(&clean_table)); // matches Kotlin side-effect parity
        camel = Some(camel_name(&foreign_key.replace(escape, "")));
        db_name = Some(primary_other.replace(escape, ""));
    }
    if let Some(caps) = constants::PRIMARY_KEY_PATTERN.captures(key) {
        id_type = constants::PK_ID.to_string();
        db_name = caps
            .get(1)
            .map(|m| m.as_str().replace(escape, "").trim().to_string());
        java_type = constants::PK_DATA_TYPE.to_string();
    } else {
        id_type = constants::FK_ID.to_string();
        java_type = if Globals::is_kotlin() {
            "Int".to_string()
        } else {
            constants::FK_DATA_TYPE.to_string()
        };
    }

    ColumnToField {
        database_id_type: Some(id_type),
        database_column_name: db_name,
        camel_case_field_name: camel,
        database_type: None,
        java_type: Some(java_type),
    }
}

fn find_alter_table_entity<'e>(
    entities: &'e [Entity],
    words: &[String],
    current: usize,
    escape: &str,
) -> Option<&'e Entity> {
    let start = current.saturating_sub(10);
    for j in (start..current).rev() {
        if words[j].eq_ignore_ascii_case("alter")
            && j + 2 < words.len()
            && words[j + 1].eq_ignore_ascii_case("table")
        {
            let table_idx = if j + 2 < words.len() && words[j + 2].eq_ignore_ascii_case("only") {
                j + 3
            } else {
                j + 2
            };
            if table_idx < words.len() {
                let table_name = strip_schema_and_escape(&words[table_idx], escape);
                return entities
                    .iter()
                    .find(|e| e.table_name.eq_ignore_ascii_case(&table_name));
            }
        }
    }
    None
}

fn set_pk_or_fk_columns(entities: &mut [Entity], words: &[String], i: usize, escape: &str) {
    let word = &words[i];
    if !constants::PRIMARY_FOREIGN_PATTERN.is_match(&word.to_uppercase()) {
        return;
    }
    let end = (i + 10).min(words.len());
    let key_string = words[i..end].join(" ");
    let key_column = build_id_from_match_text(&key_string, escape);

    let target_index = if Globals::is_postgresql() {
        find_alter_table_entity(entities, words, i, escape)
            .and_then(|target| entities.iter().position(|e| std::ptr::eq(e, target)))
            .unwrap_or_else(|| entities.len() - 1)
    } else {
        entities.len() - 1
    };

    let columns = &mut entities[target_index].columns;
    for col in columns.iter_mut() {
        if col.database_id_type.is_none()
            && col.database_column_name == key_column.database_column_name
        {
            let mut new_col = key_column.clone();
            new_col.camel_case_field_name = col
                .camel_case_field_name
                .as_deref()
                .map(|c| ID_SUFFIX.replace(c, "").to_string());
            *col = new_col;
        }
    }
}

/// Parse a tokenised DDL into a `Vec<Entity>`.
pub fn parse_words(words: &[String]) -> Vec<Entity> {
    let escape = Globals::escape_character();
    let mut entities: Vec<Entity> = Vec::new();
    let mut in_table = false;
    let mut paren_depth: i32 = 0;

    let mut i = 0;
    while i < words.len() {
        let word = &words[i];

        if word.eq_ignore_ascii_case("create") && i + 2 < words.len() {
            let next_word = &words[i + 1];
            if next_word.eq_ignore_ascii_case("table") {
                let table_word = &words[i + 2];
                let clean_name = strip_schema_and_escape(table_word, escape);
                let entity_name = entity_name_from_table(&clean_name);
                entities.push(Entity {
                    table_name: clean_name,
                    entity_name,
                    columns: Vec::new(),
                });
                in_table = true;
                paren_depth = 0;
            } else if matches!(
                next_word.to_ascii_uppercase().as_str(),
                "VIEW" | "FUNCTION" | "TRIGGER" | "PROCEDURE" | "INDEX" | "EVENT" | "SCHEMA" | "DATABASE"
            ) {
                in_table = false;
            }
        }

        // Track paren depth once we're past the `(` after the table name so
        // we can detect the closing `)` that ends the CREATE TABLE body.
        // This stops column-pattern matching from bleeding into subsequent
        // CREATE VIEW / FUNCTION / TRIGGER definitions (e.g. Sakila has
        // stored functions and triggers between table definitions).
        if in_table {
            for c in word.chars() {
                if c == '(' {
                    paren_depth += 1;
                } else if c == ')' {
                    paren_depth -= 1;
                    if paren_depth <= 0 {
                        in_table = false;
                        break;
                    }
                }
            }
        }

        if in_table
            && !entities.is_empty()
            && constants::SUPPORTED_DATA_TYPES_PATTERN.is_match(word)
        {
            if i == 0 {
                i += 1;
                continue;
            }
            let column_name = words[i - 1].replace(escape, "");
            let java_type = java_type_for(word);
            let camel = camelize_column(&column_name, escape);

            entities.last_mut().unwrap().columns.push(ColumnToField {
                database_id_type: None,
                database_column_name: Some(column_name.clone()),
                camel_case_field_name: Some(camel.clone()),
                database_type: Some(word.clone()),
                java_type: Some(java_type),
            });

            if Globals::is_sqlite() || Globals::is_mssql() {
                let max_look = (i + 5).min(words.len().saturating_sub(1));
                let mut k = i + 1;
                while k <= max_look {
                    let w = &words[k];
                    if w.eq_ignore_ascii_case("CONSTRAINT")
                        || w.eq_ignore_ascii_case("FOREIGN")
                        || w == ")"
                        || w == "("
                    {
                        break;
                    }
                    if w.eq_ignore_ascii_case("PRIMARY")
                        && k + 1 < words.len()
                        && words[k + 1].eq_ignore_ascii_case("KEY")
                    {
                        let entity = entities.last_mut().unwrap();
                        let last_idx = entity.columns.len() - 1;
                        let pk_camel = ID_SUFFIX.replace(&camel, "").to_string();
                        entity.columns[last_idx] = ColumnToField {
                            database_id_type: Some(constants::PK_ID.to_string()),
                            database_column_name: Some(column_name.clone()),
                            camel_case_field_name: Some(pk_camel),
                            database_type: None,
                            java_type: Some(constants::PK_DATA_TYPE.to_string()),
                        };
                        break;
                    }
                    k += 1;
                }
            }
        }

        if in_table && !entities.is_empty() {
            set_pk_or_fk_columns(&mut entities, words, i, escape);
        }

        i += 1;
    }

    entities
}

#[cfg(test)]
mod tests {
    //! Private-helper pins. These functions are not part of the public API
    //! but they shape every rendered entity field name and PascalCase class
    //! name, so a regression here would corrupt generated source silently.
    //! All five are pure (no `Globals` dependency) so they can be exercised
    //! without the `GLOBALS_LOCK` synchronisation used in `parity_smoke.rs`.

    use super::*;

    // ------------------------------------------------------------ trim_punct

    #[test]
    fn trim_punct_strips_trailing_semicolon() {
        assert_eq!(trim_punct("foo;"), "foo");
    }

    #[test]
    fn trim_punct_strips_trailing_comma() {
        assert_eq!(trim_punct("foo,"), "foo");
    }

    #[test]
    fn trim_punct_strips_trailing_open_paren() {
        assert_eq!(trim_punct("foo("), "foo");
    }

    #[test]
    fn trim_punct_strips_trailing_close_paren() {
        assert_eq!(trim_punct("foo)"), "foo");
    }

    #[test]
    fn trim_punct_strips_compound_trailing_punctuation() {
        // `trim_end_matches` consumes EVERY trailing char from the set,
        // so `foo;)` strips both — pin that behaviour explicitly so a
        // future refactor doesn't accidentally switch to single-char
        // trim and silently leak punctuation into entity names.
        assert_eq!(trim_punct("foo;)"), "foo");
    }

    #[test]
    fn trim_punct_leaves_internal_punctuation_alone() {
        assert_eq!(trim_punct("foo(bar)"), "foo(bar");
    }

    #[test]
    fn trim_punct_returns_empty_for_all_punct_input() {
        assert_eq!(trim_punct(";;,,()"), "");
    }

    // -------------------------------------------------- first_letter_to_caps

    #[test]
    fn first_letter_to_caps_basic_uppercase() {
        assert_eq!(first_letter_to_caps("hello"), "Hello");
    }

    #[test]
    fn first_letter_to_caps_already_uppercase_is_idempotent() {
        assert_eq!(first_letter_to_caps("Hello"), "Hello");
    }

    #[test]
    fn first_letter_to_caps_empty_string_returns_empty() {
        assert_eq!(first_letter_to_caps(""), "");
    }

    #[test]
    fn first_letter_to_caps_preserves_rest_unchanged() {
        // Rest of the string is NOT touched — only the first char is folded.
        assert_eq!(first_letter_to_caps("hELLO"), "HELLO");
    }

    #[test]
    fn first_letter_to_caps_single_character_input() {
        assert_eq!(first_letter_to_caps("h"), "H");
    }

    #[test]
    fn first_letter_to_caps_leading_digit_is_passed_through() {
        // Digits have no uppercase form; the function must not panic.
        assert_eq!(first_letter_to_caps("1abc"), "1abc");
    }

    // --------------------------------------------------------- camel_name

    #[test]
    fn camel_name_snake_to_camel_basic() {
        assert_eq!(camel_name("foo_bar"), "fooBar");
    }

    #[test]
    fn camel_name_single_word_lowercased() {
        assert_eq!(camel_name("Foo"), "foo");
    }

    #[test]
    fn camel_name_multi_underscore_segments() {
        assert_eq!(camel_name("user_account_id"), "userAccountId");
    }

    #[test]
    fn camel_name_handles_leading_underscore() {
        // A leading underscore promotes the FIRST character to upper.
        assert_eq!(camel_name("_foo"), "Foo");
    }

    #[test]
    fn camel_name_trailing_underscore_is_noop() {
        assert_eq!(camel_name("foo_"), "foo");
    }

    #[test]
    fn camel_name_double_underscore_treated_as_two_separators() {
        // Two adjacent underscores produce an upper-cased empty segment
        // followed by an upper-cased next char — same as Kotlin's pattern.
        assert_eq!(camel_name("foo__bar"), "fooBar");
    }

    #[test]
    fn camel_name_empty_string_returns_empty() {
        assert_eq!(camel_name(""), "");
    }

    #[test]
    fn camel_name_already_lowercase_no_underscores_unchanged() {
        assert_eq!(camel_name("foo"), "foo");
    }

    // ---------------------------------------------- entity_name_from_table

    #[test]
    fn entity_name_from_table_snake_to_pascal() {
        assert_eq!(entity_name_from_table("user_account"), "UserAccount");
    }

    #[test]
    fn entity_name_from_table_single_word_capitalised() {
        assert_eq!(entity_name_from_table("user"), "User");
    }

    #[test]
    fn entity_name_from_table_three_segments() {
        assert_eq!(
            entity_name_from_table("foo_bar_baz"),
            "FooBarBaz"
        );
    }

    #[test]
    fn entity_name_from_table_uppercase_in_source_is_lowered_on_rest() {
        // First letter of each segment upper-cased; rest lower-cased.
        assert_eq!(entity_name_from_table("USER_ACCOUNT"), "UserAccount");
    }

    #[test]
    fn entity_name_from_table_empty_returns_empty() {
        assert_eq!(entity_name_from_table(""), "");
    }

    #[test]
    fn entity_name_from_table_mixed_case_segments() {
        assert_eq!(entity_name_from_table("fOo_BaR"), "FooBar");
    }

    // ------------------------------------------------------ camelize_column

    #[test]
    fn camelize_column_basic_snake_to_camel() {
        assert_eq!(camelize_column("foo_bar", "`"), "fooBar");
    }

    #[test]
    fn camelize_column_strips_escape_chars() {
        assert_eq!(camelize_column("`foo_bar`", "`"), "fooBar");
    }

    #[test]
    fn camelize_column_uses_provided_escape_string() {
        // MSSQL-style escape is `[` — pass it explicitly.
        assert_eq!(camelize_column("[foo_bar", "["), "fooBar");
    }

    #[test]
    fn camelize_column_single_word_lowercased() {
        assert_eq!(camelize_column("name", "`"), "name");
    }

    #[test]
    fn camelize_column_uppercase_input_lowered_correctly() {
        assert_eq!(camelize_column("USER_ID", "`"), "userId");
    }

    #[test]
    fn camelize_column_empty_input_returns_empty() {
        assert_eq!(camelize_column("", "`"), "");
    }

    #[test]
    fn camelize_column_only_escape_chars_returns_empty() {
        assert_eq!(camelize_column("```", "`"), "");
    }

    #[test]
    fn camelize_column_multi_segment_pascal_then_lowered_first() {
        // Internal pipeline produces PascalCase then lowers the first
        // char — pin that the LAST segment retains its capital.
        assert_eq!(camelize_column("foo_bar_baz", "`"), "fooBarBaz");
    }
}
