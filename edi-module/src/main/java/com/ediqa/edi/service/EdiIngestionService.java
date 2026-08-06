package com.ediqa.edi.service;

import com.ediqa.edi.model.EdiDocument;
import com.ediqa.edi.model.LineItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Inserts a parsed {@link EdiDocument} into the {@code purchase_orders} and
 * {@code line_items} tables via plain JDBC.
 *
 * <p>The caller is responsible for providing (and closing) the {@link Connection}.
 * In tests, an H2 in-memory connection is supplied by the test fixture.
 */
public class EdiIngestionService {

    private static final Logger log = LogManager.getLogger(EdiIngestionService.class);

    private static final String INSERT_PO =
        "INSERT INTO purchase_orders "
        + "(control_number, transaction_set_id, sender_id, receiver_id, "
        + " po_number, po_date, line_count) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_LINE =
        "INSERT INTO line_items "
        + "(po_number, line_number, quantity, unit_of_measure, "
        + " unit_price, vendor_part_number) "
        + "VALUES (?, ?, ?, ?, ?, ?)";

    private final Connection connection;

    public EdiIngestionService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Inserts the purchase order header and all line items in a single batch.
     *
     * @param doc validated, parsed EDI document
     * @throws SQLException on any JDBC error
     */
    public void ingest(EdiDocument doc) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_PO)) {
            ps.setString(1, doc.getControlNumber());
            ps.setString(2, doc.getTransactionSetId());
            ps.setString(3, doc.getSenderId());
            ps.setString(4, doc.getReceiverId());
            ps.setString(5, doc.getPurchaseOrderNumber());
            ps.setString(6, doc.getPurchaseOrderDate());
            ps.setInt(7, doc.getLineItems().size());
            ps.executeUpdate();
        }

        if (!doc.getLineItems().isEmpty()) {
            try (PreparedStatement ps = connection.prepareStatement(INSERT_LINE)) {
                for (LineItem item : doc.getLineItems()) {
                    ps.setString(1, doc.getPurchaseOrderNumber());
                    ps.setString(2, item.getLineNumber());
                    ps.setString(3, item.getQuantity());
                    ps.setString(4, item.getUnitOfMeasure());
                    ps.setString(5, item.getUnitPrice());
                    ps.setString(6, item.getVendorPartNumber());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        log.info("Ingested PO '{}' with {} line item(s)",
            doc.getPurchaseOrderNumber(), doc.getLineItems().size());
    }
}
