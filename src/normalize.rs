//! Per-DB word-stream normalisation — port of `Util.normalize{Postgresql,Sqlite,Mssql}Words`.
//!
//! Each function mutates the tokenised word list in place to fold the
//! dialect's multi-word types and strip its noise so the main parser
//! sees a stream that looks like vanilla MySQL DDL.

use once_cell::sync::Lazy;
use regex::Regex;

fn trim_punct(s: &str) -> &str {
    s.trim_end_matches([';', ','])
}

/// PostgreSQL: collapse `character varying(n)` → `varchar(n)`,
/// `timestamp [without|with] time zone` → `timestamp`,
/// `double precision` → `double`.
pub fn normalize_postgresql_words(words: &mut Vec<String>) {
    for w in words.iter_mut() {
        *w = trim_punct(w).to_string();
    }
    let mut i = 0;
    while i < words.len() {
        let word_lower = words[i].to_lowercase();
        if word_lower == "character"
            && i + 1 < words.len()
            && words[i + 1].to_lowercase().starts_with("varying")
        {
            let varying = words[i + 1].clone();
            // varying(n) → grab the (n) tail
            let size = if let Some(idx) = varying.to_lowercase().find("varying") {
                &varying[idx + "varying".len()..]
            } else {
                ""
            };
            words[i] = format!("varchar{size}");
            words.remove(i + 1);
        } else if (word_lower == "timestamp" || word_lower == "time")
            && i + 3 < words.len()
            && (words[i + 1].eq_ignore_ascii_case("without")
                || words[i + 1].eq_ignore_ascii_case("with"))
            && words[i + 2].eq_ignore_ascii_case("time")
            && words[i + 3].to_lowercase().starts_with("zone")
        {
            words.remove(i + 3);
            words.remove(i + 2);
            words.remove(i + 1);
        } else if word_lower == "double"
            && i + 1 < words.len()
            && words[i + 1].to_lowercase().starts_with("precision")
        {
            words.remove(i + 1);
        }
        i += 1;
    }
}

/// SQLite: strip `IF NOT EXISTS` and drop entire `PRAGMA` / `INSERT` /
/// `BEGIN` / `COMMIT` / `DELETE` runs up to the next `CREATE` / `ALTER`.
pub fn normalize_sqlite_words(words: &mut Vec<String>) {
    for w in words.iter_mut() {
        *w = trim_punct(w).to_string();
    }
    let mut i = 0;
    while i < words.len() {
        let upper = words[i].to_uppercase();
        if upper == "IF"
            && i + 2 < words.len()
            && words[i + 1].eq_ignore_ascii_case("NOT")
            && words[i + 2].eq_ignore_ascii_case("EXISTS")
        {
            words.remove(i + 2);
            words.remove(i + 1);
            words.remove(i);
        } else if matches!(
            upper.as_str(),
            "PRAGMA" | "INSERT" | "BEGIN" | "COMMIT" | "DELETE"
        ) {
            while i < words.len() {
                let next = words[i]
                    .to_uppercase()
                    .trim_end_matches([';', ',', '(', ')'])
                    .to_string();
                if next == "CREATE" || next == "ALTER" {
                    break;
                }
                words.remove(i);
            }
        } else {
            i += 1;
        }
    }
}

static MSSQL_MAX_PAREN: Lazy<Regex> = Lazy::new(|| Regex::new(r"\((?i:max)\)").expect("re"));

/// MSSQL: strip `[...]` identifier brackets, fold `(max)` out of types,
/// drop `SET`/`USE`/`GO`/`EXEC`/`PRINT`/`IF`/`DROP`/`INSERT`/`BEGIN`/`END`
/// runs, and strip noise keywords (`CLUSTERED`, `NONCLUSTERED`, `ASC`,
/// `DESC`, `IDENTITY(...)`).
pub fn normalize_mssql_words(words: &mut Vec<String>) {
    for w in words.iter_mut() {
        let stripped = w.replace(['[', ']'], "");
        let trimmed = trim_punct(&stripped).to_string();
        *w = MSSQL_MAX_PAREN.replace_all(&trimmed, "").into_owned();
    }
    let mut i = 0;
    while i < words.len() {
        let upper = words[i].to_uppercase();
        if matches!(
            upper.as_str(),
            "SET" | "USE" | "GO" | "EXEC" | "PRINT" | "IF" | "DROP" | "INSERT" | "BEGIN" | "END"
        ) {
            while i < words.len() {
                let next = words[i]
                    .to_uppercase()
                    .trim_end_matches([';', ',', '(', ')'])
                    .to_string();
                if next == "CREATE" || next == "ALTER" {
                    break;
                }
                words.remove(i);
            }
        } else {
            let stripped = upper.trim_end_matches(['(', ')']).to_string();
            if matches!(
                stripped.as_str(),
                "CLUSTERED" | "NONCLUSTERED" | "ASC" | "DESC"
            ) {
                let trailing: String = words[i].chars().filter(|&c| c == '(' || c == ')').collect();
                if !trailing.is_empty() {
                    words[i] = trailing;
                    i += 1;
                } else {
                    words.remove(i);
                }
            } else if upper.starts_with("IDENTITY") {
                words.remove(i);
            } else {
                i += 1;
            }
        }
    }
}
