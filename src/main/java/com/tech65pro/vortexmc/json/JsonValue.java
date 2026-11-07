package com.tech65pro.vortexmc.json;

/**
 * Immutable-ish tree node used by VortexMC's configuration format.
 *
 * <p>VortexMC deliberately ships its own tiny JSON layer instead of depending on a third-party
 * library. The configuration module must be usable from any entry point (and from unit tests)
 * without pulling in Minecraft's bundled Gson, and the amount of JSON the mod needs is small
 * enough that a purpose-built reader/writer is easier to audit and guarantees byte-stable
 * output across Minecraft versions.</p>
 */
public abstract class JsonValue {

    public enum Type {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    // ------------------------------------------------------------------ factories

    public static JsonValue of(String value) {
        return value == null ? JsonNull.INSTANCE : new JsonString(value);
    }

    public static JsonValue of(boolean value) {
        return value ? JsonBoolean.TRUE : JsonBoolean.FALSE;
    }

    public static JsonValue of(long value) {
        return new JsonNumber(value);
    }

    public static JsonValue of(double value) {
        return new JsonNumber(value);
    }

    public static JsonValue of(int value) {
        return new JsonNumber(value);
    }

    public static JsonObject newObject() {
        return new JsonObject();
    }

    public static JsonArray newArray() {
        return new JsonArray();
    }

    // ------------------------------------------------------------------ queries

    public abstract Type type();

    public final boolean isObject() {
        return type() == Type.OBJECT;
    }

    public final boolean isArray() {
        return type() == Type.ARRAY;
    }

    public final boolean isString() {
        return type() == Type.STRING;
    }

    public final boolean isNumber() {
        return type() == Type.NUMBER;
    }

    public final boolean isBoolean() {
        return type() == Type.BOOLEAN;
    }

    public final boolean isNull() {
        return type() == Type.NULL;
    }

    public JsonObject asObject() {
        throw new JsonTypeException("Expected a JSON object but got " + type());
    }

    public JsonArray asArray() {
        throw new JsonTypeException("Expected a JSON array but got " + type());
    }

    public String asString() {
        throw new JsonTypeException("Expected a JSON string but got " + type());
    }

    public boolean asBoolean() {
        throw new JsonTypeException("Expected a JSON boolean but got " + type());
    }

    public long asLong() {
        throw new JsonTypeException("Expected a JSON number but got " + type());
    }

    public int asInt() {
        return (int) asLong();
    }

    public double asDouble() {
        throw new JsonTypeException("Expected a JSON number but got " + type());
    }

    /**
     * This value as an object, or an empty object when it is missing or of another type.
     *
     * <p>Used when reading a configuration section that an older or hand-edited file may simply
     * not contain. Unlike {@link #asObject()} this never throws, so a missing section degrades to
     * "nothing configured" instead of failing the whole load.</p>
     */
    public JsonObject asObjectOrEmpty() {
        return isObject() ? asObject() : new JsonObject();
    }

    /** This value as an array, or an empty array when it is missing or of another type. */
    public JsonArray asArrayOrEmpty() {
        return isArray() ? asArray() : new JsonArray();
    }

    /** Returns {@code fallback} when this value is missing ({@code null}) or of the wrong type. */
    public String asString(String fallback) {
        return isString() ? asString() : fallback;
    }

    public boolean asBoolean(boolean fallback) {
        return isBoolean() ? asBoolean() : fallback;
    }

    public long asLong(long fallback) {
        return isNumber() ? asLong() : fallback;
    }

    public int asInt(int fallback) {
        return isNumber() ? asInt() : fallback;
    }

    public double asDouble(double fallback) {
        return isNumber() ? asDouble() : fallback;
    }
}
