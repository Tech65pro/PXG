package com.tech65pro.vortexmc.client.screen.tabs;

import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.client.VortexServices;
import com.tech65pro.vortexmc.client.SettingMapper;
import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import com.tech65pro.vortexmc.client.screen.DashboardTab;
import com.tech65pro.vortexmc.client.screen.Ui;
import com.tech65pro.vortexmc.config.OptimizationProfile;
import com.tech65pro.vortexmc.core.Setting;
import com.tech65pro.vortexmc.core.SettingsSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/**
 * Manual control of every graphics setting VortexMC supports.
 *
 * <p>Each control reads the live value from the game, so the tab always shows what is actually
 * configured rather than what VortexMC last wrote. Nothing is applied until the player moves it.</p>
 */
public final class GraphicsTab implements DashboardTab {

    private static final int ROW = 26;

    @Override
    public String title() {
        return "Graphics";
    }

    @Override
    public void build(DashboardScreen screen) {
        VortexServices services = screen.services();
        SettingMapper mapper = services.settingMapper;
        int width = screen.contentWidth();
        int sliderWidth = width - 16;
        int y = 0;

        // ------------------------------------------------------------ distances
        y = section(screen, "Chunk and entity distances", y);
        y = slider(screen, mapper, y, sliderWidth, Setting.RENDER_DISTANCE,
                "Render distance", 2, 32, Setting.RENDER_DISTANCE.displayName());
        y = slider(screen, mapper, y, sliderWidth, Setting.SIMULATION_DISTANCE,
                "Simulation distance", 2, 32, "chunks");
        y = percentSlider(screen, mapper, y, sliderWidth, Setting.ENTITY_DISTANCE_SCALING,
                "Entity distance", 50, 100);
        y = slider(screen, mapper, y, sliderWidth, Setting.CLOUD_RENDER_DISTANCE,
                "Cloud render distance", 2, 64, "chunks");

        // ------------------------------------------------------------ appearance
        y = section(screen, "Appearance", y);
        y = cycle(screen, mapper, y, sliderWidth, Setting.PARTICLE_LEVEL,
                "Particles", "ALL", "DECREASED", "MINIMAL");
        y = cycle(screen, mapper, y, sliderWidth, Setting.CLOUD_LEVEL,
                "Clouds", "OFF", "FAST", "FANCY");
        y = toggle(screen, mapper, y, sliderWidth, Setting.SMOOTH_LIGHTING, "Smooth lighting");
        y = toggle(screen, mapper, y, sliderWidth, Setting.VSYNC, "VSync");
        y = slider(screen, mapper, y, sliderWidth, Setting.MIPMAP_LEVELS,
                "Mipmap levels", 0, 4, "levels");
        y = slider(screen, mapper, y, sliderWidth, Setting.BIOME_BLEND_RADIUS,
                "Biome blend radius", 0, 7, "blocks");
        y = slider(screen, mapper, y, sliderWidth, Setting.GAMMA,
                "Brightness", 0, 100, "%");

        // ------------------------------------------------------------ frame limiting
        y = section(screen, "Frame limiting", y);
        y = slider(screen, mapper, y, sliderWidth, Setting.MAX_FPS,
                "Framerate limit", 10, 260, "fps (260 = unlimited)");
        // Not a framerate: the underlying option is an enum describing what the game does while
        // the player is idle or the window is minimised, so it cycles rather than slides.
        y = cycle(screen, mapper, y, sliderWidth, Setting.INACTIVITY_FPS_LIMIT,
                "Inactivity limit", "MINIMIZED", "AFK");
        y = slider(screen, mapper, y, sliderWidth, Setting.GUI_SCALE,
                "GUI scale", 0, 4, "0 = auto");

        // ------------------------------------------------------------ actions
        y = section(screen, "Profiles and restore", y);
        int half = (sliderWidth - Theme.GAP) / 2;
        screen.addButton(0, y, half, 16, "Apply Low-End", b -> applyProfile(screen, "low_end"));
        screen.addButton(half + Theme.GAP, y, half, 16, "Apply Balanced", b -> applyProfile(screen, "balanced"));
        y += 18;
        screen.addButton(0, y, half, 16, "Apply High-Performance", b -> applyProfile(screen, "high_performance"));
        screen.addButton(half + Theme.GAP, y, half, 16, "Restore snapshot", b -> restoreSnapshot(screen));
        y += 18;
        screen.addButton(0, y, sliderWidth, 16, "Save settings to disk", b -> {
            services.settingMapper.saveOptions();
            services.markDirty();
            services.save();
        });

        y += 24;
        this.notesY = y;
        screen.reportContentHeight(y + 60);
    }

    @Override
    public void render(DashboardScreen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        // Widgets and section headers draw themselves; only the closing note needs painting.
        int width = screen.contentWidth() - 16;
        String note = "Every value is clamped to the range the vanilla video settings screen allows, "
                + "so a bad value can never leave the game unusable. Changing a setting here writes "
                + "it straight to the live game options and persists it to options.txt, exactly as "
                + "the vanilla screen does.";
        int noteHeight = Ui.wrappedHeight(screen.font(), note, width, 10) + 12;
        Ui.panelWithTitle(context, screen.font(), Text.literal("Notes"), 0, notesY, width, noteHeight);
        Ui.wrapped(context, screen.font(), note, 8, notesY + 20, width, Theme.TEXT_FAINT, 10);
        screen.reportContentHeight(notesY + noteHeight + 6);
    }

    private int notesY;

    // ------------------------------------------------------------------ helpers

    private int section(DashboardScreen screen, String title, int y) {
        int width = screen.contentWidth() - 16;
        screen.addSectionHeader(0, y, width, title);
        return y + 19;
    }

    private int slider(DashboardScreen screen, SettingMapper mapper, int y, int width,
                       Setting setting, String label, int min, int max, String unit) {
        String current = mapper.read(setting);
        int value = parseOr(current, min);
        screen.addSlider(0, y, width, 16, label + " (" + unit + ")", min, max, value, chosen -> {
            if (mapper.write(setting, String.valueOf(chosen))) {
                mapper.saveOptions();
                screen.services().markDirty();
                screen.rebuild();
            }
        });
        return y + ROW;
    }

    private int percentSlider(DashboardScreen screen, SettingMapper mapper, int y, int width,
                              Setting setting, String label, int min, int max) {
        String current = mapper.read(setting);
        int value = parseOr(current == null ? null : current.replace("%", ""), min);
        screen.addSlider(0, y, width, 16, label, min, max, value, chosen -> {
            if (mapper.write(setting, String.valueOf(chosen))) {
                mapper.saveOptions();
                screen.services().markDirty();
                screen.rebuild();
            }
        });
        return y + ROW;
    }

    private int toggle(DashboardScreen screen, SettingMapper mapper, int y, int width,
                       Setting setting, String label) {
        String current = mapper.read(setting);
        boolean on = "on".equalsIgnoreCase(current) || "true".equalsIgnoreCase(current);
        screen.addButton(0, y, width, 16, label + ": " + (on ? "on" : "off"), b -> {
            mapper.write(setting, on ? "off" : "on");
            mapper.saveOptions();
            screen.services().markDirty();
            screen.rebuild();
        });
        return y + ROW;
    }

    private int cycle(DashboardScreen screen, SettingMapper mapper, int y, int width,
                      Setting setting, String label, String... values) {
        String current = mapper.read(setting);
        String next = nextValue(values, current);
        screen.addButton(0, y, width, 16, label + ": " + (current == null ? "?" : current) + "  (click for " + next + ")", b -> {
            mapper.write(setting, next);
            mapper.saveOptions();
            screen.services().markDirty();
            screen.rebuild();
        });
        return y + ROW;
    }

    private static String nextValue(String[] values, String current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    private static void applyProfile(DashboardScreen screen, String id) {
        VortexServices services = screen.services();
        OptimizationProfile profile = services.config.profile(id);
        if (profile == null) {
            return;
        }
        SettingsSnapshot before = services.settingMapper.snapshot();
        var applied = services.settingMapper.applyProfile(profile);
        services.settingMapper.saveOptions();
        services.config.activeProfileId(id);
        services.config.lastKnownGood(before);
        services.markDirty();
        services.save();
        screen.rebuild();
    }

    private static void restoreSnapshot(DashboardScreen screen) {
        VortexServices services = screen.services();
        SettingsSnapshot snapshot = services.config.lastKnownGood();
        if (snapshot.isEmpty()) {
            return;
        }
        services.settingMapper.restore(snapshot);
        services.settingMapper.saveOptions();
        services.markDirty();
        services.save();
        screen.rebuild();
    }

    private static int parseOr(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim().replace("%", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
