package com.tech65pro.vortexmc.core;

import com.tech65pro.vortexmc.config.OptimizationProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Smart Auto's suggestion rules. */
class RecommendationEngineTest {

    private static OptimizationProfile comfortable() {
        return new OptimizationProfile("t", "Test")
                .particles(OptimizationProfile.ParticleLevel.MINIMAL)
                .clouds(OptimizationProfile.CloudLevel.OFF)
                .renderDistance(6)
                .simulationDistance(5)
                .entityDistanceScaling(0.5)
                .smoothLighting(false)
                .mipmapLevels(0)
                .biomeBlendRadius(2)
                .cloudRenderDistance(8);
    }

    @Test
    @DisplayName("nothing is proposed when the game is already comfortable")
    void nothingWhenComfortable() {
        Assertions.assertNull(RecommendationEngine.suggest(comfortable(), 120d, 100d, false, 45));
    }

    @Test
    @DisplayName("nothing is proposed when every setting is already conservative")
    void nothingWhenExhausted() {
        Assertions.assertNull(RecommendationEngine.suggest(comfortable(), 12d, 8d, true, 45));
    }

    @Test
    @DisplayName("the gentlest applicable change is proposed first")
    void gentlestFirst() {
        OptimizationProfile profile = new OptimizationProfile("t", "Test")
                .particles(OptimizationProfile.ParticleLevel.ALL)
                .clouds(OptimizationProfile.CloudLevel.FANCY)
                .renderDistance(24)
                .simulationDistance(16)
                .entityDistanceScaling(1.0)
                .smoothLighting(true)
                .mipmapLevels(4)
                .biomeBlendRadius(7)
                .cloudRenderDistance(32);
        Recommendation suggestion = RecommendationEngine.suggest(profile, 20d, 12d, true, 45);
        Assertions.assertNotNull(suggestion);
        Assertions.assertEquals(Setting.PARTICLE_LEVEL, suggestion.setting());
        Assertions.assertEquals(Recommendation.Risk.SAFE, suggestion.risk());
        Assertions.assertEquals("All", suggestion.currentValue());
        Assertions.assertEquals("Decreased", suggestion.proposedValue());
    }

    @Test
    @DisplayName("no recommendation promises a frame-rate figure")
    void noPromisedFrameRate() {
        OptimizationProfile profile = new OptimizationProfile("t", "Test")
                .particles(OptimizationProfile.ParticleLevel.ALL)
                .clouds(OptimizationProfile.CloudLevel.FANCY)
                .renderDistance(24)
                .simulationDistance(16)
                .entityDistanceScaling(1.0)
                .smoothLighting(true)
                .mipmapLevels(4)
                .biomeBlendRadius(7)
                .cloudRenderDistance(32);
        Recommendation suggestion = RecommendationEngine.suggest(profile, 15d, 6d, true, 45);
        Assertions.assertNotNull(suggestion);
        Assertions.assertFalse(suggestion.reason().matches(".*\\d+\\s*fps.*"),
                "a recommendation must describe the work removed, not a promised frame rate");
        Assertions.assertFalse(suggestion.title().matches(".*\\+\\d+.*"));
    }

    @Test
    @DisplayName("a null profile is handled without throwing")
    void nullProfileIsSafe() {
        Assertions.assertNull(RecommendationEngine.suggest(null, 10d, 5d, true, 45));
    }

    @Test
    @DisplayName("a very slow machine skips ahead to the changes that matter")
    void verySlowSkipsAhead() {
        OptimizationProfile profile = new OptimizationProfile("t", "Test")
                .particles(OptimizationProfile.ParticleLevel.ALL)
                .clouds(OptimizationProfile.CloudLevel.FANCY)
                .renderDistance(24)
                .simulationDistance(16)
                .entityDistanceScaling(1.0)
                .smoothLighting(true)
                .mipmapLevels(4)
                .biomeBlendRadius(7)
                .cloudRenderDistance(32);
        // averageFps well under 60% of the target, with the 1% low far below it
        Recommendation suggestion = RecommendationEngine.suggest(profile, 12d, 5d, true, 45);
        Assertions.assertNotNull(suggestion);
        Assertions.assertEquals(Setting.RENDER_DISTANCE, suggestion.setting(),
                "when the machine is really struggling, render distance is the honest lever");
    }
}
