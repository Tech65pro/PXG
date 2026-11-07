package com.tech65pro.vortexmc.client;

import com.tech65pro.vortexmc.core.MetricSeries;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * The compact readout drawn over the game when the overlay is enabled.
 *
 * <p>It shows only numbers the mod has actually measured. When no frames have been sampled yet it
 * says so rather than showing a zero.</p>
 */
public final class OverlayRenderer {

    private static final int WIDTH = 116;
    private static final int HEIGHT = 40;
    private static final int PADDING = 5;

    private final MinecraftClient client;
    private final PerformanceMonitor monitor;

    public OverlayRenderer(MinecraftClient client, PerformanceMonitor monitor) {
        this.client = client;
        this.monitor = monitor;
    }

    public void render(DrawContext context) {
        MinecraftClient game = client == null ? MinecraftClient.getInstance() : client;
        if (game == null || game.textRenderer == null) {
            return;
        }
        int margin = 4;
        int right = game.getWindow() == null ? 0 : game.getWindow().getScaledWidth();
        if (right <= 0) {
            return;
        }
        int x = right - WIDTH - margin;
        int y = margin;

        context.fill(x, y, x + WIDTH, y + HEIGHT, Theme.withAlpha(Theme.BACKDROP, 210));
        context.fill(x, y, x + WIDTH, y + 1, Theme.ACCENT);

        int textX = x + PADDING;
        int textY = y + PADDING;
        var renderer = game.textRenderer;

        if (!monitor.hasFrameData()) {
            context.drawTextWithShadow(renderer, Text.literal("VortexMC"), textX, textY, Theme.TEXT);
            context.drawTextWithShadow(renderer, Text.literal("measuring..."), textX, textY + 10, Theme.TEXT_DIM);
            return;
        }

        int fps = Math.round(monitor.averageFps());
        context.drawTextWithShadow(renderer, Text.literal(fps + " fps"), textX, textY, Theme.TEXT);

        float onePercent = monitor.onePercentLowFps();
        String low = onePercent > 0f ? Math.round(onePercent) + " 1% low" : "1% low n/a";
        context.drawTextWithShadow(renderer, Text.literal(low), textX, textY + 10, Theme.TEXT_DIM);

        String frame = String.format("%.1f ms", monitor.medianFrameTimeMillis());
        context.drawTextWithShadow(renderer, Text.literal(frame), textX + WIDTH / 2, textY, Theme.TEXT_DIM);
        context.drawTextWithShadow(renderer, Text.literal("avg " + Math.round(monitor.sessionAverageFps()) + " fps"),
                textX + WIDTH / 2, textY + 10, Theme.TEXT_FAINT);

        drawSparkline(context, x + PADDING, y + HEIGHT - 12, WIDTH - PADDING * 2, 9);
    }

    private void drawSparkline(DrawContext context, int x, int y, int width, int height) {
        MetricSeries history = monitor.fpsHistory();
        if (!history.hasSamples() || width < 8 || height < 4) {
            return;
        }
        double peak = history.max();
        if (peak <= 0d) {
            return;
        }
        int samples = history.sampleCount();
        int step = Math.max(1, samples / width);
        int previousX = -1;
        int previousY = -1;
        for (int i = samples - 1, drawn = 0; i >= 0 && drawn < width; i -= step, drawn++) {
            double value = history.entryAtLogical(i);
            int px = x + width - 1 - drawn;
            int py = y + height - 1 - (int) Math.round(value / peak * (height - 1));
            py = Math.max(y, Math.min(y + height - 1, py));
            if (previousX >= 0) {
                drawLine(context, previousX, previousY, px, py, Theme.GRAPH_LINE);
            }
            previousX = px;
            previousY = py;
        }
    }

    private static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1;
        int y = y1;
        while (true) {
            context.fill(x, y, x + 1, y + 1, color);
            if (x == x2 && y == y2) {
                break;
            }
            int doubleError = err * 2;
            if (doubleError > -dy) {
                err -= dy;
                x += sx;
            }
            if (doubleError < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
