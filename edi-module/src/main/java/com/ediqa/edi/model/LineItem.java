package com.ediqa.edi.model;

/**
 * A single PO1 line item extracted from an EDI X12 transaction set.
 */
public class LineItem {

    private String lineNumber;        // PO101 — sequential line ID
    private String quantity;          // PO102 — quantity ordered
    private String unitOfMeasure;     // PO103 — e.g. "EA"
    private String unitPrice;         // PO104 — e.g. "19.99"
    private String vendorPartNumber;  // PO1 VP qualifier value

    public String getLineNumber()               { return lineNumber; }
    public void   setLineNumber(String v)       { this.lineNumber = v; }

    public String getQuantity()                 { return quantity; }
    public void   setQuantity(String v)         { this.quantity = v; }

    public String getUnitOfMeasure()            { return unitOfMeasure; }
    public void   setUnitOfMeasure(String v)    { this.unitOfMeasure = v; }

    public String getUnitPrice()                { return unitPrice; }
    public void   setUnitPrice(String v)        { this.unitPrice = v; }

    public String getVendorPartNumber()         { return vendorPartNumber; }
    public void   setVendorPartNumber(String v) { this.vendorPartNumber = v; }

    @Override
    public String toString() {
        return "LineItem{line=" + lineNumber + ", qty=" + quantity
            + ", price=" + unitPrice + ", vpn=" + vendorPartNumber + "}";
    }
}
