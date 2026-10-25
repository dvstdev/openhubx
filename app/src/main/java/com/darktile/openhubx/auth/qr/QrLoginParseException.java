package com.darktile.openhubx.auth.qr;

/** Thrown when a scanned QR login envelope is malformed or fails validation. */
public class QrLoginParseException extends Exception {

    public QrLoginParseException(String message) {
        super(message);
    }

    public QrLoginParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
