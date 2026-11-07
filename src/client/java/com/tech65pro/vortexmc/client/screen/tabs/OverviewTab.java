package com.tech65pro.vortexmc.client.screen.tabs;

import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.client.VortexServices;
import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import com.tech65pro.vortexmc.client.screen.DashboardTab;
import com.tech65pro.vortexmc.client.screen.Ui;
import com.tech65pro.vortexmc.config.BuiltInProfiles;
import com.tech65pro.vortexmc.core.PerformanceAnalysis;
import com.tech65pro.vortexmc.core.Recommendation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/** The landing tab: what the game is doing right now, and what Smart Auto thinks. */
public final class OverviewTab implements DashboardTab {

    private static final int CARD_HEIGHT = 46;

    @Override
    public String title() {
        return "Overview";
    }

    @Override
    public void build(DashboardScreen screen) {
        int width = screen.contentWidth();
        int cardWidth = (width - Theme.GAP * 3) / 4;
        int x = 0;
        int y = 0;
        screen.addButton(x, y + CARD_HEIGHT - 16, cardWidth, 16, "Open FPS monitor", b -> screen.selectTab(1));
        x += cardWidth + Theme.GAP;
        screen.addButton(x, y + CARD_HEIGHT - 16, cardWidth, 16, "Smart Auto: analyse now",
                b -> screen.services().smartAuto.requestAnalysis());
        x += cardWidth + Theme.GAP;
        screen.addButton(x, y + CARD_HEIGHT - 16, cardWidth, 16, "Graphics settings",
                b -> screen.selectTab(2));
        x += cardWidth + Theme.GAP;
        screen.addButton(x, y + CARD_HEIGHT - 16, cardWidth, 16, "Profiles", b -> screen.selectTab(5));

        // Smart Auto never changes anything on its own. When it has a proposal waiting, the
        // only way forward is one of these two buttons; there is deliberately no third option
        // and no timer that applies the change behind the player's back.
        if (screen.services().smartAuto.pending() != null) {
            int rowY = CARD_HEIGHT + Theme.GAP;
            int half = (width - Theme.GAP) / 2;
            screen.addButton(0, rowY, half, 16, "Approve and measure", b -> {
                screen.services().smartAuto.approve();
                screen.rebuild();
            });
            screen.addButton(half + Theme.GAP, rowY, half, 16, "Decline", b -> {
                screen.services().smartAuto.decline();
                screen.rebuild();
            });
        }
    }

    /**
     * Height reserved for the Approve / Decline row. Zero when nothing is waiting, so the Smart
     * Auto panel moves up and there is no dead space. {@link #build} and {@link #render} must
     * agree on this or the buttons will sit on top of the panel.
     */
    private static int approvalRowHeight(DashboardScreen screen) {
        return screen.services().smartAuto.pending() != null ? 16 + Theme.GAP : 0;
    }

    @Override
    public void render(DashboardScreen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        VortexServices services = screen.services();
        TextRenderer font = screen.font();
        int width = screen.contentWidth();
        int x = 6;
        int y = 4;

        int cardWidth = (width - Theme.GAP * 3) / 4;
        drawCard(context, font, x, y, cardWidth, CARD_HEIGHT, "Framerate",
                services.performanceMonitor.hasFrameData()
                        ? Math.round(services.performanceMonitor.averageFps()) + " fps"
                        : "measuring...",
                "vanilla counter: " + services.performanceMonitor.vanillaFps(),
                services.performanceMonitor.hasFrameData()
                        ? Ui.colorForFps(services.performanceMonitor.averageFps()) : Theme.TEXT_FAINT);
        x += cardWidth + Theme.GAP;
        drawCard(context, font, x, y, cardWidth, CARD_HEIGHT, "1% low",
                formatLow(services.performanceMonitor.onePercentLowFps()),
                "0.1% low " + formatLow(services.performanceMonitor.zeroPointOnePercentLowFps()),
                Ui.colorForFps(services.performanceMonitor.onePercentLowFps()));
        x += cardWidth + Theme.GAP;
        drawCard(context, font, x, y, cardWidth, CARD_HEIGHT, "Frame time",
                String.format("%.1f ms", services.performanceMonitor.medianFrameTimeMillis()),
                "worst " + String.format("%.0f ms", services.performanceMonitor.worstFrameTimeMillis()),
                Theme.TEXT);
        x += cardWidth + Theme.GAP;
        drawCard(context, font, x, y, cardWidth, CARD_HEIGHT, "Session average",
                services.performanceMonitor.hasHistory()
                        ? Math.round(services.performanceMonitor.sessionAverageFps()) + " fps" : "n/a",
                "samples: " + services.performanceMonitor.historySampleCount() + " s",
                Ui.colorForFps((float) services.performanceMonitor.sessionAverageFps()));

        y += CARD_HEIGHT + Theme.GAP + approvalRowHeight(screen);

        // ---------------------------------------------------------------- Smart Auto
        int smartHeight = 76;
        Ui.panelWithTitle(context, font, Text.literal("Smart Auto"), 0, y, width, smartHeight);
        int innerX = 8;
        int innerY = y + 20;
        int innerWidth = width - 16;

        String state = services.smartAuto.phase().label();
        context.drawTextWithShadow(font, Text.literal("Status: " + state), innerX, innerY,
                services.smartAuto.isBusy() ? Theme.WARN : Theme.TEXT);

        double progress = services.smartAuto.progress();
        if (services.smartAuto.isBusy()) {
            String progressText = services.smartAuto.samplesInCurrentPhase() + " / "
                    + services.smartAuto.measurementTarget() + " s";
            context.drawTextWithShadow(font, Text.literal(progressText),
                    innerX + innerWidth - font.getWidth(progressText), innerY, Theme.TEXT_DIM);
            Ui.bar(context, innerX, innerY + 10, innerWidth, 6, progress, Theme.ACCENT);
        } else {
            String enabled = services.config.smartAutoEnabled() ? "enabled" : "disabled";
            context.drawTextWithShadow(font, Text.literal(enabled),
                    innerX + innerWidth - font.getWidth(enabled), innerY,
                    services.config.smartAutoEnabled() ? Theme.GOOD : Theme.TEXT_FAINT);
        }

        innerY = Ui.wrapped(context, font, services.smartAuto.lastMessage(), innerX, innerY + 20,
                innerWidth, Theme.TEXT_DIM, 10);

        PerformanceAnalysis analysis = services.smartAuto.lastAnalysis();
        if (analysis != null) {
            String verdict = analysis.describe() + "  (confidence "
                    + Math.round(analysis.confidence() * 100) + "%)";
            innerY = Ui.wrapped(context, font, verdict, innerX, innerY + 2, innerWidth,
                    analysis.isMeasuredImprovement() ? Theme.GOOD
                            : analysis.isMeasuredRegression() ? Theme.BAD : Theme.TEXT_DIM, 10);
        }

        if (services.smartAuto.pending() != null) {
            String pending = "Waiting on: " + services.smartAuto.pending().summary();
            Ui.wrapped(context, font, pending, innerX, innerY + 2, innerWidth, Theme.WARN, 10);
        }

        y += smartHeight + Theme.GAP;

        // ---------------------------------------------------------------- profile
        int profileHeight = 54;
        Ui.panelWithTitle(context, font, Text.literal("Active optimisation profile"), 0, y, width, profileHeight);
        var profile = services.config.activeProfile();
        context.drawTextWithShadow(font, Text.literal(profile.displayName()), 8, y + 20, Theme.TEXT);
        String detail = profile.id() + "  |  " + profile.definedSettingCount()
                + " setting(s) defined  |  " + (profile.builtIn() ? "built in" : "custom");
        context.drawTextWithShadow(font, Text.literal(Ui.clip(font, detail, width - 16)),
                8, y + 31, Theme.TEXT_DIM);
        String note = BuiltInProfiles.isBuiltInId(profile.id())
                ? "Built-in profiles are conservative by design. They reduce the work the renderer "
                  + "is asked to do; they do not promise a specific frame-rate gain."
                : "Custom profiles are yours to tune. VortexMC never changes a setting you have not "
                  + "asked it to change.";
        Ui.wrapped(context, font, note, 8, y + 42, width - 16, Theme.TEXT_FAINT, 10);

        y += profileHeight + Theme.GAP;

        // ---------------------------------------------------------------- honesty note
        String honesty = "What this shows: every number above is measured from real rendered frames "
                + "by VortexMC itself, once per frame, with nothing estimated. If a metric says "
                + "\"measuring...\" there genuinely is not enough data yet. VortexMC does not force "
                + "garbage collection, does not overclock anything, and does not change your "
                + "network connection.";
        int honestyHeight = Ui.wrappedHeight(font, honesty, width - 16, 10) + 12;
        Ui.panelWithTitle(context, font, Text.literal("How to read this"), 0, y, width, honestyHeight);
        Ui.wrapped(context, font, honesty, 8, y + 20, width - 16, Theme.TEXT_DIM, 10);

        screen.reportContentHeight(y + honestyHeight + 6);
    }

    private static void drawCard(DrawContext context, TextRenderer font, int x, int y, int width,
                                 int height, String title, String value, String sub, int color) {
        Ui.panel(context, x, y, width, height);
        context.drawTextWithShadow(font, Text.literal(title), x + 6, y + 5, Theme.TEXT_FAINT);
        context.drawTextWithShadow(font, Text.literal(value), x + 6, y + 15, color);
        context.drawTextWithShadow(font, Text.literal(Ui.clip(font, sub, width - 12)),
                x + 6, y + 32, Theme.TEXT_FAINT);
    }

    private static String formatLow(float fps) {
        return fps > 0f ? Math.round(fps) + " fps" : "n/a";
    }
}
