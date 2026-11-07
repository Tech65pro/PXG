package com.tech65pro.vortexmc.core;

/**
 * The graphics settings VortexMC knows how to read and write.
 *
 * <p>Each entry has a stable string key that is used in the configuration file and in the
 * change log, so a settings snapshot stays readable across mod versions even if the enum
 * order changes.</p>
 */
public enum Setting {

    RENDER_DISTANCE("render_distance", "Render Distance"),
    SIMULATION_DISTANCE("simulation_distance", "Simulation Distance"),
    PARTICLE_LEVEL("particle_level", "Particles"),
    CLOUD_LEVEL("cloud_level", "Clouds"),
    ENTITY_DISTANCE_SCALING("entity_distance_scaling", "Entity Distance"),
    MAX_FPS("max_fps", "Framerate Limit"),
    SMOOTH_LIGHTING("smooth_lighting", "Smooth Lighting"),
    VSYNC("vsync", "VSync"),
    MIPMAP_LEVELS("mipmap_levels", "Mipmap Levels"),
    BIOME_BLEND_RADIUS("biome_blend_radius", "Biome Blend"),
    CLOUD_RENDER_DISTANCE("cloud_render_distance", "Cloud Render Distance"),
    INACTIVITY_FPS_LIMIT("inactivity_fps_limit", "Inactivity Framerate Limit"),
    GUI_SCALE("gui_scale", "GUI Scale"),
    GAMMA("gamma", "Brightness");

    private final String key;
    private final String displayName;

    Setting(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public static Setting byKey(String key) {
        for (Setting setting : values()) {
            if (setting.key.equals(key)) {
                return setting;
            }
        }
        return null;
    }
}
