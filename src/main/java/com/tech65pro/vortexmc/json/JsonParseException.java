package com.tech65pro.vortexmc.json;

/** Thrown when a document cannot be parsed. Carries a 1-based line and column. */
public class JsonParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public JsonParseException(String message, int line, int column) {
        super(message + " (line " + line + ", column " + column + ")");
        this.line = line;
        this.column = column;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }
}
