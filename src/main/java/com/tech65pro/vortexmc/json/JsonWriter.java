package com.tech65pro.vortexmc.json;

import java.io.Writer;

/** Deterministic pretty-printer for {@link JsonValue} trees. */
final class JsonWriter {

    private final Writer out;
    private final boolean pretty;
    private int depth;

    private JsonWriter(Writer out, boolean pretty) {
        this.out = out;
        this.pretty = pretty;
    }

    static void write(JsonValue value, Writer out, boolean pretty) {
        new JsonWriter(out, pretty).writeValue(value);
    }

    private void writeValue(JsonValue value) {
        if (value == null || value.isNull()) {
            writeRaw("null");
            return;
        }
        switch (value.type()) {
            case OBJECT -> writeObject(value.asObject());
            case ARRAY -> writeArray(value.asArray());
            case STRING -> writeString(value.asString());
            case BOOLEAN -> writeRaw(value.asBoolean() ? "true" : "false");
            case NUMBER -> writeNumber(value);
            case NULL -> writeRaw("null");
        }
    }

    private void writeObject(JsonObject object) {
        if (object.entries().isEmpty()) {
            writeRaw("{}");
            return;
        }
        writeRaw("{");
        depth++;
        boolean first = true;
        for (var entry : object.orderedEntries()) {
            if (!first) {
                writeRaw(",");
            }
            first = false;
            newline();
            indent();
            writeString(entry.getKey());
            writeRaw(": ");
            writeValue(entry.getValue());
        }
        depth--;
        newline();
        indent();
        writeRaw("}");
    }

    private void writeArray(JsonArray array) {
        if (array.isEmpty()) {
            writeRaw("[]");
            return;
        }
        writeRaw("[");
        depth++;
        boolean first = true;
        for (JsonValue item : array.items()) {
            if (!first) {
                writeRaw(",");
            }
            first = false;
            newline();
            indent();
            writeValue(item);
        }
        depth--;
        newline();
        indent();
        writeRaw("]");
    }

    private void writeNumber(JsonValue value) {
        if (value.isNumber()) {
            JsonNumber number = (JsonNumber) value;
            writeRaw(number.asString());
        } else {
            writeRaw("0");
        }
    }

    private void writeString(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2);
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        writeRaw(out.toString());
    }

    private void newline() {
        if (pretty) {
            writeRaw("\n");
        }
    }

    private void indent() {
        if (!pretty) {
            return;
        }
        writeRaw("  ".repeat(Math.max(0, depth)));
    }

    private void writeRaw(String text) {
        try {
            out.write(text);
        } catch (Exception e) {
            throw new JsonTypeException("Failed to write JSON: " + e.getMessage());
        }
    }
}
