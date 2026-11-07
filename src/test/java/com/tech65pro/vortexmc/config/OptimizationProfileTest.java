package com.tech65pro.vortexmc.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The profile model. */
class OptimizationProfileTest {

    @Test
    @DisplayName("an unset field means 'do not touch', not 'zero'")
    void unsetMeansUntouched() {
        OptimizationProfile profile = new OptimizationProfile("x", "X");
        Assertions.assertFalse(profile.hasRenderDistance());
        Assertions.assertFalse(profile.hasSimulationDistance());
        Assertions.assertFalse(profile.hasEntityDistanceScaling());
        Assertions.assertFalse(profile.hasMaxFps());
        Assertions.assertFalse(profile.hasMipmapLevels());
        Assertions.assertFalse(profile.hasBiomeBlendRadius());
        Assertions.assertFalse(profile.hasGuiScale());
        Assertions.assertFalse(profile.hasGamma());
        Assertions.assertFalse(profile.hasCloudRenderDistance());
        Assertions.assertFalse(profile.hasInactivityFpsLimit());
        Assertions.assertFalse(profile.hasSmoothLighting());
        Assertions.assertFalse(profile.hasVsync());
        Assertions.assertEquals(0, profile.definedSettingCount());
    }

    @Test
    @DisplayName("setting a field flips it to defined")
    void settingAFieldDefinesIt() {
        OptimizationProfile profile = new OptimizationProfile("x", "X").renderDistance(8);
        Assertions.assertTrue(profile.hasRenderDistance());
        Assertions.assertEquals(8, profile.renderDistance());
        Assertions.assertEquals(1, profile.definedSettingCount());
    }

    @Test
    @DisplayName("copy is independent of the original")
    void copyIsIndependent() {
        OptimizationProfile original = new OptimizationProfile("x", "X").renderDistance(8);
        OptimizationProfile copy = original.copy();
        copy.renderDistance(16);
        Assertions.assertEquals(8, original.renderDistance());
        Assertions.assertEquals(16, copy.renderDistance());
        Assertions.assertEquals(original.id(), copy.id());
    }

    @Test
    @DisplayName("the Low-End profile is conservative")
    void lowEndIsConservative() {
        OptimizationProfile lowEnd = BuiltInProfiles.lowEnd();
        Assertions.assertEquals(OptimizationProfile.ParticleLevel.MINIMAL, lowEnd.particles());
        Assertions.assertEquals(OptimizationProfile.CloudLevel.OFF, lowEnd.clouds());
        Assertions.assertEquals(6, lowEnd.renderDistance());
        Assertions.assertEquals(5, lowEnd.simulationDistance());
        Assertions.assertEquals(0.5, lowEnd.entityDistanceScaling(), 0.0001d);
        Assertions.assertEquals(Boolean.FALSE, lowEnd.smoothLighting());
        Assertions.assertEquals(0, lowEnd.mipmapLevels());
        Assertions.assertTrue(lowEnd.builtIn());
        Assertions.assertTrue(lowEnd.definedSettingCount() >= 10,
                "the Low-End profile must actually define its settings");
    }

    @Test
    @DisplayName("the High-Performance profile is the most permissive of the three")
    void highPerformanceIsMostPermissive() {
        OptimizationProfile lowEnd = BuiltInProfiles.lowEnd();
        OptimizationProfile balanced = BuiltInProfiles.balanced();
        OptimizationProfile high = BuiltInProfiles.highPerformance();
        Assertions.assertTrue(high.renderDistance() > balanced.renderDistance());
        Assertions.assertTrue(balanced.renderDistance() > lowEnd.renderDistance());
        Assertions.assertTrue(high.simulationDistance() > lowEnd.simulationDistance());
        Assertions.assertTrue(high.entityDistanceScaling() > lowEnd.entityDistanceScaling());
    }

    @Test
    @DisplayName("every built-in profile has a stable id and a display name")
    void builtInsAreWellFormed() {
        for (OptimizationProfile profile : BuiltInProfiles.defaults().values()) {
            Assertions.assertNotNull(profile.id());
            Assertions.assertFalse(profile.id().isBlank());
            Assertions.assertNotNull(profile.displayName());
            Assertions.assertFalse(profile.displayName().isBlank());
            Assertions.assertTrue(profile.builtIn());
            Assertions.assertTrue(BuiltInProfiles.isBuiltInId(profile.id()));
        }
        Assertions.assertFalse(BuiltInProfiles.isBuiltInId("something_else"));
    }
}
