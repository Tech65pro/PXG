package com.tech65pro.vortexmc.json;

/** Boolean scalar. */
public final class JsonBoolean extends JsonValue {

    static final JsonBoolean TRUE = new JsonBoolean(true);
    static final JsonBoolean FALSE = new JsonBoolean(false);

    private final boolean value;

    JsonBoolean(boolean value) {
        this.value = value;
    }

    @Override
    public Type type() {
        return Type.BOOLEAN;
    }

    @Override
    public boolean asBoolean() {
        return value;
    }

    @Override
    public String asString() {
        return Boolean.toString(value);
    }
}
