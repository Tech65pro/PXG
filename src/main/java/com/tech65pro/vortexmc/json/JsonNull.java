package com.tech65pro.vortexmc.json;

/** Null scalar. */
public final class JsonNull extends JsonValue {

    static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
    }

    @Override
    public Type type() {
        return Type.NULL;
    }

    @Override
    public String asString() {
        return "null";
    }

    @Override
    public String asString(String fallback) {
        return fallback;
    }

    @Override
    public boolean asBoolean(boolean fallback) {
        return fallback;
    }

    @Override
    public long asLong(long fallback) {
        return fallback;
    }

    @Override
    public int asInt(int fallback) {
        return fallback;
    }

    @Override
    public double asDouble(double fallback) {
        return fallback;
    }
}
