package com.tech65pro.vortexmc.client;

import com.tech65pro.vortexmc.config.OptimizationProfile;
import com.tech65pro.vortexmc.core.Setting;
import com.tech65pro.vortexmc.core.SettingsSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.InactivityFpsLimit;
import net.minecraft.particle.ParticlesMode;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Translates between Minecraft's live {@link GameOptions} and VortexMC's setting model.
 *
 * <p>Every access goes through this one class, which keeps the rest of the mod free of
 * Minecraft types and gives a single place to clamp or reject a value that a hand-edited
 * configuration file asked for.</p>
 *
 * <p>Enum constants are resolved by name rather than referenced directly because Yarn's Enigma
 * mappings do not include enum constants, so they cannot be verified statically.</p>
 */
public final class SettingMapper {

    /** Hard limits applied to values coming from configuration files. */
    private static final int MIN_RENDER_DISTANCE = 2;
    private static final int MAX_RENDER_DISTANCE = 32;
    private static final int MIN_SIMULATION_DISTANCE = 2;
    private static final int MAX_SIMULATION_DISTANCE = 32;
    private static final int MIN_MAX_FPS = 10;
    private static final int MAX_MAX_FPS = 260;
    private static final int MIN_MIPMAP = 0;
    private static final int MAX_MIPMAP = 4;
    private static final int MIN_BIOME_BLEND = 0;
    private static final int MAX_BIOME_BLEND = 7;
    private static final int MIN_CLOUD_DISTANCE = 2;
    private static final int MAX_CLOUD_DISTANCE = 64;
    /**
     * The real option is not a number: {@code InactivityFpsLimit} is an enum describing what the
     * game does when the player is idle or the window is minimised. Values are matched
     * case-insensitively and anything unrecognised falls back to the vanilla default.
     */
    private static InactivityFpsLimit parseInactivityFpsLimit(String value) {
        if (value == null) {
            return InactivityFpsLimit.MINIMIZED;
        }
        try {
            return InactivityFpsLimit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return InactivityFpsLimit.MINIMIZED;
        }
    }

    private static final double MIN_ENTITY_SCALING = 0.5;
    private static final double MAX_ENTITY_SCALING = 1.0;
    private static final double MIN_GAMMA = 0.0;
    private static final double MAX_GAMMA = 1.0;

    private final MinecraftClient client;

    public SettingMapper(MinecraftClient client) {
        this.client = client;
    }

    private GameOptions options() {
        GameOptions options = client == null ? null : client.options;
        return options == null ? MinecraftClient.getInstance().options : options;
    }

    /** True when the game is far enough through startup that options are usable. */
    public boolean isAvailable() {
        return options() != null;
    }

    /**
     * Reads a setting as a display string. Returns {@code null} when the value cannot be read
     * (for example before the options object exists), which callers treat as "unknown" rather
     * than as zero.
     */
    public String read(Setting setting) {
        GameOptions options = options();
        if (options == null) {
            return null;
        }
        try {
            switch (setting) {
                case RENDER_DISTANCE:
                    return String.valueOf(options.getViewDistance().getValue());
                case SIMULATION_DISTANCE:
                    return String.valueOf(options.getSimulationDistance().getValue());
                case ENTITY_DISTANCE_SCALING:
                    return String.format("%.0f%%", options.getEntityDistanceScaling().getValue() * 100.0);
                case MAX_FPS:
                    return formatMaxFps(options.getMaxFps().getValue());
                case CLOUD_LEVEL:
                    return nameOf(options.getCloudRenderModeValue());
                case CLOUD_RENDER_DISTANCE:
                    return String.valueOf(options.getCloudRenderDistance().getValue());
                case SMOOTH_LIGHTING:
                    return Boolean.TRUE.equals(options.getAo().getValue()) ? "on" : "off";
                case VSYNC:
                    return Boolean.TRUE.equals(options.getEnableVsync().getValue()) ? "on" : "off";
                case MIPMAP_LEVELS:
                    return String.valueOf(options.getMipmapLevels().getValue());
                case BIOME_BLEND_RADIUS:
                    return String.valueOf(options.getBiomeBlendRadius().getValue());
                case INACTIVITY_FPS_LIMIT:
                    return options.getInactivityFpsLimit().getValue().asString();
                case GUI_SCALE:
                    return formatGuiScale(options.getGuiScale().getValue());
                case GAMMA:
                    return String.format("%.0f%%", options.getGamma().getValue() * 100.0);
                case PARTICLE_LEVEL:
                    return nameOf(options.getParticles().getValue());
                default:
                    return null;
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Applies a value. Values are clamped to the same ranges the video settings screen uses, so
     * a bad value in the configuration file cannot lock the game into an unusable state.
     *
     * @return true when the value was applied
     */
    public boolean write(Setting setting, String value) {
        GameOptions options = options();
        if (options == null || value == null) {
            return false;
        }
        try {
            switch (setting) {
                case RENDER_DISTANCE:
                    options.getViewDistance().setValue(clampInt(value, MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE, 8));
                    return true;
                case SIMULATION_DISTANCE:
                    options.getSimulationDistance().setValue(clampInt(value, MIN_SIMULATION_DISTANCE, MAX_SIMULATION_DISTANCE, 8));
                    return true;
                case ENTITY_DISTANCE_SCALING:
                    options.getEntityDistanceScaling().setValue(clampDouble(value, MIN_ENTITY_SCALING, MAX_ENTITY_SCALING, 1.0));
                    return true;
                case MAX_FPS:
                    options.getMaxFps().setValue(clampInt(value, MIN_MAX_FPS, MAX_MAX_FPS, 60));
                    return true;
                case CLOUD_LEVEL:
                    options.getCloudRenderMode().setValue(cloudMode(value));
                    return true;
                case CLOUD_RENDER_DISTANCE:
                    options.getCloudRenderDistance().setValue(clampInt(value, MIN_CLOUD_DISTANCE, MAX_CLOUD_DISTANCE, 16));
                    return true;
                case SMOOTH_LIGHTING:
                    options.getAo().setValue(Boolean.parseBoolean(value.trim()));
                    return true;
                case VSYNC:
                    options.getEnableVsync().setValue(Boolean.parseBoolean(value.trim()));
                    return true;
                case MIPMAP_LEVELS:
                    options.getMipmapLevels().setValue(clampInt(value, MIN_MIPMAP, MAX_MIPMAP, 0));
                    return true;
                case BIOME_BLEND_RADIUS:
                    options.getBiomeBlendRadius().setValue(clampInt(value, MIN_BIOME_BLEND, MAX_BIOME_BLEND, 5));
                    return true;
                case INACTIVITY_FPS_LIMIT:
                    options.getInactivityFpsLimit().setValue(parseInactivityFpsLimit(value));
                    return true;
                case GUI_SCALE:
                    options.getGuiScale().setValue(clampInt(value, 0, 4, 0));
                    return true;
                case GAMMA:
                    options.getGamma().setValue(clampDouble(value, MIN_GAMMA, MAX_GAMMA, 0.5));
                    return true;
                case PARTICLE_LEVEL:
                    options.getParticles().setValue(particlesMode(value));
                    return true;
                default:
                    return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Captures every setting VortexMC can write, so it can be restored later. */
    public SettingsSnapshot snapshot() {
        Map<Setting, String> values = new EnumMap<>(Setting.class);
        for (Setting setting : Setting.values()) {
            String value = read(setting);
            if (value != null) {
                values.put(setting, value);
            }
        }
        return SettingsSnapshot.of(values);
    }

    /** Writes a previously captured snapshot back to the game. */
    public int restore(SettingsSnapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }
        int applied = 0;
        for (Map.Entry<Setting, String> entry : snapshot.entries().entrySet()) {
            if (write(entry.getKey(), entry.getValue())) {
                applied++;
            }
        }
        return applied;
    }

    /**
     * Applies the settings a profile defines. Settings the profile leaves unset are not touched.
     *
     * @return the changes that were actually made
     */
    public java.util.List<SettingChangeRecord> applyProfile(OptimizationProfile profile) {
        java.util.List<SettingChangeRecord> applied = new java.util.ArrayList<>();
        if (profile == null) {
            return applied;
        }
        applyIfDifferent(applied, Setting.PARTICLE_LEVEL, profile.particles() == null ? null : profile.particles().name());
        applyIfDifferent(applied, Setting.CLOUD_LEVEL, profile.clouds() == null ? null : profile.clouds().name());
        applyIfDifferent(applied, Setting.RENDER_DISTANCE, profile.hasRenderDistance() ? String.valueOf(profile.renderDistance()) : null);
        applyIfDifferent(applied, Setting.SIMULATION_DISTANCE, profile.hasSimulationDistance() ? String.valueOf(profile.simulationDistance()) : null);
        applyIfDifferent(applied, Setting.ENTITY_DISTANCE_SCALING, profile.hasEntityDistanceScaling() ? scaleToPercent(profile.entityDistanceScaling()) : null);
        applyIfDifferent(applied, Setting.MAX_FPS, profile.hasMaxFps() ? String.valueOf(profile.maxFps()) : null);
        applyIfDifferent(applied, Setting.CLOUD_RENDER_DISTANCE, profile.hasCloudRenderDistance() ? String.valueOf(profile.cloudRenderDistance()) : null);
        applyIfDifferent(applied, Setting.SMOOTH_LIGHTING, profile.hasSmoothLighting() ? (profile.smoothLighting() ? "on" : "off") : null);
        applyIfDifferent(applied, Setting.VSYNC, profile.hasVsync() ? (profile.vsync() ? "on" : "off") : null);
        applyIfDifferent(applied, Setting.MIPMAP_LEVELS, profile.hasMipmapLevels() ? String.valueOf(profile.mipmapLevels()) : null);
        applyIfDifferent(applied, Setting.BIOME_BLEND_RADIUS, profile.hasBiomeBlendRadius() ? String.valueOf(profile.biomeBlendRadius()) : null);
        applyIfDifferent(applied, Setting.INACTIVITY_FPS_LIMIT, profile.hasInactivityFpsLimit() ? profile.inactivityFpsLimit() : null);
        applyIfDifferent(applied, Setting.GUI_SCALE, profile.hasGuiScale() ? String.valueOf(profile.guiScale()) : null);
        applyIfDifferent(applied, Setting.GAMMA, profile.hasGamma() ? percentToRaw(profile.gamma()) : null);
        return applied;
    }

    private void applyIfDifferent(java.util.List<SettingChangeRecord> applied, Setting setting, String target) {
        if (target == null) {
            return;
        }
        String current = read(setting);
        if (target.equals(current)) {
            return;
        }
        if (write(setting, target)) {
            applied.add(new SettingChangeRecord(setting, current, read(setting)));
        }
    }

    /**
     * Builds a profile describing the live settings, so the recommendation engine can be given
     * the same object shape it is unit-tested with.
     */
    public OptimizationProfile currentAsProfile() {
        OptimizationProfile profile = new OptimizationProfile("current", "Current settings").builtIn(false);
        profile.particles(particlesFrom(read(Setting.PARTICLE_LEVEL)));
        profile.clouds(cloudsFrom(read(Setting.CLOUD_LEVEL)));
        profile.renderDistance(intFrom(read(Setting.RENDER_DISTANCE), OptimizationProfile.UNSET));
        profile.simulationDistance(intFrom(read(Setting.SIMULATION_DISTANCE), OptimizationProfile.UNSET));
        profile.entityDistanceScaling(scaleFrom(read(Setting.ENTITY_DISTANCE_SCALING)));
        profile.maxFps(intFrom(read(Setting.MAX_FPS), OptimizationProfile.UNSET));
        profile.cloudRenderDistance(intFrom(read(Setting.CLOUD_RENDER_DISTANCE), OptimizationProfile.UNSET));
        profile.smoothLighting(booleanFrom(read(Setting.SMOOTH_LIGHTING)));
        profile.vsync(booleanFrom(read(Setting.VSYNC)));
        profile.mipmapLevels(intFrom(read(Setting.MIPMAP_LEVELS), OptimizationProfile.UNSET));
        profile.biomeBlendRadius(intFrom(read(Setting.BIOME_BLEND_RADIUS), OptimizationProfile.UNSET));
        profile.inactivityFpsLimit(read(Setting.INACTIVITY_FPS_LIMIT));
        profile.gamma(scaleFrom(read(Setting.GAMMA)));
        return profile;
    }

    private static OptimizationProfile.ParticleLevel particlesFrom(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OptimizationProfile.ParticleLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static OptimizationProfile.CloudLevel cloudsFrom(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OptimizationProfile.CloudLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int intFrom(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Boolean booleanFrom(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().equalsIgnoreCase("on") || value.trim().equalsIgnoreCase("true");
    }

    private static double scaleFrom(String value) {
        if (value == null) {
            return OptimizationProfile.UNSET_DOUBLE;
        }
        try {
            String trimmed = value.trim().replace("%", "");
            double parsed = Double.parseDouble(trimmed);
            // Both entity distance and gamma are stored as percentages when read back.
            return parsed > 1.0 ? parsed / 100.0 : parsed;
        } catch (NumberFormatException e) {
            return OptimizationProfile.UNSET_DOUBLE;
        }
    }

    /** Persists the option file so changes survive a restart. */
    public boolean saveOptions() {
        GameOptions options = options();
        if (options == null) {
            return false;
        }
        try {
            options.write();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String nameOf(Object enumValue) {
        return enumValue == null ? "unknown" : enumValue.toString();
    }

    private static String formatMaxFps(Integer value) {
        if (value == null) {
            return "unknown";
        }
        return value >= 260 ? "unlimited" : value + " fps";
    }

    private static String formatGuiScale(Integer value) {
        if (value == null) {
            return "unknown";
        }
        return value <= 0 ? "auto" : String.valueOf(value);
    }

    private static String scaleToPercent(double scaling) {
        return String.format("%.0f%%", scaling * 100.0);
    }

    private static String percentToRaw(double fraction) {
        return String.format("%.3f", fraction);
    }

    private static ParticlesMode particlesMode(String value) {
        String name = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (name.equals("OFF") || name.equals("NONE")) {
            name = "MINIMAL";
        }
        try {
            return ParticlesMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return ParticlesMode.ALL;
        }
    }

    private static CloudRenderMode cloudMode(String value) {
        String name = value.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            return CloudRenderMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return CloudRenderMode.FANCY;
        }
    }

    private static int clampInt(String value, int min, int max, int fallback) {
        try {
            long parsed = Math.round(Double.parseDouble(value.trim()));
            if (parsed < min) {
                return min;
            }
            if (parsed > max) {
                return max;
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double clampDouble(String value, double min, double max, double fallback) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (parsed < min) {
                return min;
            }
            if (parsed > max) {
                return max;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
