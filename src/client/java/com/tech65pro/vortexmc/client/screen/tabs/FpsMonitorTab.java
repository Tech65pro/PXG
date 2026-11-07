package com.tech65pro.vortexmc.client.screen.tabs;

import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.client.VortexServices;
import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import com.tech65pro.vortexmc.client.screen.DashboardTab;
import com.tech65pro.vortexmc.client.screen.Ui;
import com.tech65pro.vortexmc.core.MetricSeries;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/** Graphs and detailed statistics for the measured frame times. */
public final class FpsMonitorTab implements DashboardTab {

    @Override
    public String title() {
        return "FPS Monitor";
    }

    @Override
    public void build(DashboardScreen screen) {
        int width = screen.contentWidth();
        int y = 100;
        int half = (width - Theme.GAP) / 2;
        screen.addButton(0, y, half, 16, "Reset measurements", b -> {
            screen.services().performanceMonitor.reset();
            screen.rebuild();
        });
        screen.addButton(half + Theme.GAP, y, half, 16, "Copy summary to chat", b -> {
            var services = screen.services();
            var monitor = services.performanceMonitor;
            if (monitor.hasFrameData() && services.client().inGameHud != null) {
                String summary = String.format(
                        "VortexMC: %.1f fps avg, %.0f fps 1%% low, %.1f ms median, %.0f ms worst",
                        monitor.averageFps(), monitor.onePercentLowFps(),
                        monitor.medianFrameTimeMillis(), monitor.worstFrameTimeMillis());
                services.client().inGameHud.getChatHud().addMessage(Text.literal(summary));
            }
            screen.rebuild();
        });
    }

    @Override
    public void render(DashboardScreen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        var monitor = screen.services().performanceMonitor;
        TextRenderer font = screen.font();
        int width = screen.contentWidth();
        int inner = width - 16;

        int graphHeight = 58;
        Ui.panelWithTitle(context, font, Text.literal("Average FPS, last 5 minutes"), 0, 0, width, graphHeight);
        Ui.sparkline(context, 8, 20, inner, graphHeight - 34, monitor.fpsHistory(),
                Theme.GRAPH_LINE, Theme.GRAPH_FILL);
        drawGraphLegend(context, font, 8, graphHeight - 11, monitor.fpsHistory());

        int y = graphHeight + Theme.GAP;
        Ui.panelWithTitle(context, font, Text.literal("1% low FPS, last 5 minutes"), 0, y, width, graphHeight);
        Ui.sparkline(context, 8, y + 20, inner, graphHeight - 34, monitor.onePercentHistory(),
                Theme.ACCENT_ALT, Theme.ACCENT_ALT_SOFT);
        drawGraphLegend(context, font, 8, y + graphHeight - 11, monitor.onePercentHistory());

        y += graphHeight + Theme.GAP;
        int statsHeight = 122;
        Ui.panelWithTitle(context, font, Text.literal("Frame time statistics"), 0, y, width, statsHeight);
        int rowY = y + 22;
        int half = inner / 2 - 4;
        row(context, font, 8, rowY, half, "Samples in window", monitor.frameSampleCount() + " frames");
        row(context, font, 8 + half + 8, rowY, half, "History samples", monitor.historySampleCount() + " s");
        rowY += 13;
        row(context, font, 8, rowY, half, "Instant FPS", fmt(monitor.instantFps()));
        row(context, font, 8 + half + 8, rowY, half, "Average FPS", fmt(monitor.averageFps()));
        rowY += 13;
        row(context, font, 8, rowY, half, "1% low FPS", fmt(monitor.onePercentLowFps()));
        row(context, font, 8 + half + 8, rowY, half, "0.1% low FPS", fmt(monitor.zeroPointOnePercentLowFps()));
        rowY += 13;
        row(context, font, 8, rowY, half, "Median frame time", ms(monitor.medianFrameTimeMillis()));
        row(context, font, 8 + half + 8, rowY, half, "Average frame time", ms(monitor.frameTimeHistory().latest()));
        rowY += 13;
        row(context, font, 8, rowY, half, "Worst frame time", ms(monitor.worstFrameTimeMillis()));
        row(context, font, 8 + half + 8, rowY, half, "Frame time spread", ms(monitor.frameTimeSpreadMillis()));
        rowY += 13;
        row(context, font, 8, rowY, half, "Session average", fmt((float) monitor.sessionAverageFps()));
        row(context, font, 8 + half + 8, rowY, half, "Session range",
                fmt((float) monitor.sessionLowestAverageFps()) + " - "
                        + fmt((float) monitor.sessionHighestAverageFps()));

        rowY += 18;
        String trend = String.format("Trend: %+.2f fps per second across the retained window. ", monitor.fpsTrendPerSecond());
        double drop = monitor.detectedDropPercent();
        String dropText = drop > 0d
                ? String.format("A sustained drop of %.0f%% was detected. ", drop)
                : "No sustained drop detected. ";
        String stutter = monitor.isStuttering()
                ? "Frame times are uneven, which reads as stutter rather than a low average."
                : "Frame times are fairly even.";
        Ui.wrapped(context, font, trend + dropText + stutter, 8, rowY, inner, Theme.TEXT_DIM, 10);

        y += statsHeight + Theme.GAP;
        String note = "Sampling happens once per rendered frame from a Fabric HUD element, so these "
                + "are the frame times the game actually produced. The vanilla FPS counter is shown "
                + "on the Overview tab for comparison; small differences between the two are normal "
                + "because they average over different windows.";
        int noteHeight = Ui.wrappedHeight(font, note, inner, 10) + 12;
        Ui.panelWithTitle(context, font, Text.literal("About these numbers"), 0, y, width, noteHeight);
        Ui.wrapped(context, font, note, 8, y + 20, inner, Theme.TEXT_FAINT, 10);

        screen.reportContentHeight(y + noteHeight + 6);
    }

    private static void drawGraphLegend(DrawContext context, TextRenderer font, int x, int y, MetricSeries series) {
        if (!series.hasSamples()) {
            context.drawTextWithShadow(font, Text.literal("no samples yet"), x, y, Theme.TEXT_FAINT);
            return;
        }
        String text = String.format("min %.0f   max %.0f   now %.0f   n = %d",
                series.min(), series.max(), series.latest(), series.sampleCount());
        context.drawTextWithShadow(font, Text.literal(text), x, y, Theme.TEXT_FAINT);
    }

    private static void row(DrawContext context, TextRenderer font, int x, int y, int width,
                            String label, String value) {
        Ui.row(context, font, x, y, width, label, value, Theme.TEXT);
    }

    private static String fmt(float value) {
        return value > 0f ? String.format("%.1f fps", value) : "n/a";
    }

    private static String ms(float value) {
        return value > 0f ? String.format("%.1f ms", value) : "n/a";
    }

    private static String ms(double value) {
        return value > 0d ? String.format("%.1f ms", value) : "n/a";
    }
}
