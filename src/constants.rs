//! Magic strings + pre-compiled regexes — port of `utils/EntityToRESTConstants.kt`.
//!
//! Java `Pattern.matches()` requires the regex to match the *entire* input,
//! while `Pattern.matcher(s).matches()` does too. Rust's `regex::Regex`
//! defaults to substring `find`; to keep Kotlin parity each pattern that
//! was used via `.matches(word)` is anchored here with `^…$`.
//!
//! Tokens like `\b(?i:varchar)[(\d)]*` in the Kotlin source compile fine
//! under Java's regex engine; here they are emitted as case-insensitive
//! groups (`(?i)…`) so the same word boundary intent is preserved.

use once_cell::sync::Lazy;
use regex::Regex;

pub const DB_ESCAPE_CHARACTER: &str = "`";
pub const PG_DB_ESCAPE_CHARACTER: &str = "\"";
pub const MSSQL_DB_ESCAPE_OPEN: &str = "[";
pub const MSSQL_DB_ESCAPE_CLOSE: &str = "]";
pub const UNDERSCORE: &str = "_";
pub const SPACE_CHAR: &str = " ";
pub const PK_DATA_TYPE: &str = "Long";
pub const FK_DATA_TYPE: &str = "Integer";
pub const PK_ID: &str = "@Id";
pub const FK_ID: &str = "@ManyToOne";

const SUPPORTED_DATA_TYPES_REGEX: &str = r"^\b(?i:nvarchar|nchar|ntext|varchar|uniqueidentifier|datetimeoffset|smalldatetime|datetime2|datetime|smallmoney|tinyint|smallint|bigint|bigserial|integer|serial|decimal|numeric|timestamp|money|float|image|double|bit|boolean|bool|real|date|time|text|blob|char|xml|int)[()\d,]*$";
const PRIMARY_FOREIGN_REGEX: &str = r"^(PRIMARY|FOREIGN)";
const INT_REGEX: &str = r"^\b(tinyint|int)[()\d]*$";
const VARCHAR_REGEX: &str = r"^\bvarchar[()\d]*$";
const BIGINT_REGEX: &str = r"^\bbigint[()\d]*$";
const DATETIME_REGEX: &str = r"^\bdatetime[()\d]*$";
const BIT_REGEX: &str = r"^\bbit[()\d]*$";
const FLOAT_REGEX: &str = r"^\bfloat[()\d]*$";
const DOUBLE_REGEX: &str = r"^\bdouble[()\d]*$";
const TIME_REGEX: &str = r"^\btime[()\d]*$";
const TIMESTAMP_REGEX: &str = r"^\btimestamp[()\d]*$";

const PG_INTEGER_REGEX: &str = r"^\b(integer|smallint|serial)[()\d]*$";
const PG_BIGINT_REGEX: &str = r"^\b(bigint|bigserial)[()\d]*$";
const PG_TEXT_REGEX: &str = r"^\btext$";
const PG_BOOLEAN_REGEX: &str = r"^\b(boolean|bool)$";
const PG_NUMERIC_REGEX: &str = r"^\bnumeric[()\d,]*$";
const PG_REAL_REGEX: &str = r"^\breal[()\d]*$";
const PG_DATE_REGEX: &str = r"^\bdate$";

const MSSQL_NVARCHAR_REGEX: &str = r"^\b(nvarchar|nchar|ntext|char)[()\d]*$";
const MSSQL_MONEY_REGEX: &str = r"^\b(money|smallmoney)$";
const MSSQL_UNIQUEIDENTIFIER_REGEX: &str = r"^\buniqueidentifier$";
const MSSQL_DATETIME2_REGEX: &str = r"^\b(datetime2|datetimeoffset|smalldatetime)[()\d]*$";
const MSSQL_DECIMAL_REGEX: &str = r"^\bdecimal[()\d,]*$";
const MSSQL_IMAGE_REGEX: &str = r"^\b(image|xml)$";

const FOREIGN_KEY_REFERENCES_REGEX: &str =
    r"^FOREIGN KEY\s*\(([^)]+)\)\s+REFERENCES\s+([^(\s]+)\s*\(([^)]+)\)";
const PRIMARY_KEY_S_S: &str = r"^PRIMARY KEY\s*\(([^)]+)\).*";

pub static SUPPORTED_DATA_TYPES_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(SUPPORTED_DATA_TYPES_REGEX).expect("supported types regex"));
pub static PRIMARY_FOREIGN_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PRIMARY_FOREIGN_REGEX).expect("primary/foreign regex"));
pub static VARCHAR_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(VARCHAR_REGEX).expect("varchar regex"));
pub static MSSQL_NVARCHAR_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(MSSQL_NVARCHAR_REGEX).expect("mssql nvarchar regex"));
pub static PG_TEXT_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PG_TEXT_REGEX).expect("pg text regex"));
pub static MSSQL_UNIQUEIDENTIFIER_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(MSSQL_UNIQUEIDENTIFIER_REGEX).expect("mssql uniqueidentifier regex"));
pub static MSSQL_IMAGE_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(MSSQL_IMAGE_REGEX).expect("mssql image regex"));
pub static PG_BIGINT_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PG_BIGINT_REGEX).expect("pg bigint regex"));
pub static BIGINT_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(BIGINT_REGEX).expect("bigint regex"));
pub static INT_PATTERN: Lazy<Regex> = Lazy::new(|| Regex::new(INT_REGEX).expect("int regex"));
pub static PG_INTEGER_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PG_INTEGER_REGEX).expect("pg integer regex"));
pub static MSSQL_DATETIME2_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(MSSQL_DATETIME2_REGEX).expect("mssql datetime2 regex"));
pub static DATETIME_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(DATETIME_REGEX).expect("datetime regex"));
pub static PG_DATE_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PG_DATE_REGEX).expect("pg date regex"));
pub static PG_BOOLEAN_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PG_BOOLEAN_REGEX).expect("pg boolean regex"));
pub static BIT_PATTERN: Lazy<Regex> = Lazy::new(|| Regex::new(BIT_REGEX).expect("bit regex"));
pub static FLOAT_PATTERN: Lazy<Regex> = Lazy::new(|| Regex::new(FLOAT_REGEX).expect("float regex"));
pub static PG_REAL_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PG_REAL_REGEX).expect("pg real regex"));
pub static DOUBLE_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(DOUBLE_REGEX).expect("double regex"));
pub static PG_NUMERIC_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PG_NUMERIC_REGEX).expect("pg numeric regex"));
pub static MSSQL_DECIMAL_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(MSSQL_DECIMAL_REGEX).expect("mssql decimal regex"));
pub static MSSQL_MONEY_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(MSSQL_MONEY_REGEX).expect("mssql money regex"));
pub static TIME_PATTERN: Lazy<Regex> = Lazy::new(|| Regex::new(TIME_REGEX).expect("time regex"));
pub static TIMESTAMP_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(TIMESTAMP_REGEX).expect("timestamp regex"));
pub static FOREIGN_KEY_REFERENCES_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(FOREIGN_KEY_REFERENCES_REGEX).expect("fk references regex"));
pub static PRIMARY_KEY_PATTERN: Lazy<Regex> =
    Lazy::new(|| Regex::new(PRIMARY_KEY_S_S).expect("primary key regex"));

#[cfg(test)]
mod tests {
    //! Pattern pins for the DDL-type classification cascade.
    //!
    //! `parser::java_type_for` walks 22 type patterns in priority order;
    //! every pattern decides whether a DDL token like `varchar(255)`
    //! routes to `String`, `Long`, `LocalDateTime`, etc. A wrong anchor
    //! or missing alternative silently classifies the column as the
    //! fallback `String`, which compiles but corrupts the schema.
    //!
    //! `java_type_for` lowercases input before matching, so these tests
    //! only exercise lowercase variants (no case-insensitive `(?i:…)` is
    //! present in most patterns by design — anchoring + pre-lowering is
    //! what enforces correctness).
    //!
    //! Also pins that every `Lazy<Regex>` compiles without panicking
    //! (a malformed regex would only surface at first use, hiding the
    //! defect from CI runs that never hit that specific dialect path).

    use super::*;

    // -------------------------------------------------------- VARCHAR

    #[test]
    fn varchar_pattern_matches_bare_varchar() {
        assert!(VARCHAR_PATTERN.is_match("varchar"));
    }

    #[test]
    fn varchar_pattern_matches_varchar_with_size() {
        assert!(VARCHAR_PATTERN.is_match("varchar(255)"));
    }

    #[test]
    fn varchar_pattern_rejects_trailing_word_chars() {
        // The regex set `[()\d]*` excludes letters, so anything beyond
        // `varchar` + digits/parens must NOT match.
        assert!(!VARCHAR_PATTERN.is_match("varcharblah"));
        assert!(!VARCHAR_PATTERN.is_match("varchar_foo"));
    }

    #[test]
    fn varchar_pattern_rejects_substring_in_longer_word() {
        // Anchoring (`^…$`) blocks substring matches.
        assert!(!VARCHAR_PATTERN.is_match("nvarchar"));
    }

    // ------------------------------------------------------- INT / BIGINT

    #[test]
    fn int_pattern_matches_int_and_tinyint() {
        assert!(INT_PATTERN.is_match("int"));
        assert!(INT_PATTERN.is_match("tinyint"));
    }

    #[test]
    fn int_pattern_matches_int_with_size() {
        assert!(INT_PATTERN.is_match("int(11)"));
        assert!(INT_PATTERN.is_match("tinyint(1)"));
    }

    #[test]
    fn int_pattern_rejects_smallint_and_bigint() {
        // smallint / bigint route to PG_INTEGER / BIGINT, NOT INT.
        // Pinning this prevents accidental promotion across types.
        assert!(!INT_PATTERN.is_match("smallint"));
        assert!(!INT_PATTERN.is_match("bigint"));
    }

    #[test]
    fn bigint_pattern_matches_bigint() {
        assert!(BIGINT_PATTERN.is_match("bigint"));
        assert!(BIGINT_PATTERN.is_match("bigint(20)"));
    }

    #[test]
    fn bigint_pattern_rejects_int() {
        assert!(!BIGINT_PATTERN.is_match("int"));
    }

    // -------------------------------------------------- PG types

    #[test]
    fn pg_integer_pattern_matches_pg_integer_smallint_serial() {
        for t in &["integer", "smallint", "serial"] {
            assert!(
                PG_INTEGER_PATTERN.is_match(t),
                "PG_INTEGER_PATTERN must match {t}"
            );
        }
    }

    #[test]
    fn pg_bigint_pattern_matches_bigint_and_bigserial() {
        assert!(PG_BIGINT_PATTERN.is_match("bigint"));
        assert!(PG_BIGINT_PATTERN.is_match("bigserial"));
    }

    #[test]
    fn pg_text_pattern_matches_only_bare_text() {
        assert!(PG_TEXT_PATTERN.is_match("text"));
        // Anchored end (`$`) so `text2` doesn't match.
        assert!(!PG_TEXT_PATTERN.is_match("text2"));
        // And nothing substring-like sneaks through.
        assert!(!PG_TEXT_PATTERN.is_match("textfield"));
    }

    #[test]
    fn pg_boolean_pattern_matches_boolean_and_bool() {
        assert!(PG_BOOLEAN_PATTERN.is_match("boolean"));
        assert!(PG_BOOLEAN_PATTERN.is_match("bool"));
    }

    #[test]
    fn pg_boolean_pattern_rejects_booleanish_extensions() {
        assert!(!PG_BOOLEAN_PATTERN.is_match("booleans"));
        assert!(!PG_BOOLEAN_PATTERN.is_match("bools"));
    }

    #[test]
    fn pg_date_pattern_matches_only_bare_date() {
        assert!(PG_DATE_PATTERN.is_match("date"));
        // `datetime` must NOT match the PG_DATE branch (datetime routes
        // through DATETIME_PATTERN earlier in the cascade).
        assert!(!PG_DATE_PATTERN.is_match("datetime"));
    }

    #[test]
    fn pg_numeric_pattern_matches_with_and_without_precision() {
        assert!(PG_NUMERIC_PATTERN.is_match("numeric"));
        assert!(PG_NUMERIC_PATTERN.is_match("numeric(10)"));
        assert!(PG_NUMERIC_PATTERN.is_match("numeric(10,2)"));
    }

    #[test]
    fn pg_real_pattern_matches_real() {
        assert!(PG_REAL_PATTERN.is_match("real"));
    }

    // ----------------------------------------------- MSSQL types

    #[test]
    fn mssql_nvarchar_pattern_matches_full_family() {
        for t in &["nvarchar", "nchar", "ntext", "char"] {
            assert!(
                MSSQL_NVARCHAR_PATTERN.is_match(t),
                "MSSQL_NVARCHAR_PATTERN must match {t}"
            );
        }
    }

    #[test]
    fn mssql_nvarchar_pattern_matches_with_size() {
        assert!(MSSQL_NVARCHAR_PATTERN.is_match("nvarchar(100)"));
    }

    #[test]
    fn mssql_uniqueidentifier_pattern_matches() {
        assert!(MSSQL_UNIQUEIDENTIFIER_PATTERN.is_match("uniqueidentifier"));
    }

    #[test]
    fn mssql_uniqueidentifier_rejects_extensions() {
        assert!(!MSSQL_UNIQUEIDENTIFIER_PATTERN.is_match("uniqueidentifier2"));
    }

    #[test]
    fn mssql_image_pattern_matches_image_and_xml() {
        assert!(MSSQL_IMAGE_PATTERN.is_match("image"));
        assert!(MSSQL_IMAGE_PATTERN.is_match("xml"));
    }

    #[test]
    fn mssql_datetime2_pattern_matches_family() {
        for t in &["datetime2", "datetimeoffset", "smalldatetime"] {
            assert!(
                MSSQL_DATETIME2_PATTERN.is_match(t),
                "MSSQL_DATETIME2_PATTERN must match {t}"
            );
        }
    }

    #[test]
    fn mssql_money_pattern_matches_money_and_smallmoney() {
        assert!(MSSQL_MONEY_PATTERN.is_match("money"));
        assert!(MSSQL_MONEY_PATTERN.is_match("smallmoney"));
    }

    #[test]
    fn mssql_decimal_pattern_matches_with_precision_scale() {
        assert!(MSSQL_DECIMAL_PATTERN.is_match("decimal"));
        assert!(MSSQL_DECIMAL_PATTERN.is_match("decimal(10,2)"));
    }

    // ------------------------------------------- datetime / time / timestamp

    #[test]
    fn datetime_pattern_matches_bare_datetime() {
        assert!(DATETIME_PATTERN.is_match("datetime"));
    }

    #[test]
    fn datetime_pattern_accidentally_matches_datetime2_via_permissive_char_class() {
        // DATETIME_REGEX = `^\bdatetime[()\d]*$` — the `[()\d]*` set
        // allows trailing digits, so `datetime2` matches DATETIME_PATTERN.
        // Cascade order in `parser::java_type_for` saves correctness:
        // MSSQL_DATETIME2_PATTERN is consulted BEFORE DATETIME_PATTERN,
        // so `datetime2` routes to LocalDateTime via the datetime2 branch
        // first. Pinning the bare-regex behaviour here documents this
        // subtle ordering dependency — if `java_type_for` ever reorders
        // checks, this test reminds the reader that DATETIME_PATTERN
        // alone cannot distinguish `datetime` from `datetime2`.
        assert!(DATETIME_PATTERN.is_match("datetime2"));
    }

    #[test]
    fn time_pattern_matches_bare_time() {
        assert!(TIME_PATTERN.is_match("time"));
    }

    #[test]
    fn time_pattern_rejects_timestamp() {
        assert!(!TIME_PATTERN.is_match("timestamp"));
    }

    #[test]
    fn timestamp_pattern_matches_bare_timestamp() {
        assert!(TIMESTAMP_PATTERN.is_match("timestamp"));
    }

    // --------------------------------------------------- bit / float / double

    #[test]
    fn bit_pattern_matches_bit() {
        assert!(BIT_PATTERN.is_match("bit"));
        assert!(BIT_PATTERN.is_match("bit(1)"));
    }

    #[test]
    fn float_pattern_matches_float() {
        assert!(FLOAT_PATTERN.is_match("float"));
        assert!(FLOAT_PATTERN.is_match("float(7)"));
    }

    #[test]
    fn double_pattern_matches_double() {
        assert!(DOUBLE_PATTERN.is_match("double"));
        // Note: `double precision` (PG) is folded to `double` by the
        // normalize_postgresql_words step before reaching this pattern.
    }

    // ------------------------------------ supported / pk / fk meta patterns

    #[test]
    fn primary_foreign_pattern_matches_primary_and_foreign_starts() {
        assert!(PRIMARY_FOREIGN_PATTERN.is_match("PRIMARY"));
        assert!(PRIMARY_FOREIGN_PATTERN.is_match("FOREIGN"));
        assert!(PRIMARY_FOREIGN_PATTERN.is_match("PRIMARY KEY"));
        assert!(PRIMARY_FOREIGN_PATTERN.is_match("FOREIGN KEY"));
    }

    #[test]
    fn primary_foreign_pattern_is_case_sensitive() {
        // The Kotlin parser tokenizes & upper-cases keywords before this
        // pattern is consulted, so lowercase must NOT match.
        assert!(!PRIMARY_FOREIGN_PATTERN.is_match("primary"));
        assert!(!PRIMARY_FOREIGN_PATTERN.is_match("foreign"));
    }

    #[test]
    fn supported_data_types_pattern_matches_common_dialect_types() {
        let ok = [
            "varchar(255)",
            "int(11)",
            "bigint",
            "text",
            "datetime",
            "timestamp",
            "boolean",
            "decimal(10,2)",
            "uniqueidentifier",
        ];
        for t in &ok {
            assert!(
                SUPPORTED_DATA_TYPES_PATTERN.is_match(t),
                "SUPPORTED_DATA_TYPES_PATTERN must accept {t}"
            );
        }
    }

    #[test]
    fn supported_data_types_pattern_is_case_insensitive() {
        // The regex carries `(?i:…)` per the file's header comment, so
        // uppercase/mixed-case dialect tokens must still match.
        assert!(SUPPORTED_DATA_TYPES_PATTERN.is_match("VARCHAR(255)"));
        assert!(SUPPORTED_DATA_TYPES_PATTERN.is_match("BigInt"));
    }

    #[test]
    fn supported_data_types_pattern_rejects_unknown_types() {
        // ENUM, JSON, GEOMETRY etc. are not in the supported set.
        assert!(!SUPPORTED_DATA_TYPES_PATTERN.is_match("enum"));
        assert!(!SUPPORTED_DATA_TYPES_PATTERN.is_match("json"));
        assert!(!SUPPORTED_DATA_TYPES_PATTERN.is_match("geometry"));
    }

    // -------------------------------------- FK references / PK capture groups

    #[test]
    fn foreign_key_references_pattern_captures_three_groups() {
        let caps = FOREIGN_KEY_REFERENCES_PATTERN
            .captures("FOREIGN KEY (user_id) REFERENCES users(id)")
            .expect("must capture full FK declaration");
        assert_eq!(caps.get(1).unwrap().as_str(), "user_id");
        assert_eq!(caps.get(2).unwrap().as_str(), "users");
        assert_eq!(caps.get(3).unwrap().as_str(), "id");
    }

    #[test]
    fn foreign_key_references_pattern_tolerates_extra_whitespace() {
        let caps = FOREIGN_KEY_REFERENCES_PATTERN
            .captures("FOREIGN KEY   (a)   REFERENCES   t(b)")
            .expect("must capture under whitespace variance");
        assert_eq!(caps.get(1).unwrap().as_str(), "a");
        assert_eq!(caps.get(2).unwrap().as_str(), "t");
        assert_eq!(caps.get(3).unwrap().as_str(), "b");
    }

    #[test]
    fn primary_key_pattern_captures_column_list() {
        let caps = PRIMARY_KEY_PATTERN
            .captures("PRIMARY KEY (id, name)")
            .expect("must capture inside parens");
        assert_eq!(caps.get(1).unwrap().as_str(), "id, name");
    }

    // ----------------------------------------------- regex compile pins

    #[test]
    fn every_lazy_regex_compiles() {
        // Force-touch every Lazy<Regex>. A malformed regex would only
        // surface at first call from java_type_for; CI runs that never
        // hit that specific dialect path would silently miss the bug.
        let _ = SUPPORTED_DATA_TYPES_PATTERN.is_match("x");
        let _ = PRIMARY_FOREIGN_PATTERN.is_match("X");
        let _ = VARCHAR_PATTERN.is_match("x");
        let _ = MSSQL_NVARCHAR_PATTERN.is_match("x");
        let _ = PG_TEXT_PATTERN.is_match("x");
        let _ = MSSQL_UNIQUEIDENTIFIER_PATTERN.is_match("x");
        let _ = MSSQL_IMAGE_PATTERN.is_match("x");
        let _ = PG_BIGINT_PATTERN.is_match("x");
        let _ = BIGINT_PATTERN.is_match("x");
        let _ = INT_PATTERN.is_match("x");
        let _ = PG_INTEGER_PATTERN.is_match("x");
        let _ = MSSQL_DATETIME2_PATTERN.is_match("x");
        let _ = DATETIME_PATTERN.is_match("x");
        let _ = PG_DATE_PATTERN.is_match("x");
        let _ = PG_BOOLEAN_PATTERN.is_match("x");
        let _ = BIT_PATTERN.is_match("x");
        let _ = FLOAT_PATTERN.is_match("x");
        let _ = PG_REAL_PATTERN.is_match("x");
        let _ = DOUBLE_PATTERN.is_match("x");
        let _ = PG_NUMERIC_PATTERN.is_match("x");
        let _ = MSSQL_DECIMAL_PATTERN.is_match("x");
        let _ = MSSQL_MONEY_PATTERN.is_match("x");
        let _ = TIME_PATTERN.is_match("x");
        let _ = TIMESTAMP_PATTERN.is_match("x");
        let _ = FOREIGN_KEY_REFERENCES_PATTERN.is_match("x");
        let _ = PRIMARY_KEY_PATTERN.is_match("x");
    }
}
