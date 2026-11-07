package com.tech65pro.vortexmc.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * An immutable record of the values a set of settings had at a point in time.
 *
 * <p>Values are stored as strings so a snapshot can be written to disk, shown in the GUI and
 * restored later without holding references to live Minecraft option objects.</p>
 */
public final class SettingsSnapshot {

    private final EnumMap<Setting, String> values;

    public SettingsSnapshot() {
        this.values = new EnumMap<>(Setting.class);
    }

    private SettingsSnapshot(EnumMap<Setting, String> values) {
        this.values = values;
    }

    public SettingsSnapshot put(Setting setting, String value) {
        EnumMap<Setting, String> copy = new EnumMap<>(values);
        copy.put(setting, value);
        return new SettingsSnapshot(copy);
    }

    public String get(Setting setting) {
        return values.get(setting);
    }

    public boolean has(Setting setting) {
        return values.containsKey(setting);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<Setting, String> entries() {
        return java.util.Collections.unmodifiableMap(values);
    }

    public static SettingsSnapshot of(Map<Setting, String> entries) {
        SettingsSnapshot snapshot = new SettingsSnapshot();
        for (Map.Entry<Setting, String> entry : entries.entrySet()) {
            snapshot.values.put(entry.getKey(), entry.getValue());
        }
        return snapshot;
    }
}
