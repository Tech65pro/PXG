package com.tech65pro.vortexmc.client;

import com.tech65pro.vortexmc.core.Setting;

/** One setting actually changed by a profile or a Smart Auto action. */
public final class SettingChangeRecord {

    private final Setting setting;
    private final String before;
    private final String after;

    public SettingChangeRecord(Setting setting, String before, String after) {
        this.setting = setting;
        this.before = before;
        this.after = after;
    }

    public Setting setting() {
        return setting;
    }

    public String before() {
        return before;
    }

    public String after() {
        return after;
    }

    public String describe() {
        return setting.displayName() + ": " + (before == null ? "?" : before) + " -> " + (after == null ? "?" : after);
    }
}
