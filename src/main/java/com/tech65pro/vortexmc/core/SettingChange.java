package com.tech65pro.vortexmc.core;

/** A single applied setting change, kept so it can be reported and undone. */
public final class SettingChange {

    public enum Source {
        MANUAL("Manual"),
        PROFILE("Profile"),
        SMART_AUTO("Smart Auto");

        private final String label;

        Source(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Setting setting;
    private final String beforeValue;
    private final String afterValue;
    private final Source source;
    private final long timestampMillis;
    private boolean reverted;

    public SettingChange(Setting setting,
                         String beforeValue,
                         String afterValue,
                         Source source,
                         long timestampMillis) {
        this.setting = setting;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.source = source;
        this.timestampMillis = timestampMillis;
    }

    public Setting setting() {
        return setting;
    }

    public String beforeValue() {
        return beforeValue;
    }

    public String afterValue() {
        return afterValue;
    }

    public Source source() {
        return source;
    }

    public long timestampMillis() {
        return timestampMillis;
    }

    public boolean reverted() {
        return reverted;
    }

    public void markReverted() {
        this.reverted = true;
    }

    public String describe() {
        return setting.displayName() + ": " + beforeValue + " -> " + afterValue;
    }

    @Override
    public String toString() {
        return describe() + (reverted ? " (reverted)" : "");
    }
}
