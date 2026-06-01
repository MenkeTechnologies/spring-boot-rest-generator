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
        // Strip MSSQL-style [Brackets] which Chinook and other SQLite
        // dumps sometimes use as identifier escape (in addition to "").
        let stripped = w.replace(['[', ']'], "");
        *w = trim_punct(&stripped).to_string();
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
    // Pass 0: collapse multi-word quoted identifiers like `"Order Details"`
    // (Northwind has spaces inside quoted names) into a single token with
    // spaces replaced by underscores: `"Order_Details"`. Without this, the
    // parser sees `Order` as the table name and ignores `Details`, which
    // can collide with another table called `Orders` after pluralisation.
    let mut joined: Vec<String> = Vec::with_capacity(words.len());
    let mut i = 0;
    while i < words.len() {
        let w = &words[i];
        if w.starts_with('"') && !w[1..].contains('"') {
            let mut combined = w.clone();
            let mut j = i + 1;
            while j < words.len() {
                combined.push('_');
                combined.push_str(&words[j]);
                if words[j].contains('"') {
                    j += 1;
                    break;
                }
                j += 1;
            }
            joined.push(combined);
            i = j;
        } else {
            joined.push(w.clone());
            i += 1;
        }
    }
    *words = joined;

    // Pass 1: strip `(max)` from `varchar(max)` / `nvarchar(max)` /
    // `varbinary(max)` BEFORE splitting parens, otherwise the regex
    // can't see the contiguous `(max)` group.
    for w in words.iter_mut() {
        *w = MSSQL_MAX_PAREN.replace_all(w, "").into_owned();
    }

    // Pass 2: split tokens that have `(` or `)` glued to identifiers
    // (e.g. `([CustomerID]` from `([CustomerID] nchar...` in the
    // un-formatted Northwind dump). Put each paren on its own token so
    // the parser's paren-depth tracking sees it and so identifiers don't
    // end up with stray parens after bracket-stripping.
    let mut split: Vec<String> = Vec::with_capacity(words.len() * 2);
    for w in words.iter() {
        let mut buf = String::new();
        for c in w.chars() {
            if c == '(' || c == ')' {
                if !buf.is_empty() {
                    split.push(std::mem::take(&mut buf));
                }
                split.push(c.to_string());
            } else {
                buf.push(c);
            }
        }
        if !buf.is_empty() {
            split.push(buf);
        }
    }
    *words = split;

    // Pass 3: strip MSSQL identifier escapes — bracketed (`[Foo]`) and
    // double-quoted (`"Foo"`) — and trim trailing punctuation.
    for w in words.iter_mut() {
        let stripped = w.replace(['[', ']', '"'], "");
        *w = trim_punct(&stripped).to_string();
    }
    // Drop empties left by stripping.
    words.retain(|w| !w.is_empty());
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
