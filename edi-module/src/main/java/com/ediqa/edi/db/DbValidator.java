package com.ediqa.edi.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Queries the {@code purchase_orders} and {@code line_items} tables to verify
 * that ingested records exist and match expected values.
 *
 * <p>Column names in the returned maps are whatever the JDBC driver reports
 * (H2 returns upper-case names by default — e.g. {@code "PO_NUMBER"}).
 */
public class DbValidator {

    private static final Logger log = LogManager.getLogger(DbValidator.class);

    private final Connection connection;

    public DbValidator(Connection connection) {
        this.connection = connection;
    }

    /**
     * Returns {@code true} if at least one {@code purchase_orders} row exists
     * for the given PO number.
     */
    public boolean purchaseOrderExists(String poNumber) throws SQLException {
        String sql = "SELECT COUNT(*) FROM purchase_orders WHERE po_number = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, poNumber);
            try (ResultSet rs = ps.executeQuery()) {
                boolean found = rs.next() && rs.getInt(1) > 0;
                log.debug("purchaseOrderExists('{}') = {}", poNumber, found);
                return found;
            }
        }
    }

    /**
     * Returns a column-name → value map for the {@code purchase_orders} row
     * identified by {@code poNumber}.
     *
     * @throws SQLException if no row is found or on any JDBC error
     */
    public Map<String, Object> getPurchaseOrder(String poNumber) throws SQLException {
        String sql = "SELECT * FROM purchase_orders WHERE po_number = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, poNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No purchase_orders row found for po_number='" + poNumber + "'");
                }
                return rowToMap(rs);
            }
        }
    }

    /**
     * Returns all {@code line_items} rows for the given PO number, ordered by
     * {@code line_number}.
     */
    public List<Map<String, Object>> getLineItems(String poNumber) throws SQLException {
        String sql = "SELECT * FROM line_items WHERE po_number = ? ORDER BY line_number";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, poNumber);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(rowToMap(rs));
                }
                log.debug("getLineItems('{}') returned {} row(s)", poNumber, rows.size());
                return rows;
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnName(i), rs.getObject(i));
        }
        return row;
    }
}
