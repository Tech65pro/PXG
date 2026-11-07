package com.tech65pro.vortexmc.core;

import com.tech65pro.vortexmc.config.OptimizationProfile;

/**
 * Decides which single setting Smart Auto should propose next.
 *
 * <p>One change at a time, always the safest available one, and never when the game is already
 * running comfortably. Each suggestion carries the reason it is expected to reduce work, and
 * deliberately carries no promised frame-rate figure: the effect is measured afterwards by
 * {@link PerformanceAnalyzer} and only then reported.</p>
 */
public final class RecommendationEngine {

    private RecommendationEngine() {
    }

    /**
     * @param current            the settings as they are right now
     * @param averageFps         average frame rate over the recent window
     * @param onePercentLowFps   the 1% low, or a non-positive value when unknown
     * @param stuttering         whether the slowest frames are far below the median
     * @param minFpsBeforeActing do not propose anything above this frame rate
     * @return the suggestion, or {@code null} when nothing should be changed
     */
    public static Recommendation suggest(OptimizationProfile current,
                                         double averageFps,
                                         double onePercentLowFps,
                                         boolean stuttering,
                                         int minFpsBeforeActing) {
        if (current == null) {
            return null;
        }
        boolean dataAvailable = averageFps > 0d;
        boolean needsHelp = !dataAvailable || averageFps < minFpsBeforeActing || stuttering;
        if (!needsHelp) {
            return null;
        }
        boolean verySlow = dataAvailable && averageFps < minFpsBeforeActing * 0.6;
        boolean lowFrames = onePercentLowFps > 0d && onePercentLowFps < averageFps * 0.6;

        // Ordered from "least visible" to "most visible". The first one that applies wins, so the
        // player is never offered a drastic change when a subtle one would do.
        Recommendation candidate;

        if (OptimizationProfile.ParticleLevel.ALL == current.particles()) {
            candidate = new Recommendation(Setting.PARTICLE_LEVEL, "Reduce particles",
                    "Fewer particle effects means fewer draw calls per frame.",
                    "All", "Decreased", Recommendation.Risk.SAFE);
        } else if (current.hasBiomeBlendRadius() && current.biomeBlendRadius() > 2) {
            candidate = new Recommendation(Setting.BIOME_BLEND_RADIUS, "Reduce biome blend",
                    "Biome colour blending is recomputed when chunks load.",
                    String.valueOf(current.biomeBlendRadius()), "2", Recommendation.Risk.SAFE);
        } else if (current.hasMipmapLevels() && current.mipmapLevels() > 0) {
            candidate = new Recommendation(Setting.MIPMAP_LEVELS, "Lower mipmap levels",
                    "Mipmapping sharpens distant textures at the cost of memory bandwidth.",
                    String.valueOf(current.mipmapLevels()), "0", Recommendation.Risk.MODERATE);
        } else if (current.hasCloudRenderDistance() && current.cloudRenderDistance() > 8) {
            candidate = new Recommendation(Setting.CLOUD_RENDER_DISTANCE, "Shorten cloud distance",
                    "Clouds still render, but fewer layers are drawn.",
                    String.valueOf(current.cloudRenderDistance()), "8", Recommendation.Risk.SAFE);
        } else if (current.clouds() == OptimizationProfile.CloudLevel.FANCY) {
            candidate = new Recommendation(Setting.CLOUD_LEVEL, "Use fast clouds",
                    "Fast clouds draw far fewer quads than fancy clouds.",
                    "Fancy", "Fast", Recommendation.Risk.SAFE);
        } else if (current.hasEntityDistanceScaling() && current.entityDistanceScaling() > 0.5) {
            candidate = new Recommendation(Setting.ENTITY_DISTANCE_SCALING, "Shorten entity distance",
                    "Entities beyond the scaled distance stop being rendered and ticked.",
                    percent(current.entityDistanceScaling()), "50%", Recommendation.Risk.MODERATE);
        } else if (OptimizationProfile.ParticleLevel.DECREASED == current.particles()) {
            candidate = new Recommendation(Setting.PARTICLE_LEVEL, "Reduce particles further",
                    "The minimum setting removes most particle effects entirely.",
                    "Decreased", "Minimal", Recommendation.Risk.MODERATE);
        } else if (current.hasSmoothLighting() && Boolean.TRUE.equals(current.smoothLighting())) {
            candidate = new Recommendation(Setting.SMOOTH_LIGHTING, "Turn off smooth lighting",
                    "Smooth lighting is a per-vertex lighting calculation on every rebuilt chunk.",
                    "on", "off", Recommendation.Risk.NOTICEABLE);
        } else if (current.hasSimulationDistance() && current.simulationDistance() > 5) {
            candidate = new Recommendation(Setting.SIMULATION_DISTANCE, "Shorten simulation distance",
                    "Blocks, mobs and redstone beyond this distance stop ticking.",
                    String.valueOf(current.simulationDistance()), "5", Recommendation.Risk.NOTICEABLE);
        } else if (current.hasRenderDistance() && current.renderDistance() > 6) {
            candidate = new Recommendation(Setting.RENDER_DISTANCE, "Shorten render distance",
                    "Fewer chunks are loaded, so fewer chunk meshes are built and drawn.",
                    String.valueOf(current.renderDistance()), "6", Recommendation.Risk.NOTICEABLE);
        } else if (current.clouds() != OptimizationProfile.CloudLevel.OFF) {
            candidate = new Recommendation(Setting.CLOUD_LEVEL, "Turn clouds off",
                    "Clouds are a full-screen-ish draw that scales with view distance.",
                    current.clouds().name(), "Off", Recommendation.Risk.SAFE);
        } else {
            return null;
        }

        // When the game is struggling badly, skip ahead to the changes that actually move the
        // needle rather than offering the gentlest one first.
        if (verySlow && lowFrames && candidate.risk() == Recommendation.Risk.SAFE) {
            Recommendation aggressive = aggressiveSuggestion(current);
            if (aggressive != null) {
                return aggressive;
            }
        }
        return candidate;
    }

    private static Recommendation aggressiveSuggestion(OptimizationProfile current) {
        if (current.hasRenderDistance() && current.renderDistance() > 6) {
            return new Recommendation(Setting.RENDER_DISTANCE, "Shorten render distance",
                    "Fewer chunks are loaded, so fewer chunk meshes are built and drawn.",
                    String.valueOf(current.renderDistance()), "6", Recommendation.Risk.NOTICEABLE);
        }
        if (current.hasEntityDistanceScaling() && current.entityDistanceScaling() > 0.5) {
            return new Recommendation(Setting.ENTITY_DISTANCE_SCALING, "Shorten entity distance",
                    "Entities beyond the scaled distance stop being rendered and ticked.",
                    percent(current.entityDistanceScaling()), "50%", Recommendation.Risk.MODERATE);
        }
        return null;
    }

    private static String percent(double fraction) {
        return Math.round(fraction * 100.0) + "%";
    }
}
