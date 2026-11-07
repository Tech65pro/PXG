package com.tech65pro.vortexmc.config;

import com.tech65pro.vortexmc.config.OptimizationProfile.CloudLevel;
import com.tech65pro.vortexmc.config.OptimizationProfile.ParticleLevel;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The profiles VortexMC ships with.
 *
 * <p>These are intentionally conservative. VortexMC never claims a specific frame-rate gain:
 * every machine, driver and world is different, and the honest statement is that these
 * settings reduce the amount of work the renderer is asked to do. Whether that translates into
 * more frames is measured, not promised.</p>
 */
public final class BuiltInProfiles {

    public static final String LOW_END = "low_end";
    public static final String BALANCED = "balanced";
    public static final String HIGH_PERFORMANCE = "high_performance";

    private BuiltInProfiles() {
    }

    /** Conservative settings aimed at integrated GPUs such as Intel HD Graphics 4600. */
    public static OptimizationProfile lowEnd() {
        return new OptimizationProfile(LOW_END, "Low-End")
                .builtIn(true)
                .particles(ParticleLevel.MINIMAL)
                .clouds(CloudLevel.OFF)
                .renderDistance(6)
                .simulationDistance(5)
                .entityDistanceScaling(0.5)
                .smoothLighting(false)
                .vsync(false)
                .mipmapLevels(0)
                .biomeBlendRadius(2)
                .cloudRenderDistance(8);
    }

    /** The default: keeps the game looking normal while removing the most expensive extras. */
    public static OptimizationProfile balanced() {
        return new OptimizationProfile(BALANCED, "Balanced")
                .builtIn(true)
                .particles(ParticleLevel.DECREASED)
                .clouds(CloudLevel.FAST)
                .renderDistance(10)
                .simulationDistance(8)
                .entityDistanceScaling(0.75)
                .smoothLighting(true)
                .mipmapLevels(2)
                .biomeBlendRadius(5)
                .cloudRenderDistance(16);
    }

    /** For machines with a dedicated GPU and headroom to spare. */
    public static OptimizationProfile highPerformance() {
        return new OptimizationProfile(HIGH_PERFORMANCE, "High-Performance")
                .builtIn(true)
                .particles(ParticleLevel.ALL)
                .clouds(CloudLevel.FANCY)
                .renderDistance(16)
                .simulationDistance(12)
                .entityDistanceScaling(1.0)
                .smoothLighting(true)
                .mipmapLevels(4)
                .biomeBlendRadius(5)
                .cloudRenderDistance(32);
    }

    /** All built-in profiles, in display order. */
    public static Map<String, OptimizationProfile> defaults() {
        Map<String, OptimizationProfile> map = new LinkedHashMap<>();
        map.put(LOW_END, lowEnd());
        map.put(BALANCED, balanced());
        map.put(HIGH_PERFORMANCE, highPerformance());
        return map;
    }

    public static boolean isBuiltInId(String id) {
        return LOW_END.equals(id) || BALANCED.equals(id) || HIGH_PERFORMANCE.equals(id);
    }
}
