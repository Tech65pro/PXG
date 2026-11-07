package com.tech65pro.vortexmc.client.screen.tabs;

import com.tech65pro.vortexmc.client.SettingMapper;
import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.client.VortexServices;
import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import com.tech65pro.vortexmc.client.screen.DashboardTab;
import com.tech65pro.vortexmc.client.screen.Ui;
import com.tech65pro.vortexmc.config.BuiltInProfiles;
import com.tech65pro.vortexmc.config.OptimizationProfile;
import com.tech65pro.vortexmc.config.VortexConfig;
import com.tech65pro.vortexmc.core.Setting;
import com.tech65pro.vortexmc.core.SettingsSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Built-in and custom optimisation profiles. */
public final class ProfilesTab implements DashboardTab {

    private static final int ROW = 30;
    private static final int PANEL_HEIGHT = 66;

    private TextFieldWidget nameField;
    private int detailsY;

    @Override
    public String title() {
        return "Profiles";
    }

    @Override
    public void build(DashboardScreen screen) {
        VortexServices services = screen.services();
        VortexConfig config = services.config;
        int width = screen.contentWidth();
        int inner = width - 16;

        // ---------------------------------------------------------- save current settings
        screen.addSectionHeader(0, 0, inner, "Save the current settings as a profile");
        nameField = new TextFieldWidget(screen.font(), 0, 20, inner, 16, Text.literal("Profile name"));
        nameField.setMaxLength(48);
        nameField.setText(nextCustomName(services));
        screen.addChild(nameField);
        screen.addButton(0, 42, inner, 16, "Save as custom profile", b -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                return;
            }
            OptimizationProfile profile = services.settingMapper.currentAsProfile();
            profile.id(slug(name));
            profile.displayName(name);
            profile.builtIn(false);
            config.setCustomProfile(profile);
            config.activeProfileId(profile.id());
            services.markDirty();
            services.save();
            screen.rebuild();
        });

        int y = 66;

        // ---------------------------------------------------------- profile list
        screen.addSectionHeader(0, y, inner, "Available profiles");
        y += 19;

        Map<String, OptimizationProfile> all = new LinkedHashMap<>(BuiltInProfiles.defaults());
        all.putAll(config.customProfiles());
        String activeId = config.activeProfileId();

        for (Map.Entry<String, OptimizationProfile> entry : all.entrySet()) {
            OptimizationProfile profile = entry.getValue();
            boolean active = entry.getKey().equals(activeId);
            int half = (inner - Theme.GAP) / 2;

            screen.addButton(0, y, half, 16,
                    (active ? "> " : "") + profile.displayName(), b -> {
                        config.activeProfileId(entry.getKey());
                        services.markDirty();
                        services.save();
                        screen.rebuild();
                    });

            if (profile.builtIn()) {
                screen.addButton(half + Theme.GAP, y, half, 16, "Apply to game", b -> {
                    SettingsSnapshot before = services.settingMapper.snapshot();
                    services.settingMapper.applyProfile(profile);
                    services.settingMapper.saveOptions();
                    config.lastKnownGood(before);
                    services.markDirty();
                    services.save();
                    screen.rebuild();
                });
            } else {
                screen.addButton(half + Theme.GAP, y, half, 16, "Apply and delete", b -> {
                    SettingsSnapshot before = services.settingMapper.snapshot();
                    services.settingMapper.applyProfile(profile);
                    services.settingMapper.saveOptions();
                    config.lastKnownGood(before);
                    config.removeCustomProfile(entry.getKey());
                    services.markDirty();
                    services.save();
                    screen.rebuild();
                });
            }
            y += ROW;
        }

        detailsY = y + 6;
        screen.reportContentHeight(detailsY + 200);
    }

    @Override
    public void render(DashboardScreen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        VortexServices services = screen.services();
        TextRenderer font = screen.font();
        int width = screen.contentWidth();
        int inner = width - 16;

        OptimizationProfile active = services.config.activeProfile();
        int panelHeight = PANEL_HEIGHT;
        Ui.panelWithTitle(context, font, Text.literal("Selected profile: " + active.displayName()),
                0, detailsY, width, panelHeight);

        int rowY = detailsY + 22;
        int half = inner / 2 - 4;
        List<String> lines = describe(active);
        int column = 0;
        for (String line : lines) {
            int x = column == 0 ? 8 : 8 + half + 8;
            context.drawTextWithShadow(font, Text.literal(Ui.clip(font, line, half)), x, rowY, Theme.TEXT_DIM);
            column++;
            if (column >= 2) {
                column = 0;
                rowY += 11;
            }
        }
        if (column != 0) {
            rowY += 11;
        }

        String note = BuiltInProfiles.isBuiltInId(active.id())
                ? "Built-in profiles are fixed. Saving the current settings creates a separate custom "
                  + "profile, so the built-in ones always stay as shipped."
                : "Custom profiles are stored in vortexmc.json. Applying one changes the game options "
                  + "immediately and persists them; the previous values are kept in the last "
                  + "known-good snapshot so they can be restored from the Graphics tab.";
        rowY = Ui.wrapped(context, font, note, 8, rowY + 2, inner, Theme.TEXT_FAINT, 10);

        screen.reportContentHeight(Math.max(detailsY + panelHeight, rowY) + 6);
    }

    private static List<String> describe(OptimizationProfile profile) {
        List<String> lines = new ArrayList<>();
        if (profile.particles() != null) {
            lines.add("Particles: " + profile.particles().name());
        }
        if (profile.clouds() != null) {
            lines.add("Clouds: " + profile.clouds().name());
        }
        if (profile.hasRenderDistance()) {
            lines.add("Render distance: " + profile.renderDistance());
        }
        if (profile.hasSimulationDistance()) {
            lines.add("Simulation distance: " + profile.simulationDistance());
        }
        if (profile.hasEntityDistanceScaling()) {
            lines.add("Entity distance: " + Math.round(profile.entityDistanceScaling() * 100) + "%");
        }
        if (profile.hasMaxFps()) {
            lines.add("Framerate limit: " + profile.maxFps());
        }
        if (profile.hasCloudRenderDistance()) {
            lines.add("Cloud distance: " + profile.cloudRenderDistance());
        }
        if (profile.hasSmoothLighting()) {
            lines.add("Smooth lighting: " + (profile.smoothLighting() ? "on" : "off"));
        }
        if (profile.hasVsync()) {
            lines.add("VSync: " + (profile.vsync() ? "on" : "off"));
        }
        if (profile.hasMipmapLevels()) {
            lines.add("Mipmap levels: " + profile.mipmapLevels());
        }
        if (profile.hasBiomeBlendRadius()) {
            lines.add("Biome blend: " + profile.biomeBlendRadius());
        }
        if (profile.hasInactivityFpsLimit()) {
            lines.add("Inactivity limit: " + profile.inactivityFpsLimit());
        }
        if (profile.hasGuiScale()) {
            lines.add("GUI scale: " + profile.guiScale());
        }
        if (profile.hasGamma()) {
            lines.add("Brightness: " + Math.round(profile.gamma() * 100) + "%");
        }
        if (lines.isEmpty()) {
            lines.add("This profile does not define any setting.");
        }
        return lines;
    }

    private static String nextCustomName(VortexServices services) {
        int index = 1;
        while (services.config.customProfiles().containsKey(slug("Custom " + index))) {
            index++;
        }
        return "Custom " + index;
    }

    private static String slug(String name) {
        StringBuilder builder = new StringBuilder();
        for (char c : name.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            } else if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '_') {
                builder.append('_');
            }
        }
        String result = builder.toString().replaceAll("_+$", "");
        return result.isEmpty() ? "custom" : result;
    }
}
