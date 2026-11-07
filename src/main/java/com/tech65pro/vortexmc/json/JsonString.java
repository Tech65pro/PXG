package com.tech65pro.vortexmc.json;

/** String scalar. */
public final class JsonString extends JsonValue {

    private final String value;

    JsonString(String value) {
        this.value = value;
    }

    @Override
    public Type type() {
        return Type.STRING;
    }

    @Override
    public String asString() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
