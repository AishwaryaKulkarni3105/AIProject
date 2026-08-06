package com.ediqa.edi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat representation of a parsed EDI X12 transaction set.
 * Fields are populated by {@link com.ediqa.edi.parser.EdiParser}.
 */
public class EdiDocument {

    private String transactionSetId;      // ST01 — e.g. "850", "855"
    private String controlNumber;         // ISA13
    private String senderId;              // ISA06 (trimmed)
    private String receiverId;            // ISA08 (trimmed)
    private String purchaseOrderNumber;   // BEG03 (850) or BAK03 (855)
    private String purchaseOrderDate;     // BEG05 (850) or BAK04 (855)
    private List<LineItem> lineItems = new ArrayList<>();

    public String getTransactionSetId()                  { return transactionSetId; }
    public void   setTransactionSetId(String v)          { this.transactionSetId = v; }

    public String getControlNumber()                     { return controlNumber; }
    public void   setControlNumber(String v)             { this.controlNumber = v; }

    public String getSenderId()                          { return senderId; }
    public void   setSenderId(String v)                  { this.senderId = v; }

    public String getReceiverId()                        { return receiverId; }
    public void   setReceiverId(String v)                { this.receiverId = v; }

    public String getPurchaseOrderNumber()               { return purchaseOrderNumber; }
    public void   setPurchaseOrderNumber(String v)       { this.purchaseOrderNumber = v; }

    public String getPurchaseOrderDate()                 { return purchaseOrderDate; }
    public void   setPurchaseOrderDate(String v)         { this.purchaseOrderDate = v; }

    public List<LineItem> getLineItems()                 { return lineItems; }
    public void           setLineItems(List<LineItem> v) { this.lineItems = v; }

    @Override
    public String toString() {
        return "EdiDocument{txSet=" + transactionSetId
            + ", po=" + purchaseOrderNumber
            + ", lines=" + lineItems.size() + "}";
    }
}
