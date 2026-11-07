package com.tech65pro.vortexmc.config;

import com.tech65pro.vortexmc.core.Setting;
import com.tech65pro.vortexmc.core.SettingChange;
import com.tech65pro.vortexmc.core.SettingsSnapshot;
import com.tech65pro.vortexmc.json.Json;
import com.tech65pro.vortexmc.json.JsonValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Configuration persistence and migration. */
class VortexConfigTest {

    @Test
    @DisplayName("a full configuration round-trips through text")
    void fullRoundTrip() {
        VortexConfig config = new VortexConfig();
        config.activeProfileId("low_end");
        config.smartAutoEnabled(true);
        config.requireApproval(true);
        config.autoRevertFailedChanges(false);
        config.measurementSeconds(20);
        config.cooldownSeconds(90);
        config.dropThresholdPercent(12.5);
        config.minFpsBeforeActing(60);
        config.showOverlay(false);
        config.showHistory(false);

        OptimizationProfile custom = new OptimizationProfile("mine", "My Profile")
                .particles(OptimizationProfile.ParticleLevel.MINIMAL)
                .clouds(OptimizationProfile.CloudLevel.OFF)
                .renderDistance(8)
                .simulationDistance(6)
                .entityDistanceScaling(0.5)
                .maxFps(90)
                .smoothLighting(false)
                .vsync(true)
                .mipmapLevels(1)
                .biomeBlendRadius(3)
                .cloudRenderDistance(12)
                .inactivityFpsLimit("AFK")
                .gamma(0.75);
        config.setCustomProfile(custom);

        config.lastKnownGood(new SettingsSnapshot()
                .put(Setting.RENDER_DISTANCE, "12")
                .put(Setting.MAX_FPS, "120"));
        config.recordChange(new SettingChange(Setting.RENDER_DISTANCE, "12", "8",
                SettingChange.Source.SMART_AUTO, 1234L));

        VortexConfig back = VortexConfig.fromJsonText(config.toJsonText());

        Assertions.assertEquals("low_end", back.activeProfileId());
        Assertions.assertTrue(back.smartAutoEnabled());
        Assertions.assertTrue(back.requireApproval());
        Assertions.assertFalse(back.autoRevertFailedChanges());
        Assertions.assertEquals(20, back.measurementSeconds());
        Assertions.assertEquals(90, back.cooldownSeconds());
        Assertions.assertEquals(12.5, back.dropThresholdPercent(), 0.0001d);
        Assertions.assertEquals(60, back.minFpsBeforeActing());
        Assertions.assertFalse(back.showOverlay());
        Assertions.assertFalse(back.showHistory());

        Assertions.assertTrue(back.customProfiles().containsKey("mine"));
        OptimizationProfile restored = back.customProfiles().get("mine");
        Assertions.assertEquals(OptimizationProfile.ParticleLevel.MINIMAL, restored.particles());
        Assertions.assertEquals(OptimizationProfile.CloudLevel.OFF, restored.clouds());
        Assertions.assertEquals(8, restored.renderDistance());
        Assertions.assertEquals(6, restored.simulationDistance());
        Assertions.assertEquals(0.5, restored.entityDistanceScaling(), 0.0001d);
        Assertions.assertEquals(90, restored.maxFps());
        Assertions.assertEquals(Boolean.FALSE, restored.smoothLighting());
        Assertions.assertEquals(Boolean.TRUE, restored.vsync());
        Assertions.assertEquals(1, restored.mipmapLevels());
        Assertions.assertEquals(3, restored.biomeBlendRadius());
        Assertions.assertEquals(12, restored.cloudRenderDistance());
        Assertions.assertEquals("AFK", restored.inactivityFpsLimit());
        Assertions.assertEquals(0.75, restored.gamma(), 0.0001d);
        Assertions.assertFalse(restored.builtIn());

        Assertions.assertEquals("12", back.lastKnownGood().get(Setting.RENDER_DISTANCE));
        Assertions.assertEquals("120", back.lastKnownGood().get(Setting.MAX_FPS));
        Assertions.assertEquals(1, back.changeHistory().size());
        Assertions.assertEquals(Setting.RENDER_DISTANCE, back.changeHistory().get(0).setting());
        Assertions.assertEquals(SettingChange.Source.SMART_AUTO, back.changeHistory().get(0).source());
    }

    @Test
    @DisplayName("unparseable text falls back to defaults instead of throwing")
    void brokenTextFallsBack() {
        VortexConfig config = VortexConfig.fromJsonText("{not json at all");
        Assertions.assertFalse(config.smartAutoEnabled());
        Assertions.assertEquals(BuiltInProfiles.BALANCED, config.activeProfileId());
    }

    @Test
    @DisplayName("an empty or null document is treated as absent")
    void emptyTextFallsBack() {
        Assertions.assertFalse(VortexConfig.fromJsonText(null).smartAutoEnabled());
        Assertions.assertFalse(VortexConfig.fromJsonText("   ").smartAutoEnabled());
        Assertions.assertFalse(VortexConfig.fromJson(null).smartAutoEnabled());
    }

    @Test
    @DisplayName("a value from a newer build is ignored rather than crashing the load")
    void unknownEnumValueIsIgnored() {
        JsonValue root = Json.parse("{\"customProfiles\":{\"x\":{\"id\":\"x\",\"particles\":\"SOMETHING_NEW\"}}}");
        VortexConfig config = VortexConfig.fromJson(root);
        Assertions.assertTrue(config.customProfiles().containsKey("x"));
        Assertions.assertNull(config.customProfiles().get("x").particles(),
                "an unrecognised value must leave the setting untouched");
    }

    @Test
    @DisplayName("numeric settings are clamped to sane ranges")
    void numbersAreClamped() {
        VortexConfig config = new VortexConfig();
        config.measurementSeconds(100000);
        config.cooldownSeconds(-5);
        config.dropThresholdPercent(1000d);
        config.minFpsBeforeActing(9999);
        Assertions.assertEquals(120, config.measurementSeconds());
        Assertions.assertEquals(30, config.cooldownSeconds());
        Assertions.assertEquals(60d, config.dropThresholdPercent(), 0.0001d);
        Assertions.assertEquals(240, config.minFpsBeforeActing());
    }

    @Test
    @DisplayName("deleting a custom profile falls back to Balanced")
    void deletingCustomProfileFallsBack() {
        VortexConfig config = new VortexConfig();
        config.setCustomProfile(new OptimizationProfile("mine", "Mine"));
        config.activeProfileId("mine");
        config.removeCustomProfile("mine");
        Assertions.assertEquals(BuiltInProfiles.BALANCED, config.activeProfileId());
    }

    @Test
    @DisplayName("built-in profiles cannot be deleted")
    void builtInsCannotBeDeleted() {
        VortexConfig config = new VortexConfig();
        config.activeProfileId(BuiltInProfiles.LOW_END);
        config.removeCustomProfile(BuiltInProfiles.LOW_END);
        Assertions.assertEquals(BuiltInProfiles.LOW_END, config.activeProfileId());
        Assertions.assertNotNull(config.profile(BuiltInProfiles.LOW_END));
    }

    @Test
    @DisplayName("the change history is bounded")
    void historyIsBounded() {
        VortexConfig config = new VortexConfig();
        for (int i = 0; i < 200; i++) {
            config.recordChange(new SettingChange(Setting.RENDER_DISTANCE, "a", "b",
                    SettingChange.Source.MANUAL, i));
        }
        Assertions.assertEquals(50, config.changeHistory().size(), "history must not grow without bound");
        Assertions.assertNotNull(config.lastRevertableChange());
        config.clearHistory();
        Assertions.assertTrue(config.changeHistory().isEmpty());
        Assertions.assertNull(config.lastRevertableChange());
    }

    @Test
    @DisplayName("the last known-good snapshot starts empty")
    void snapshotStartsEmpty() {
        VortexConfig config = new VortexConfig();
        Assertions.assertTrue(config.lastKnownGood().isEmpty());
        Assertions.assertEquals(0, config.lastKnownGood().size());
        Assertions.assertNull(config.lastKnownGood().get(Setting.RENDER_DISTANCE));
    }
}
