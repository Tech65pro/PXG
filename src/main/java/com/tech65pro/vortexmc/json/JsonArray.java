package com.tech65pro.vortexmc.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** JSON array node. */
public final class JsonArray extends JsonValue {

    private final List<JsonValue> items = new ArrayList<>();

    @Override
    public Type type() {
        return Type.ARRAY;
    }

    @Override
    public JsonArray asArray() {
        return this;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public JsonValue get(int index) {
        if (index < 0 || index >= items.size()) {
            return JsonNull.INSTANCE;
        }
        return items.get(index);
    }

    public JsonArray add(JsonValue value) {
        items.add(value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    public JsonArray add(String value) {
        return add(JsonValue.of(value));
    }

    public JsonArray add(boolean value) {
        return add(JsonValue.of(value));
    }

    public JsonArray add(long value) {
        return add(JsonValue.of(value));
    }

    public JsonArray add(int value) {
        return add(JsonValue.of(value));
    }

    public JsonArray add(double value) {
        return add(JsonValue.of(value));
    }

    public List<JsonValue> items() {
        return Collections.unmodifiableList(items);
    }
}
