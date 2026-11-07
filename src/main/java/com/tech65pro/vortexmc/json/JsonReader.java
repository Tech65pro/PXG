package com.tech65pro.vortexmc.json;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Strict, allocation-light JSON reader. Only the subset VortexMC needs is supported, and
 * anything malformed is rejected rather than silently coerced, so a corrupt config file can
 * never leave the mod in a half-configured state.
 */
final class JsonReader implements Closeable {

    private final char[] buffer;
    private int position;
    private int limit;
    private int line = 1;
    private int column = 1;

    private int pushback = -2;

    JsonReader(char[] buffer, int length) {
        this.buffer = buffer;
        this.limit = length;
    }

    static JsonValue parse(Reader reader) throws IOException {
        char[] chunk = new char[8192];
        StringBuilder text = new StringBuilder();
        int read;
        while ((read = reader.read(chunk)) != -1) {
            text.append(chunk, 0, read);
        }
        return parse(text.toString());
    }

    static JsonValue parse(String text) {
        JsonReader reader = new JsonReader(text.toCharArray(), text.length());
        try {
            reader.skipWhitespace();
            if (reader.peek() == -1) {
                throw reader.error("Unexpected end of input");
            }
            JsonValue value = reader.readValue();
            reader.skipWhitespace();
            if (reader.peek() != -1) {
                throw reader.error("Trailing content after JSON value");
            }
            return value;
        } catch (IOException impossible) {
            // The buffer is fully in memory; IOException can never occur here.
            throw new JsonParseException("Unexpected IO failure: " + impossible.getMessage(), 0, 0);
        } finally {
            reader.close();
        }
    }

    private JsonValue readValue() throws IOException {
        int c = peek();
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return JsonValue.of(readString());
            case 't':
                expectLiteral("true");
                return JsonValue.of(true);
            case 'f':
                expectLiteral("false");
                return JsonValue.of(false);
            case 'n':
                expectLiteral("null");
                return JsonNull.INSTANCE;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return readNumber();
                }
                throw error("Unexpected character '" + (char) c + "'");
        }
    }

    private JsonObject readObject() throws IOException {
        next(); // consume '{'
        JsonObject object = JsonValue.newObject();
        skipWhitespace();
        if (peek() == '}') {
            next();
            return object;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Expected a string key in object");
            }
            String key = readString();
            skipWhitespace();
            if (next() != ':') {
                throw error("Expected ':' after object key");
            }
            skipWhitespace();
            object.put(key, readValue());
            skipWhitespace();
            int c = next();
            if (c == '}') {
                return object;
            }
            if (c != ',') {
                throw error("Expected ',' or '}' in object");
            }
        }
    }

    private JsonArray readArray() throws IOException {
        next(); // consume '['
        JsonArray array = JsonValue.newArray();
        skipWhitespace();
        if (peek() == ']') {
            next();
            return array;
        }
        while (true) {
            skipWhitespace();
            array.add(readValue());
            skipWhitespace();
            int c = next();
            if (c == ']') {
                return array;
            }
            if (c != ',') {
                throw error("Expected ',' or ']' in array");
            }
        }
    }

    private String readString() throws IOException {
        next(); // consume '"'
        StringBuilder out = new StringBuilder();
        while (true) {
            int c = next();
            if (c == -1) {
                throw error("Unterminated string");
            }
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                int escape = next();
                switch (escape) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        int code = 0;
                        for (int i = 0; i < 4; i++) {
                            int digit = next();
                            int value = Character.digit((char) digit, 16);
                            if (value < 0) {
                                throw error("Invalid unicode escape");
                            }
                            code = (code << 4) | value;
                        }
                        out.append((char) code);
                    }
                    default -> throw error("Invalid escape sequence");
                }
                continue;
            }
            if (c < 0x20) {
                throw error("Unescaped control character in string");
            }
            out.append((char) c);
        }
    }

    private JsonValue readNumber() throws IOException {
        StringBuilder raw = new StringBuilder();
        if (peek() == '-') {
            raw.append((char) next());
        }
        while (true) {
            int c = peek();
            if (c >= '0' && c <= '9') {
                raw.append((char) next());
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                raw.append((char) next());
            } else {
                break;
            }
        }
        String text = raw.toString();
        if (text.isEmpty() || "-".equals(text)) {
            throw error("Invalid number");
        }
        try {
            if (text.indexOf('.') < 0 && text.indexOf('e') < 0 && text.indexOf('E') < 0) {
                return JsonValue.of(Long.parseLong(text));
            }
            return JsonValue.of(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            throw error("Invalid number '" + text + "'");
        }
    }

    private void expectLiteral(String literal) throws IOException {
        for (int i = 0; i < literal.length(); i++) {
            if (next() != literal.charAt(i)) {
                throw error("Invalid literal, expected '" + literal + "'");
            }
        }
    }

    private void skipWhitespace() throws IOException {
        while (true) {
            int c = peek();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                advance();
            } else {
                return;
            }
        }
    }

    private JsonParseException error(String message) {
        return new JsonParseException(message, line, column);
    }

    private int peek() throws IOException {
        if (pushback != -2) {
            return pushback;
        }
        if (position >= limit) {
            return -1;
        }
        return buffer[position];
    }

    private int next() throws IOException {
        if (pushback != -2) {
            int value = pushback;
            pushback = -2;
            return value;
        }
        if (position >= limit) {
            return -1;
        }
        char c = buffer[position++];
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private void advance() throws IOException {
        if (pushback != -2) {
            pushback = -2;
        } else {
            next();
        }
    }

    @Override
    public void close() {
        // nothing to release: the buffer is owned by the caller
    }
}
