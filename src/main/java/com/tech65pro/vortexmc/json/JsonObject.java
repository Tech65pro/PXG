package com.tech65pro.vortexmc.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON object node. Insertion order is preserved so config files stay human-readable. */
public final class JsonObject extends JsonValue {

    private final Map<String, JsonValue> entries = new LinkedHashMap<>();

    @Override
    public Type type() {
        return Type.OBJECT;
    }

    @Override
    public JsonObject asObject() {
        return this;
    }

    public boolean has(String key) {
        return entries.containsKey(key);
    }

    public JsonValue get(String key) {
        return entries.get(key);
    }

    /** Never returns {@code null}: absent keys come back as {@link JsonNull}. */
    public JsonValue getOrNull(String key) {
        JsonValue v = entries.get(key);
        return v == null ? JsonNull.INSTANCE : v;
    }

    public JsonObject put(String key, String value) {
        entries.put(key, JsonValue.of(value));
        return this;
    }

    public JsonObject put(String key, boolean value) {
        entries.put(key, JsonValue.of(value));
        return this;
    }

    public JsonObject put(String key, long value) {
        entries.put(key, JsonValue.of(value));
        return this;
    }

    public JsonObject put(String key, int value) {
        entries.put(key, JsonValue.of(value));
        return this;
    }

    public JsonObject put(String key, double value) {
        entries.put(key, JsonValue.of(value));
        return this;
    }

    public JsonObject put(String key, JsonValue value) {
        entries.put(key, value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    public JsonObject putNull(String key) {
        entries.put(key, JsonNull.INSTANCE);
        return this;
    }

    public JsonObject remove(String key) {
        entries.remove(key);
        return this;
    }

    public Map<String, JsonValue> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public Iterable<Map.Entry<String, JsonValue>> orderedEntries() {
        return new ArrayList<>(entries.entrySet());
    }
}
