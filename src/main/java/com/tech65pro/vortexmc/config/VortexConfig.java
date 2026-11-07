package com.tech65pro.vortexmc.config;

import com.tech65pro.vortexmc.core.Setting;
import com.tech65pro.vortexmc.core.SettingChange;
import com.tech65pro.vortexmc.core.SettingsSnapshot;
import com.tech65pro.vortexmc.json.Json;
import com.tech65pro.vortexmc.json.JsonArray;
import com.tech65pro.vortexmc.json.JsonObject;
import com.tech65pro.vortexmc.json.JsonValue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything VortexMC persists between sessions.
 *
 * <p>The configuration is written atomically (write to a temporary file, then move) so an
 * interrupted save can never leave a half-written file behind. Unknown keys are ignored on load
 * so a config written by a newer build does not stop an older build from starting.</p>
 */
public final class VortexConfig {

    public static final int SCHEMA_VERSION = 1;

    private static final int MAX_HISTORY = 50;

    private int schemaVersion = SCHEMA_VERSION;
    private String activeProfileId = BuiltInProfiles.BALANCED;

    private boolean smartAutoEnabled;
    private boolean requireApproval = true;
    private boolean autoRevertFailedChanges = true;
    private boolean measureBeforeActing = true;

    private int measurementSeconds = 12;
    private int cooldownSeconds = 120;
    private double dropThresholdPercent = 15.0;
    private int minFpsBeforeActing = 45;

    private boolean showOverlay = true;
    private boolean showHistory = true;
    private boolean confirmDestructiveProfiles = true;

    private final Map<String, OptimizationProfile> customProfiles = new LinkedHashMap<>();
    private SettingsSnapshot lastKnownGood = new SettingsSnapshot();
    private final List<SettingChange> changeHistory = new ArrayList<>();

    public VortexConfig() {
    }

    // ------------------------------------------------------------------ generic settings

    public int schemaVersion() {
        return schemaVersion;
    }

    public String activeProfileId() {
        return activeProfileId;
    }

    public void activeProfileId(String value) {
        this.activeProfileId = value == null ? BuiltInProfiles.BALANCED : value;
    }

    public boolean smartAutoEnabled() {
        return smartAutoEnabled;
    }

    public void smartAutoEnabled(boolean value) {
        this.smartAutoEnabled = value;
    }

    /** When true, Smart Auto asks before changing anything. Defaults to true. */
    public boolean requireApproval() {
        return requireApproval;
    }

    public void requireApproval(boolean value) {
        this.requireApproval = value;
    }

    public boolean autoRevertFailedChanges() {
        return autoRevertFailedChanges;
    }

    public void autoRevertFailedChanges(boolean value) {
        this.autoRevertFailedChanges = value;
    }

    public boolean measureBeforeActing() {
        return measureBeforeActing;
    }

    public void measureBeforeActing(boolean value) {
        this.measureBeforeActing = value;
    }

    public int measurementSeconds() {
        return measurementSeconds;
    }

    public void measurementSeconds(int value) {
        this.measurementSeconds = Math.max(4, Math.min(120, value));
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public void cooldownSeconds(int value) {
        this.cooldownSeconds = Math.max(30, Math.min(3600, value));
    }

    public double dropThresholdPercent() {
        return dropThresholdPercent;
    }

    public void dropThresholdPercent(double value) {
        this.dropThresholdPercent = Math.max(3.0, Math.min(60.0, value));
    }

    /** Smart Auto stays out of the way while the game is already running comfortably. */
    public int minFpsBeforeActing() {
        return minFpsBeforeActing;
    }

    public void minFpsBeforeActing(int value) {
        this.minFpsBeforeActing = Math.max(10, Math.min(240, value));
    }

    public boolean showOverlay() {
        return showOverlay;
    }

    public void showOverlay(boolean value) {
        this.showOverlay = value;
    }

    public boolean showHistory() {
        return showHistory;
    }

    public void showHistory(boolean value) {
        this.showHistory = value;
    }

    public boolean confirmDestructiveProfiles() {
        return confirmDestructiveProfiles;
    }

    public void confirmDestructiveProfiles(boolean value) {
        this.confirmDestructiveProfiles = value;
    }

    // ------------------------------------------------------------------ profiles

    public Map<String, OptimizationProfile> customProfiles() {
        return customProfiles;
    }

    public OptimizationProfile profile(String id) {
        if (id == null) {
            return null;
        }
        Map<String, OptimizationProfile> builtIn = BuiltInProfiles.defaults();
        if (builtIn.containsKey(id)) {
            return builtIn.get(id);
        }
        return customProfiles.get(id);
    }

    /** The profile the player currently has selected, falling back to Balanced. */
    public OptimizationProfile activeProfile() {
        OptimizationProfile profile = profile(activeProfileId);
        return profile != null ? profile : BuiltInProfiles.balanced();
    }

    public void setCustomProfile(OptimizationProfile profile) {
        if (profile == null || profile.id() == null) {
            return;
        }
        customProfiles.put(profile.id(), profile);
    }

    public void removeCustomProfile(String id) {
        if (!BuiltInProfiles.isBuiltInId(id)) {
            customProfiles.remove(id);
            if (id != null && id.equals(activeProfileId)) {
                activeProfileId = BuiltInProfiles.BALANCED;
            }
        }
    }

    // ------------------------------------------------------------------ history / restore

    public SettingsSnapshot lastKnownGood() {
        return lastKnownGood;
    }

    public void lastKnownGood(SettingsSnapshot snapshot) {
        this.lastKnownGood = snapshot == null ? new SettingsSnapshot() : snapshot;
    }

    public List<SettingChange> changeHistory() {
        return changeHistory;
    }

    public void recordChange(SettingChange change) {
        changeHistory.add(change);
        while (changeHistory.size() > MAX_HISTORY) {
            changeHistory.remove(0);
        }
    }

    /** The most recent change that has not been reverted yet, if any. */
    public SettingChange lastRevertableChange() {
        for (int i = changeHistory.size() - 1; i >= 0; i--) {
            if (!changeHistory.get(i).reverted()) {
                return changeHistory.get(i);
            }
        }
        return null;
    }

    public void clearHistory() {
        changeHistory.clear();
    }

    // ------------------------------------------------------------------ serialization

    public JsonObject toJson() {
        JsonObject root = JsonValue.newObject();
        root.put("schemaVersion", schemaVersion);
        root.put("activeProfileId", activeProfileId);

        JsonObject smart = JsonValue.newObject();
        smart.put("enabled", smartAutoEnabled);
        smart.put("requireApproval", requireApproval);
        smart.put("autoRevertFailedChanges", autoRevertFailedChanges);
        smart.put("measureBeforeActing", measureBeforeActing);
        smart.put("measurementSeconds", measurementSeconds);
        smart.put("cooldownSeconds", cooldownSeconds);
        smart.put("dropThresholdPercent", dropThresholdPercent);
        smart.put("minFpsBeforeActing", minFpsBeforeActing);
        root.put("smartAuto", smart);

        JsonObject ui = JsonValue.newObject();
        ui.put("showOverlay", showOverlay);
        ui.put("showHistory", showHistory);
        ui.put("confirmDestructiveProfiles", confirmDestructiveProfiles);
        root.put("ui", ui);

        JsonObject profiles = JsonValue.newObject();
        for (Map.Entry<String, OptimizationProfile> entry : customProfiles.entrySet()) {
            profiles.put(entry.getKey(), profileToJson(entry.getValue()));
        }
        root.put("customProfiles", profiles);

        JsonObject snapshot = JsonValue.newObject();
        for (Map.Entry<Setting, String> entry : lastKnownGood.entries().entrySet()) {
            snapshot.put(entry.getKey().key(), entry.getValue());
        }
        root.put("lastKnownGood", snapshot);

        JsonArray history = JsonValue.newArray();
        for (SettingChange change : changeHistory) {
            history.add(changeToJson(change));
        }
        root.put("changeHistory", history);

        return root;
    }

    public static VortexConfig fromJson(JsonValue root) {
        VortexConfig config = new VortexConfig();
        if (root == null || !root.isObject()) {
            return config;
        }
        JsonObject object = root.asObject();
        config.schemaVersion = object.getOrNull("schemaVersion").asInt(SCHEMA_VERSION);
        config.activeProfileId(object.getOrNull("activeProfileId").asString(BuiltInProfiles.BALANCED));

        JsonObject smart = object.getOrNull("smartAuto").asObjectOrEmpty();
        config.smartAutoEnabled = smart.getOrNull("enabled").asBoolean(false);
        config.requireApproval = smart.getOrNull("requireApproval").asBoolean(true);
        config.autoRevertFailedChanges = smart.getOrNull("autoRevertFailedChanges").asBoolean(true);
        config.measureBeforeActing = smart.getOrNull("measureBeforeActing").asBoolean(true);
        config.measurementSeconds(smart.getOrNull("measurementSeconds").asInt(12));
        config.cooldownSeconds(smart.getOrNull("cooldownSeconds").asInt(120));
        config.dropThresholdPercent(smart.getOrNull("dropThresholdPercent").asDouble(15.0));
        config.minFpsBeforeActing(smart.getOrNull("minFpsBeforeActing").asInt(45));

        JsonObject ui = object.getOrNull("ui").asObjectOrEmpty();
        config.showOverlay = ui.getOrNull("showOverlay").asBoolean(true);
        config.showHistory = ui.getOrNull("showHistory").asBoolean(true);
        config.confirmDestructiveProfiles = ui.getOrNull("confirmDestructiveProfiles").asBoolean(true);

        JsonObject profiles = object.getOrNull("customProfiles").asObjectOrEmpty();
        for (Map.Entry<String, JsonValue> entry : profiles.entries().entrySet()) {
            OptimizationProfile profile = profileFromJson(entry.getKey(), entry.getValue());
            if (profile != null) {
                config.customProfiles.put(entry.getKey(), profile);
            }
        }

        JsonObject snapshot = object.getOrNull("lastKnownGood").asObjectOrEmpty();
        EnumMap<Setting, String> values = new EnumMap<>(Setting.class);
        for (Map.Entry<String, JsonValue> entry : snapshot.entries().entrySet()) {
            Setting setting = Setting.byKey(entry.getKey());
            if (setting != null && entry.getValue().isString()) {
                values.put(setting, entry.getValue().asString());
            }
        }
        config.lastKnownGood = SettingsSnapshot.of(values);

        JsonArray history = object.getOrNull("changeHistory").asArrayOrEmpty();
        for (JsonValue item : history.items()) {
            SettingChange change = changeFromJson(item);
            if (change != null) {
                config.changeHistory.add(change);
            }
        }
        while (config.changeHistory.size() > MAX_HISTORY) {
            config.changeHistory.remove(0);
        }

        return config;
    }

    public String toJsonText() {
        return Json.write(toJson(), true);
    }

    public static VortexConfig fromJsonText(String text) {
        if (text == null || text.isBlank()) {
            return new VortexConfig();
        }
        try {
            return fromJson(Json.parse(text));
        } catch (RuntimeException e) {
            return new VortexConfig();
        }
    }

    // ------------------------------------------------------------------ profile codec

    public static JsonObject profileToJson(OptimizationProfile profile) {
        JsonObject json = JsonValue.newObject();
        json.put("id", profile.id());
        json.put("displayName", profile.displayName());
        json.put("particles", profile.particles() == null ? null : profile.particles().name());
        json.put("clouds", profile.clouds() == null ? null : profile.clouds().name());
        if (profile.hasRenderDistance()) {
            json.put("renderDistance", profile.renderDistance());
        }
        if (profile.hasSimulationDistance()) {
            json.put("simulationDistance", profile.simulationDistance());
        }
        if (profile.hasEntityDistanceScaling()) {
            json.put("entityDistanceScaling", profile.entityDistanceScaling());
        }
        if (profile.hasMaxFps()) {
            json.put("maxFps", profile.maxFps());
        }
        if (profile.hasSmoothLighting()) {
            json.put("smoothLighting", profile.smoothLighting());
        }
        if (profile.hasVsync()) {
            json.put("vsync", profile.vsync());
        }
        if (profile.hasMipmapLevels()) {
            json.put("mipmapLevels", profile.mipmapLevels());
        }
        if (profile.hasBiomeBlendRadius()) {
            json.put("biomeBlendRadius", profile.biomeBlendRadius());
        }
        if (profile.hasGuiScale()) {
            json.put("guiScale", profile.guiScale());
        }
        if (profile.hasGamma()) {
            json.put("gamma", profile.gamma());
        }
        if (profile.hasCloudRenderDistance()) {
            json.put("cloudRenderDistance", profile.cloudRenderDistance());
        }
        if (profile.hasInactivityFpsLimit()) {
            json.put("inactivityFpsLimit", profile.inactivityFpsLimit());
        }
        return json;
    }

    public static OptimizationProfile profileFromJson(String fallbackId, JsonValue value) {
        if (value == null || !value.isObject()) {
            return null;
        }
        JsonObject json = value.asObjectOrEmpty();
        String id = json.getOrNull("id").asString(fallbackId);
        String displayName = json.getOrNull("displayName").asString(id);
        OptimizationProfile profile = new OptimizationProfile(id, displayName).builtIn(false);

        String particles = json.getOrNull("particles").asString(null);
        if (particles != null) {
            try {
                profile.particles(OptimizationProfile.ParticleLevel.valueOf(particles));
            } catch (IllegalArgumentException ignored) {
                // unknown value from a newer build: leave the setting untouched
            }
        }
        String clouds = json.getOrNull("clouds").asString(null);
        if (clouds != null) {
            try {
                profile.clouds(OptimizationProfile.CloudLevel.valueOf(clouds));
            } catch (IllegalArgumentException ignored) {
                // unknown value from a newer build
            }
        }
        if (json.has("renderDistance")) {
            profile.renderDistance(json.getOrNull("renderDistance").asInt(OptimizationProfile.UNSET));
        }
        if (json.has("simulationDistance")) {
            profile.simulationDistance(json.getOrNull("simulationDistance").asInt(OptimizationProfile.UNSET));
        }
        if (json.has("entityDistanceScaling")) {
            profile.entityDistanceScaling(json.getOrNull("entityDistanceScaling").asDouble(OptimizationProfile.UNSET_DOUBLE));
        }
        if (json.has("maxFps")) {
            profile.maxFps(json.getOrNull("maxFps").asInt(OptimizationProfile.UNSET));
        }
        if (json.has("smoothLighting")) {
            profile.smoothLighting(json.getOrNull("smoothLighting").asBoolean(true));
        }
        if (json.has("vsync")) {
            profile.vsync(json.getOrNull("vsync").asBoolean(false));
        }
        if (json.has("mipmapLevels")) {
            profile.mipmapLevels(json.getOrNull("mipmapLevels").asInt(OptimizationProfile.UNSET));
        }
        if (json.has("biomeBlendRadius")) {
            profile.biomeBlendRadius(json.getOrNull("biomeBlendRadius").asInt(OptimizationProfile.UNSET));
        }
        if (json.has("guiScale")) {
            profile.guiScale(json.getOrNull("guiScale").asInt(OptimizationProfile.UNSET));
        }
        if (json.has("gamma")) {
            profile.gamma(json.getOrNull("gamma").asDouble(OptimizationProfile.UNSET_DOUBLE));
        }
        if (json.has("cloudRenderDistance")) {
            profile.cloudRenderDistance(json.getOrNull("cloudRenderDistance").asInt(OptimizationProfile.UNSET));
        }
        if (json.has("inactivityFpsLimit")) {
            profile.inactivityFpsLimit(json.getOrNull("inactivityFpsLimit").asString(null));
        }
        return profile;
    }

    private static JsonObject changeToJson(SettingChange change) {
        JsonObject json = JsonValue.newObject();
        json.put("setting", change.setting().key());
        json.put("before", change.beforeValue());
        json.put("after", change.afterValue());
        json.put("source", change.source().name());
        json.put("timestamp", change.timestampMillis());
        json.put("reverted", change.reverted());
        return json;
    }

    private static SettingChange changeFromJson(JsonValue value) {
        if (value == null || !value.isObject()) {
            return null;
        }
        JsonObject json = value.asObjectOrEmpty();
        Setting setting = Setting.byKey(json.getOrNull("setting").asString(null));
        if (setting == null) {
            return null;
        }
        String sourceName = json.getOrNull("source").asString("MANUAL");
        SettingChange.Source source;
        try {
            source = SettingChange.Source.valueOf(sourceName);
        } catch (IllegalArgumentException e) {
            source = SettingChange.Source.MANUAL;
        }
        SettingChange change = new SettingChange(
                setting,
                json.getOrNull("before").asString(""),
                json.getOrNull("after").asString(""),
                source,
                json.getOrNull("timestamp").asLong(0L));
        if (json.getOrNull("reverted").asBoolean(false)) {
            change.markReverted();
        }
        return change;
    }
}
