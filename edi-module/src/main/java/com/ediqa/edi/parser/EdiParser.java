package com.ediqa.edi.parser;

import com.ediqa.edi.exception.EdiValidationException;
import com.ediqa.edi.model.EdiDocument;
import com.ediqa.edi.model.LineItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a raw X12 EDI string into an {@link EdiDocument}.
 *
 * <p>Assumptions:
 * <ul>
 *   <li>Segment terminator is {@code ~}</li>
 *   <li>Element separator is {@code *}</li>
 *   <li>Segments may be separated by newlines (for readability); these are trimmed.</li>
 *   <li>Transaction sets 850 (BEG) and 855 (BAK) are supported.</li>
 * </ul>
 *
 * <p>Required segments: ISA, GS, ST, SE, GE, IEA.
 * A missing required segment causes an {@link EdiValidationException}.
 */
public class EdiParser {

    private static final Logger log = LogManager.getLogger(EdiParser.class);

    private static final String[] REQUIRED_SEGMENTS = {"ISA", "GS", "ST", "SE", "GE", "IEA"};

    /**
     * Parses {@code ediContent} and returns a populated {@link EdiDocument}.
     *
     * @param ediContent raw X12 EDI text
     * @return parsed document
     * @throws EdiValidationException if any required segment is absent
     */
    public EdiDocument parse(String ediContent) {
        if (ediContent == null || ediContent.isBlank()) {
            throw new EdiValidationException("EDI content is empty or null");
        }

        EdiDocument doc = new EdiDocument();
        List<LineItem> lineItems = new ArrayList<>();

        boolean hasISA = false, hasGS = false, hasST = false;
        boolean hasSE  = false, hasGE  = false, hasIEA = false;

        // Split on segment terminator; trim each token to handle embedded newlines
        String[] rawSegments = ediContent.split("~");

        for (String raw : rawSegments) {
            String segment = raw.trim();
            if (segment.isEmpty()) {
                continue;
            }

            // -1 limit preserves trailing empty elements (e.g. blank BEG04)
            String[] el = segment.split("\\*", -1);
            String id = el[0];

            switch (id) {
                case "ISA":
                    hasISA = true;
                    if (el.length > 13) doc.setControlNumber(el[13]);
                    if (el.length > 6)  doc.setSenderId(el[6].trim());
                    if (el.length > 8)  doc.setReceiverId(el[8].trim());
                    break;

                case "GS":
                    hasGS = true;
                    break;

                case "ST":
                    hasST = true;
                    if (el.length > 1) doc.setTransactionSetId(el[1]);
                    break;

                case "BEG": // 850 Purchase Order
                    if (el.length > 3) doc.setPurchaseOrderNumber(el[3]);
                    if (el.length > 5) doc.setPurchaseOrderDate(el[5]);
                    break;

                case "BAK": // 855 Purchase Order Acknowledgment
                    if (el.length > 3) doc.setPurchaseOrderNumber(el[3]);
                    if (el.length > 4) doc.setPurchaseOrderDate(el[4]);
                    break;

                case "PO1":
                    LineItem item = new LineItem();
                    if (el.length > 1) item.setLineNumber(el[1]);
                    if (el.length > 2) item.setQuantity(el[2]);
                    if (el.length > 3) item.setUnitOfMeasure(el[3]);
                    if (el.length > 4) item.setUnitPrice(el[4]);
                    // PO106+ come in qualifier/value pairs; scan for VP (vendor part number)
                    for (int i = 6; i + 1 < el.length; i += 2) {
                        if ("VP".equals(el[i])) {
                            item.setVendorPartNumber(el[i + 1]);
                            break;
                        }
                    }
                    lineItems.add(item);
                    break;

                case "SE":
                    hasSE = true;
                    break;

                case "GE":
                    hasGE = true;
                    break;

                case "IEA":
                    hasIEA = true;
                    break;

                default:
                    // Intentionally ignore segments not relevant to this parser
                    break;
            }
        }

        // ── Validation ────────────────────────────────────────────────────────
        if (!hasISA) throw new EdiValidationException("Missing required segment: ISA");
        if (!hasGS)  throw new EdiValidationException("Missing required segment: GS");
        if (!hasST)  throw new EdiValidationException("Missing required segment: ST");
        if (!hasSE)  throw new EdiValidationException("Missing required segment: SE");
        if (!hasGE)  throw new EdiValidationException("Missing required segment: GE");
        if (!hasIEA) throw new EdiValidationException("Missing required segment: IEA");

        doc.setLineItems(lineItems);
        log.info("Parsed EDI document: {}", doc);
        return doc;
    }
}
