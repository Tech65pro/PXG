package com.tech65pro.vortexmc.json;

/** Thrown when a JSON value is accessed with the wrong accessor. */
public class JsonTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonTypeException(String message) {
        super(message);
    }
}
