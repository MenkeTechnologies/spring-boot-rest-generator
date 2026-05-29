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
