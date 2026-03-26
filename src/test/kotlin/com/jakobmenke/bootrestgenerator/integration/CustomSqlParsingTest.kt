package com.jakobmenke.bootrestgenerator.integration

import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.templates.Templates
import com.jakobmenke.bootrestgenerator.utils.Util
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomSqlParsingTest {

    private fun parseFromSql(sql: String): List<Entity> {
        val words = mutableListOf<String>()
        Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
        val entities = mutableListOf<Entity>()
        Util.parseWords(entities, words)
        return entities
    }

    // ── Complex SQL structures ─────────────────────────────────────────

    @Test
    fun tableWithMixedComments() {
        val sql = """
            -- This is a comment
            # This is also a comment
            CREATE TABLE `PRODUCT`
            (
                -- Primary key column
                `PRODUCT_ID` int(11) NOT NULL AUTO_INCREMENT,
                `NAME` varchar(255) NOT NULL,
                # Price column
                `PRICE` double NOT NULL,
                PRIMARY KEY (`PRODUCT_ID`) extra words pad here to ten
            )
        """.trimIndent()
        val entities = parseFromSql(sql)
        assertEquals(1, entities.size)
        assertEquals("Product", entities[0].entityName)
        assertTrue(entities[0].columns.any { it.databaseColumnName == "NAME" })
        assertTrue(entities[0].columns.any { it.javaType == "Double" })
    }

    @Test
    fun multipleTablesWithForeignKeys() {
        val sql = """
            CREATE TABLE `AUTHOR`
            (
                `AUTHOR_ID` int(11) NOT NULL AUTO_INCREMENT,
                `NAME` varchar(200) NOT NULL,
                PRIMARY KEY (`AUTHOR_ID`) extra words pad here to ten
            )
            CREATE TABLE `BOOK`
            (
                `BOOK_ID` int(11) NOT NULL AUTO_INCREMENT,
                `AUTHOR_ID` int(11) NOT NULL,
                `TITLE` varchar(500) NOT NULL,
                `PAGE_COUNT` int(11) NOT NULL,
                `PUBLISH_DATE` datetime NOT NULL,
                PRIMARY KEY (`BOOK_ID`) extra words pad here to ten,
                FOREIGN KEY (`AUTHOR_ID`) REFERENCES `AUTHOR` (`AUTHOR_ID`) ON DELETE CASCADE
            )
        """.trimIndent()
        val entities = parseFromSql(sql)
        assertEquals(2, entities.size)

        val author = entities.find { it.entityName == "Author" }!!
        assertEquals(1, author.columns.count { it.databaseIdType == "@Id" })
        assertEquals(0, author.columns.count { it.databaseIdType == "@ManyToOne" })

        val book = entities.find { it.entityName == "Book" }!!
        assertEquals(1, book.columns.count { it.databaseIdType == "@Id" })
        assertEquals(1, book.columns.count { it.databaseIdType == "@ManyToOne" })
        val fk = book.columns.find { it.databaseIdType == "@ManyToOne" }!!
        assertEquals("Integer", fk.javaType)
    }

    @Test
    fun tableWithEveryDataType() {
        val sql = """
            CREATE TABLE `ALL_TYPES`
            (
                `ID` int(11) NOT NULL AUTO_INCREMENT,
                `VARCHAR_COL` varchar(255) NOT NULL,
                `INT_COL` int(11) NOT NULL,
                `TINYINT_COL` tinyint(1) NOT NULL,
                `BIGINT_COL` bigint(20) NOT NULL,
                `DATETIME_COL` datetime NOT NULL,
                `TIMESTAMP_COL` timestamp NULL,
                `FLOAT_COL` float NOT NULL,
                `DOUBLE_COL` double NOT NULL,
                `BIT_COL` bit(1) NOT NULL,
                `TIME_COL` time NOT NULL,
                PRIMARY KEY (`ID`) extra words pad here to ten
            )
        """.trimIndent()
        val entities = parseFromSql(sql)
        val cols = entities[0].columns
        val typeMap = cols.associate { (it.databaseColumnName ?: "") to (it.javaType ?: "") }

        assertEquals("String", typeMap["VARCHAR_COL"])
        assertEquals("Integer", typeMap["INT_COL"])
        assertEquals("Integer", typeMap["TINYINT_COL"])
        assertEquals("Long", typeMap["BIGINT_COL"])
        assertEquals("LocalDate", typeMap["DATETIME_COL"])
        assertEquals("LocalDateTime", typeMap["TIMESTAMP_COL"])
        assertEquals("Float", typeMap["FLOAT_COL"])
        assertEquals("Double", typeMap["DOUBLE_COL"])
        assertEquals("String", typeMap["BIT_COL"])
        assertEquals("LocalTime", typeMap["TIME_COL"])
    }

    @Test
    fun tableWithMultipleForeignKeys() {
        val sql = """
            CREATE TABLE `ORDER_ITEM`
            (
                `ORDER_ITEM_ID` int(11) NOT NULL AUTO_INCREMENT,
                `ORDER_ID` int(11) NOT NULL,
                `PRODUCT_ID` int(11) NOT NULL,
                `QUANTITY` int(11) NOT NULL,
                PRIMARY KEY (`ORDER_ITEM_ID`) extra words pad here to ten,
                FOREIGN KEY (`ORDER_ID`) REFERENCES `ORDER` (`ORDER_ID`) ON DELETE CASCADE,
                FOREIGN KEY (`PRODUCT_ID`) REFERENCES `PRODUCT` (`PRODUCT_ID`) ON DELETE CASCADE
            )
        """.trimIndent()
        val entities = parseFromSql(sql)
        val orderItem = entities[0]
        assertEquals("OrderItem", orderItem.entityName)
        val fks = orderItem.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(2, fks.size)
    }

    @Test
    fun tableWithThreeWordName() {
        val sql = """
            CREATE TABLE `USER_ROLE_MAPPING`
            (
                `ID` int(11) NOT NULL AUTO_INCREMENT,
                PRIMARY KEY (`ID`) extra words pad here to ten
            )
        """.trimIndent()
        val entities = parseFromSql(sql)
        assertEquals("UserRoleMapping", entities[0].entityName)
        assertEquals("USER_ROLE_MAPPING", entities[0].tableName)
    }

    @Test
    fun tableWithSingleWordName() {
        val sql = "CREATE TABLE `USER`\n(\n`ID` int(11) NOT NULL\n)"
        val entities = parseFromSql(sql)
        assertEquals("User", entities[0].entityName)
    }

    // ── End-to-end template generation from parsed SQL ─────────────────

    @Test
    fun parsedEntityGeneratesValidTemplate() {
        val sql = """
            CREATE TABLE `CUSTOMER`
            (
                `CUSTOMER_ID` int(11) NOT NULL AUTO_INCREMENT,
                `FIRST_NAME` varchar(100) NOT NULL,
                `LAST_NAME` varchar(100) NOT NULL,
                `EMAIL` varchar(250) NOT NULL,
                `CREATED_AT` timestamp NULL,
                PRIMARY KEY (`CUSTOMER_ID`) extra words pad here to ten
            )
        """.trimIndent()
        val entities = parseFromSql(sql)
        val customer = entities[0]

        val templates = Templates()
        val entityTemplate = templates.getEntityTemplate(customer, "com/myapp")
        assertContains(entityTemplate, "package com.myapp.entity")
        assertContains(entityTemplate, "@Table(name = \"CUSTOMER\")")
        assertContains(entityTemplate, "public class Customer implements Serializable")
        assertContains(entityTemplate, "@Id")
        assertContains(entityTemplate, "private String firstName;")
        assertContains(entityTemplate, "private String lastName;")
        assertContains(entityTemplate, "private String email;")
        assertContains(entityTemplate, "private LocalDateTime createdAt;")
        assertFalse(entityTemplate.contains("{{"))
    }

    @Test
    fun parsedEntityGeneratesValidResourceTemplate() {
        val sql = "CREATE TABLE `ORDER`\n(\n`ORDER_ID` int(11) NOT NULL\n)"
        val entities = parseFromSql(sql)
        val templates = Templates()
        val restTemplate = templates.getResourceTemplate("com/myapp", entities[0].entityName)
        assertContains(restTemplate, "class OrderResource")
        assertContains(restTemplate, "OrderDao dao")
        assertContains(restTemplate, "\"/order\"")
    }

    @Test
    fun parsedEntityGeneratesValidDaoTemplate() {
        val sql = "CREATE TABLE `PAYMENT`\n(\n`PAYMENT_ID` int(11) NOT NULL\n)"
        val entities = parseFromSql(sql)
        val templates = Templates()
        val daoTemplate = templates.getDaoTemplate("com/myapp", entities[0].entityName)
        assertContains(daoTemplate, "class PaymentDao implements GenericDao<Payment>")
        assertContains(daoTemplate, "PaymentRepository paymentRepository")
    }

    @Test
    fun parsedEntityGeneratesValidRepositoryTemplate() {
        val sql = "CREATE TABLE `INVOICE`\n(\n`INVOICE_ID` int(11) NOT NULL\n)"
        val entities = parseFromSql(sql)
        val templates = Templates()
        val repoTemplate = templates.getRepositoryTemplate("com/myapp", entities[0].entityName)
        assertContains(repoTemplate, "interface InvoiceRepository extends JpaRepository<Invoice, Long>")
    }

    // ── Edge cases ─────────────────────────────────────────────────────

    @Test
    fun commentsBeforeCreateTable() {
        val sql = """
            -- comment line 1
            -- comment line 2
            -- comment line 3
            CREATE TABLE `FOO`
            (
                `FOO_ID` int(11) NOT NULL
            )
        """.trimIndent()
        val entities = parseFromSql(sql)
        assertEquals(1, entities.size)
        assertEquals("Foo", entities[0].entityName)
    }

    @Test
    fun mysqlConditionalStatementsSkipped() {
        // These /*!...*/ lines are not comments (-- or #), they pass through
        // but they don't match CREATE TABLE so they're ignored for entity creation
        val sql = """
            /*!40101 SET @OLD = @@VAR */;
            CREATE TABLE `BAR`
            (
                `BAR_ID` int(11) NOT NULL
            )
            /*!40101 SET VAR = @OLD */;
        """.trimIndent()
        val entities = parseFromSql(sql)
        assertEquals(1, entities.size)
        assertEquals("Bar", entities[0].entityName)
    }

    @Test
    fun multipleTablesInSequence() {
        val tables = (1..5).joinToString("\n") { i ->
            "CREATE TABLE `TABLE_$i`\n(\n`ID` int(11) NOT NULL\n)"
        }
        val entities = parseFromSql(tables)
        assertEquals(5, entities.size)
        assertEquals("Table1", entities[0].entityName)
        assertEquals("Table5", entities[4].entityName)
    }

    @Test
    fun camelCaseFieldNameForMultiWordColumn() {
        val sql = "CREATE TABLE `T`\n(\n`SOME_LONG_COLUMN_NAME` varchar(50) NOT NULL\n)"
        val entities = parseFromSql(sql)
        assertEquals("someLongColumnName", entities[0].columns[0].camelCaseFieldName)
    }

    @Test
    fun databaseTypePreservedWithParentheses() {
        val sql = "CREATE TABLE `T`\n(\n`COL` varchar(4000) NOT NULL\n)"
        val entities = parseFromSql(sql)
        assertEquals("varchar(4000)", entities[0].columns[0].databaseType)
    }

    @Test
    fun intTypeWithDifferentLengths() {
        val sql = """
            CREATE TABLE `T`
            (
                `A` int(3) NOT NULL,
                `B` int(11) NOT NULL,
                `C` tinyint(1) NOT NULL,
                `D` tinyint(3) NOT NULL
            )
        """.trimIndent()
        val entities = parseFromSql(sql)
        assertTrue(entities[0].columns.all { it.javaType == "Integer" })
    }
}
