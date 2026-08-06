package com.ediqa.edi.tests;

import com.ediqa.edi.db.DbValidator;
import com.ediqa.edi.exception.EdiValidationException;
import com.ediqa.edi.model.EdiDocument;
import com.ediqa.edi.model.LineItem;
import com.ediqa.edi.parser.EdiParser;
import com.ediqa.edi.service.EdiIngestionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * End-to-end tests for the EDI parsing and H2 ingestion pipeline.
 *
 * <ul>
 *   <li>{@link #testEdi850ParseIngestAndVerify} — happy path: reads the sample 850,
 *       parses it, ingests into H2, and asserts every field in the DB matches
 *       the source file.</li>
 *   <li>{@link #testEdi855AckParseAndVerify} — parse-only: reads the sample 855
 *       acknowledgment, parses it via {@link EdiParser}, and asserts all header
 *       fields and both PO1 line items match the source file.</li>
 *   <li>{@link #testMalformedEdiThrowsValidationException} — negative test:
 *       feeds a file that is missing SE/GE/IEA and asserts
 *       {@link EdiValidationException} is thrown.</li>
 * </ul>
 */
public class EdiIngestionTest {

    private static final Logger log = LogManager.getLogger(EdiIngestionTest.class);

    private static final String JDBC_URL =
        "jdbc:h2:mem:edidb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private static Connection conn;
    private static EdiParser           parser;
    private static EdiIngestionService ingestionService;
    private static DbValidator         dbValidator;

    // ── lifecycle ─────────────────────────────────────────────────────────────

    @BeforeClass
    public static void setUp() throws Exception {
        conn = DriverManager.getConnection(JDBC_URL, "sa", "");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_orders (
                    id               INT AUTO_INCREMENT PRIMARY KEY,
                    control_number   VARCHAR(9)  NOT NULL,
                    transaction_set_id VARCHAR(3) NOT NULL,
                    sender_id        VARCHAR(15),
                    receiver_id      VARCHAR(15),
                    po_number        VARCHAR(50) NOT NULL,
                    po_date          VARCHAR(8),
                    line_count       INT
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS line_items (
                    id                  INT AUTO_INCREMENT PRIMARY KEY,
                    po_number           VARCHAR(50) NOT NULL,
                    line_number         VARCHAR(6),
                    quantity            VARCHAR(15),
                    unit_of_measure     VARCHAR(2),
                    unit_price          VARCHAR(17),
                    vendor_part_number  VARCHAR(48)
                )""");
        }

        parser           = new EdiParser();
        ingestionService = new EdiIngestionService(conn);
        dbValidator      = new DbValidator(conn);

        log.info("H2 in-memory DB initialised; schema created");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
            log.info("H2 connection closed");
        }
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test(description = "850 PO: parse → ingest into H2 → assert DB matches source EDI")
    public void testEdi850ParseIngestAndVerify() throws Exception {
        String content = readResource("sample-edi/850_purchase_order.edi");

        // ── 1. Parse ──────────────────────────────────────────────────────────
        EdiDocument doc = parser.parse(content);

        assertEquals(doc.getTransactionSetId(),    "850",              "transaction set ID");
        assertEquals(doc.getPurchaseOrderNumber(), "PO-20260801-001",  "PO number from BEG03");
        assertEquals(doc.getPurchaseOrderDate(),   "20260801",         "PO date from BEG05");
        assertEquals(doc.getSenderId(),            "SENDER",           "sender ID from ISA06");
        assertEquals(doc.getReceiverId(),          "RECEIVER",         "receiver ID from ISA08");
        assertEquals(doc.getLineItems().size(),    2,                  "line item count");

        LineItem first = doc.getLineItems().get(0);
        assertEquals(first.getLineNumber(),       "1");
        assertEquals(first.getQuantity(),         "5");
        assertEquals(first.getUnitOfMeasure(),    "EA");
        assertEquals(first.getUnitPrice(),        "19.99");
        assertEquals(first.getVendorPartNumber(), "WDG-100");

        LineItem second = doc.getLineItems().get(1);
        assertEquals(second.getQuantity(),         "10");
        assertEquals(second.getVendorPartNumber(), "WDG-200");

        log.info("Parser assertions passed: {}", doc);

        // ── 2. Ingest ─────────────────────────────────────────────────────────
        ingestionService.ingest(doc);
        log.info("Ingestion completed for PO: {}", doc.getPurchaseOrderNumber());

        // ── 3. DB header verification ─────────────────────────────────────────
        assertTrue(dbValidator.purchaseOrderExists(doc.getPurchaseOrderNumber()),
            "purchase_orders row must exist");

        Map<String, Object> row = dbValidator.getPurchaseOrder(doc.getPurchaseOrderNumber());
        assertEquals(row.get("PO_NUMBER"),           "PO-20260801-001");
        assertEquals(row.get("TRANSACTION_SET_ID"),  "850");
        assertEquals(row.get("PO_DATE"),             "20260801");
        assertEquals(row.get("SENDER_ID"),           "SENDER");
        assertEquals(row.get("RECEIVER_ID"),         "RECEIVER");
        assertEquals(row.get("LINE_COUNT"),          2);

        // ── 4. DB line-item verification ──────────────────────────────────────
        List<Map<String, Object>> items = dbValidator.getLineItems(doc.getPurchaseOrderNumber());
        assertEquals(items.size(), 2, "line_items row count");

        Map<String, Object> li1 = items.get(0);
        assertEquals(li1.get("LINE_NUMBER"),        "1");
        assertEquals(li1.get("QUANTITY"),           "5");
        assertEquals(li1.get("UNIT_OF_MEASURE"),    "EA");
        assertEquals(li1.get("UNIT_PRICE"),         "19.99");
        assertEquals(li1.get("VENDOR_PART_NUMBER"), "WDG-100");

        Map<String, Object> li2 = items.get(1);
        assertEquals(li2.get("QUANTITY"),           "10");
        assertEquals(li2.get("VENDOR_PART_NUMBER"), "WDG-200");

        log.info("All DB assertions passed for PO: {}", doc.getPurchaseOrderNumber());
    }

    @Test(description = "855 PO Ack: parse sample 855 and assert all header fields and line items (parse-only, no DB ingestion)")
    public void testEdi855AckParseAndVerify() throws Exception {
        String content = readResource("sample-edi/855_purchase_order_ack.edi");

        // ── 1. Parse ──────────────────────────────────────────────────────────
        EdiDocument doc = parser.parse(content);

        // ── 2. Header assertions ──────────────────────────────────────────────
        assertEquals(doc.getTransactionSetId(),    "855",             "transaction set ID");
        assertEquals(doc.getPurchaseOrderNumber(), "PO-20260801-001", "PO number from BAK03");
        assertEquals(doc.getPurchaseOrderDate(),   "20260801",        "PO date from BAK04");
        assertEquals(doc.getSenderId(),            "RECEIVER",        "sender ID from ISA06");
        assertEquals(doc.getReceiverId(),          "SENDER",          "receiver ID from ISA08");
        assertEquals(doc.getLineItems().size(),    2,                 "line item count");

        // ── 3. Line-item assertions ───────────────────────────────────────────
        LineItem first = doc.getLineItems().get(0);
        assertEquals(first.getQuantity(),         "5",       "line 1 quantity");
        assertEquals(first.getUnitOfMeasure(),    "EA",      "line 1 unit of measure");
        assertEquals(first.getUnitPrice(),        "19.99",   "line 1 unit price");
        assertEquals(first.getVendorPartNumber(), "WDG-100", "line 1 vendor part number");

        LineItem second = doc.getLineItems().get(1);
        assertEquals(second.getQuantity(),         "10",      "line 2 quantity");
        assertEquals(second.getUnitOfMeasure(),    "EA",      "line 2 unit of measure");
        assertEquals(second.getUnitPrice(),        "8.49",    "line 2 unit price");
        assertEquals(second.getVendorPartNumber(), "WDG-200", "line 2 vendor part number");

        // DB ingestion not performed — EdiIngestionService currently supports 850 only (future iteration)

        log.info("All 855 parser assertions passed for PO: {}", doc.getPurchaseOrderNumber());
    }

    @Test(
        description = "Malformed 850 missing SE/GE/IEA must throw EdiValidationException",
        expectedExceptions = EdiValidationException.class,
        expectedExceptionsMessageRegExp = ".*Missing required segment.*"
    )
    public void testMalformedEdiThrowsValidationException() throws Exception {
        String content = readResource("sample-edi/malformed_850.edi");
        parser.parse(content); // must throw
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String readResource(String path) throws Exception {
        try (InputStream is = EdiIngestionTest.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
