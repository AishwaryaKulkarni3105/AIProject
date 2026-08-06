package com.ediqa.edi.exception;

/**
 * Thrown by {@link com.ediqa.edi.parser.EdiParser} when an EDI document
 * is structurally invalid — e.g. a required envelope or transaction-set
 * segment is missing.
 */
public class EdiValidationException extends RuntimeException {

    public EdiValidationException(String message) {
        super(message);
    }

    public EdiValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
