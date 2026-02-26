package org.concentus;

public class OpusException extends Exception {
    public OpusException() {
    }

    public OpusException(String message) {
        super(message);
    }

    public OpusException(String message, Throwable cause) {
        super(message, cause);
    }
}
