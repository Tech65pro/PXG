package com.tech65pro.vortexmc.json;

/**
 * Number scalar. Values are stored as {@code long} when they are integral so that integer
 * settings round-trip exactly, and as {@code double} otherwise.
 */
public final class JsonNumber extends JsonValue {

    private final long longValue;
    private final double doubleValue;
    private final boolean integral;

    JsonNumber(long value) {
        this.longValue = value;
        this.doubleValue = value;
        this.integral = true;
    }

    JsonNumber(double value) {
        this.longValue = Math.round(value);
        this.doubleValue = value;
        this.integral = false;
    }

    @Override
    public Type type() {
        return Type.NUMBER;
    }

    @Override
    public long asLong() {
        return longValue;
    }

    @Override
    public int asInt() {
        return (int) longValue;
    }

    @Override
    public double asDouble() {
        return doubleValue;
    }

    @Override
    public boolean asBoolean() {
        return longValue != 0L;
    }

    @Override
    public String asString() {
        if (integral) {
            return Long.toString(longValue);
        }
        double rounded = Math.round(doubleValue * 1_000_000.0) / 1_000_000.0;
        if (rounded == Math.rint(rounded) && !Double.isInfinite(rounded)) {
            return Long.toString((long) rounded);
        }
        return Double.toString(doubleValue);
    }
}
