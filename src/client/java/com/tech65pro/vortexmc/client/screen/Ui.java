package com.tech65pro.vortexmc.client.screen;

import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.core.MetricSeries;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Small drawing helpers shared by every dashboard tab. */
public final class Ui {

    private Ui() {
    }

    public static void panel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, Theme.PANEL);
        context.fill(x, y, x + width, y + 1, Theme.DIVIDER);
        context.fill(x, y, x + 1, y + height, Theme.DIVIDER);
        context.fill(x + width - 1, y, x + width, y + height, Theme.DIVIDER);
        context.fill(x, y + height - 1, x + width, y + height, Theme.DIVIDER);
    }

    public static void panelWithTitle(DrawContext context, TextRenderer font, Text title,
                                      int x, int y, int width, int height) {
        panel(context, x, y, width, height);
        context.drawTextWithShadow(font, title, x + 6, y + 5, Theme.TEXT);
        context.fill(x + 1, y + 16, x + width - 1, y + 17, Theme.DIVIDER);
    }

    /** A label on the left and a value on the right, on one line. */
    public static void row(DrawContext context, TextRenderer font, int x, int y, int width,
                           String label, String value, int valueColor) {
        context.drawTextWithShadow(font, Text.literal(label), x, y, Theme.TEXT_DIM);
        String clipped = clip(font, value, width - font.getWidth(label) - 8);
        context.drawTextWithShadow(font, Text.literal(clipped),
                x + width - font.getWidth(clipped), y, valueColor);
    }

    /** A horizontal bar, {@code fraction} clamped to 0..1. */
    public static void bar(DrawContext context, int x, int y, int width, int height,
                           double fraction, int color) {
        context.fill(x, y, x + width, y + height, Theme.PANEL_RAISED);
        double clamped = Math.max(0d, Math.min(1d, fraction));
        int filled = (int) Math.round(clamped * (width - 2));
        if (filled > 0) {
            context.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);
        }
    }

    /** A sparkline of a metric series. The newest sample is on the right. */
    public static void sparkline(DrawContext context, int x, int y, int width, int height,
                                 MetricSeries series, int lineColor, int fillColor) {
        context.fill(x, y, x + width, y + height, Theme.PANEL_RAISED);
        if (!series.hasSamples() || width < 8 || height < 4) {
            return;
        }
        double peak = series.max();
        if (peak <= 0d) {
            return;
        }
        int samples = series.sampleCount();
        int step = Math.max(1, samples / width);
        int previousX = -1;
        int previousY = -1;
        for (int i = samples - 1, drawn = 0; i >= 0 && drawn < width; i -= step, drawn++) {
            double value = series.entryAtLogical(i);
            int px = x + width - 1 - drawn;
            int py = y + height - 1 - (int) Math.round(value / peak * (height - 2));
            py = Math.max(y + 1, Math.min(y + height - 2, py));
            if (previousX >= 0 && fillColor != 0) {
                context.fill(Math.min(previousX, px), py, Math.max(previousX, px) + 1,
                        Math.max(previousY, py) + 1, fillColor);
            }
            if (previousX >= 0) {
                drawLine(context, previousX, previousY, px, py, lineColor);
            }
            previousX = px;
            previousY = py;
        }
    }

    public static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
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
            int doubled = err * 2;
            if (doubled > -dy) {
                err -= dy;
                x += sx;
            }
            if (doubled < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /** Word-wraps text to the given width and returns the lines. */
    public static List<String> wrap(TextRenderer font, String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || width <= 8) {
            return lines;
        }
        for (String paragraph : text.split("\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (font.getWidth(candidate) <= width) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    if (current.length() > 0) {
                        lines.add(current.toString());
                    }
                    if (font.getWidth(word) <= width) {
                        current.setLength(0);
                        current.append(word);
                    } else {
                        // A single word longer than the line: break it hard.
                        StringBuilder chunk = new StringBuilder();
                        for (char c : word.toCharArray()) {
                            if (font.getWidth(chunk.toString() + c) > width && chunk.length() > 0) {
                                lines.add(chunk.toString());
                                chunk.setLength(0);
                            }
                            chunk.append(c);
                        }
                        current.setLength(0);
                        current.append(chunk);
                    }
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    public static int wrappedHeight(TextRenderer font, String text, int width, int lineHeight) {
        return Math.max(1, wrap(font, text, width).size()) * lineHeight;
    }

    /** Draws wrapped text starting at y and returns the y position after it. */
    public static int wrapped(DrawContext context, TextRenderer font, String text,
                              int x, int y, int width, int color, int lineHeight) {
        int cursor = y;
        for (String line : wrap(font, text, width)) {
            context.drawTextWithShadow(font, Text.literal(line), x, cursor, color);
            cursor += lineHeight;
        }
        return cursor;
    }

    public static String clip(TextRenderer font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (font.getWidth(text) <= maxWidth) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.getWidth(builder.toString() + c + "...") > maxWidth) {
                break;
            }
            builder.append(c);
        }
        return builder + "...";
    }

    public static int colorForFps(float fps) {
        if (fps <= 0f) {
            return Theme.TEXT_FAINT;
        }
        if (fps < 30f) {
            return Theme.BAD;
        }
        if (fps < 55f) {
            return Theme.WARN;
        }
        return Theme.GOOD;
    }

    public static int colorForPercent(double percent) {
        if (percent < 60d) {
            return Theme.GOOD;
        }
        if (percent < 85d) {
            return Theme.WARN;
        }
        return Theme.BAD;
    }
}
