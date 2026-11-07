package com.tech65pro.vortexmc.client.screen.tabs;

import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.client.VortexServices;
import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import com.tech65pro.vortexmc.client.screen.DashboardTab;
import com.tech65pro.vortexmc.client.screen.Ui;
import com.tech65pro.vortexmc.core.SettingChange;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Smart Auto behaviour, interface options, change history and compatibility. */
public final class SettingsTab implements DashboardTab {

    private static final int ROW = 26;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String title() {
        return "Settings";
    }

    @Override
    public void build(DashboardScreen screen) {
        VortexServices services = screen.services();
        var config = services.config;
        int width = screen.contentWidth();
        int inner = width - 16;
        int y = 0;

        // ------------------------------------------------------------ Smart Auto
        screen.addSectionHeader(0, y, inner, "Smart Auto");
        y += 19;
        y = toggle(screen, y, inner, "Smart Auto enabled", config.smartAutoEnabled(), value -> {
            config.smartAutoEnabled(value);
            services.markDirty();
            services.save();
        });
        y = toggle(screen, y, inner, "Ask before applying anything", config.requireApproval(), value -> {
            config.requireApproval(value);
            services.markDirty();
            services.save();
        });
        y = toggle(screen, y, inner, "Measure a baseline before changing", config.measureBeforeActing(), value -> {
            config.measureBeforeActing(value);
            services.markDirty();
            services.save();
        });
        y = toggle(screen, y, inner, "Auto-revert a change that measured worse", config.autoRevertFailedChanges(), value -> {
            config.autoRevertFailedChanges(value);
            services.markDirty();
            services.save();
        });

        y = slider(screen, y, inner, "Measurement length", 4, 60,
                config.measurementSeconds(), "seconds", value -> {
                    config.measurementSeconds(value);
                    services.markDirty();
                    services.save();
                });
        y = slider(screen, y, inner, "Cooldown between suggestions", 30, 600,
                config.cooldownSeconds(), "seconds", value -> {
                    config.cooldownSeconds(value);
                    services.markDirty();
                    services.save();
                });
        y = slider(screen, y, inner, "Do not act above this frame rate", 10, 120,
                config.minFpsBeforeActing(), "fps", value -> {
                    config.minFpsBeforeActing(value);
                    services.markDirty();
                    services.save();
                });

        // ------------------------------------------------------------ interface
        screen.addSectionHeader(0, y, inner, "Interface");
        y += 19;
        y = toggle(screen, y, inner, "Show the in-game overlay", config.showOverlay(), value -> {
            config.showOverlay(value);
            services.markDirty();
            services.save();
        });
        y = toggle(screen, y, inner, "Keep the change history", config.showHistory(), value -> {
            config.showHistory(value);
            services.markDirty();
            services.save();
        });

        // ------------------------------------------------------------ actions
        screen.addSectionHeader(0, y, inner, "Actions");
        y += 19;
        int half = (inner - Theme.GAP) / 2;
        screen.addButton(0, y, half, 16, "Undo last Smart Auto change", b -> {
            services.smartAuto.revertLast();
            services.markDirty();
            services.save();
            screen.rebuild();
        });
        screen.addButton(half + Theme.GAP, y, half, 16, "Restore last known good", b -> {
            services.smartAuto.restoreLastKnownGood();
            services.markDirty();
            services.save();
            screen.rebuild();
        });
        y += 18;
        screen.addButton(0, y, inner, 16, "Save configuration now", b -> {
            services.save();
            screen.rebuild();
        });
        y += 18;
        screen.addButton(0, y, inner, 16, "Clear the change history", b -> {
            config.clearHistory();
            services.markDirty();
            services.save();
            screen.rebuild();
        });

        y += 22;
        this.historyY = y;
        screen.reportContentHeight(y + 220);
    }

    private int historyY;

    @Override
    public void render(DashboardScreen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        VortexServices services = screen.services();
        TextRenderer font = screen.font();
        int width = screen.contentWidth();
        int inner = width - 16;

        int y = historyY;

        // ------------------------------------------------------------ history
        List<SettingChange> history = services.config.changeHistory();
        int historyPanel = Math.max(40, Math.min(140, 22 + history.size() * 11));
        Ui.panelWithTitle(context, font, Text.literal("Change history"), 0, y, width, historyPanel);
        if (history.isEmpty()) {
            context.drawTextWithShadow(font, Text.literal("Nothing has been changed yet."),
                    8, y + 22, Theme.TEXT_FAINT);
        } else {
            int rowY = y + 22;
            int start = Math.max(0, history.size() - 9);
            for (int i = start; i < history.size(); i++) {
                SettingChange change = history.get(i);
                String line = TIME.format(new Date(change.timestampMillis())) + "  "
                        + change.describe() + "  [" + change.source().label() + "]"
                        + (change.reverted() ? "  reverted" : "");
                context.drawTextWithShadow(font,
                        Text.literal(Ui.clip(font, line, inner)),
                        8, rowY, change.reverted() ? Theme.TEXT_FAINT : Theme.TEXT_DIM);
                rowY += 11;
            }
        }
        y += historyPanel + 6;

        // ------------------------------------------------------------ compatibility
        Map<String, List<String>> categories = services.compatibility.presentByCategory();
        int compatHeight = 24;
        for (List<String> found : categories.values()) {
            compatHeight += found.isEmpty() ? 0 : 11;
        }
        compatHeight += 22;
        Ui.panelWithTitle(context, font, Text.literal("Installed mods VortexMC knows about"),
                0, y, width, compatHeight);
        int rowY = y + 22;
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            context.drawTextWithShadow(font,
                    Text.literal(entry.getKey() + ": " + String.join(", ", entry.getValue())),
                    8, rowY, Theme.TEXT_DIM);
            rowY += 11;
        }
        rowY = Ui.wrapped(context, font, services.compatibility.interactionNote(), 8, rowY + 2,
                inner, Theme.TEXT_FAINT, 10);

        y += compatHeight + 6;
        String note = "VortexMC detects other mods only by their mod id, through the loader's "
                + "isModLoaded call. It never reads, reflects into or copies another mod's code, and "
                + "it does not disable anything. The note above exists so you can tell at a glance "
                + "whether a change VortexMC makes is likely to have an effect.";
        int noteHeight = Ui.wrappedHeight(font, note, inner, 10) + 12;
        Ui.panelWithTitle(context, font, Text.literal("About compatibility detection"), 0, y, width, noteHeight);
        Ui.wrapped(context, font, note, 8, y + 20, inner, Theme.TEXT_FAINT, 10);

        screen.reportContentHeight(y + noteHeight + 6);
    }

    private int toggle(DashboardScreen screen, int y, int width, String label, boolean value,
                       java.util.function.Consumer<Boolean> onChange) {
        screen.addButton(0, y, width, 16, label + ": " + (value ? "on" : "off"), b -> {
            onChange.accept(Boolean.valueOf(!value));
            screen.rebuild();
        });
        return y + ROW;
    }

    private int slider(DashboardScreen screen, int y, int width, String label, int min, int max,
                       int value, String unit, java.util.function.IntConsumer onChange) {
        screen.addSlider(0, y, width, 16, label + " (" + unit + ")", min, max, value, onChange);
        return y + ROW;
    }
}
