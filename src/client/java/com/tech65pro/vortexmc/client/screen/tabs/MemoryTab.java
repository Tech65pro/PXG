package com.tech65pro.vortexmc.client.screen.tabs;

import com.tech65pro.vortexmc.client.MemoryDiagnostics;
import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import com.tech65pro.vortexmc.client.screen.DashboardTab;
import com.tech65pro.vortexmc.client.screen.Ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/** Heap diagnostics, read straight from the JVM. */
public final class MemoryTab implements DashboardTab {

    @Override
    public String title() {
        return "Memory";
    }

    @Override
    public void build(DashboardScreen screen) {
        // No widgets: every value on this tab is read live from the JVM at render time, so there
        // is nothing to configure and nothing to rebuild.
    }

    @Override
    public void render(DashboardScreen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        MemoryDiagnostics memory = screen.services().memoryDiagnostics;
        TextRenderer font = screen.font();
        int width = screen.contentWidth();
        int inner = width - 16;

        double usedPercent = memory.usedPercent();
        double usedOfCommitted = memory.usedOfCommittedPercent();

        int barPanel = 62;
        Ui.panelWithTitle(context, font, Text.literal("Heap usage"), 0, 0, width, barPanel);
        Ui.bar(context, 8, 24, inner, 12, usedPercent / 100d, Ui.colorForPercent(usedPercent));
        String headline = String.format("%.0f MB used of %.0f MB maximum (%.0f%%)",
                MemoryDiagnostics.toMegabytes(memory.usedHeapBytes()),
                MemoryDiagnostics.toMegabytes(memory.maxHeapBytes()),
                usedPercent);
        context.drawTextWithShadow(font, Text.literal(headline), 8, 40, Theme.TEXT);
        Ui.wrapped(context, font, memory.assessment(), 8, barPanel - 10, inner, Theme.TEXT_DIM, 10);

        int y = barPanel + 6;
        int rows = 116;
        Ui.panelWithTitle(context, font, Text.literal("Detail"), 0, y, width, rows);
        int rowY = y + 22;
        int half = inner / 2 - 4;
        Ui.row(context, font, 8, rowY, half, "Used heap",
                MemoryDiagnostics.formatMegabytes(memory.usedHeapBytes()), Theme.TEXT);
        Ui.row(context, font, 8 + half + 8, rowY, half, "Committed heap",
                MemoryDiagnostics.formatMegabytes(memory.committedHeapBytes()), Theme.TEXT);
        rowY += 13;
        Ui.row(context, font, 8, rowY, half, "Maximum heap",
                MemoryDiagnostics.formatMegabytes(memory.maxHeapBytes()), Theme.TEXT);
        Ui.row(context, font, 8 + half + 8, rowY, half, "Free in committed",
                MemoryDiagnostics.formatMegabytes(memory.freeHeapBytes()), Theme.TEXT);
        rowY += 13;
        Ui.row(context, font, 8, rowY, half, "Not yet committed",
                MemoryDiagnostics.formatMegabytes(memory.uncommittedHeapBytes()), Theme.TEXT);
        Ui.row(context, font, 8 + half + 8, rowY, half, "Used of committed",
                String.format("%.0f%%", usedOfCommitted), Ui.colorForPercent(usedOfCommitted));
        rowY += 13;
        Ui.row(context, font, 8, rowY, half, "Available processors",
                String.valueOf(memory.availableProcessors()), Theme.TEXT);
        Ui.row(context, font, 8 + half + 8, rowY, half, "Heap sizing",
                memory.looksUndersized() ? "below 2 GB" : "adequate", memory.looksUndersized() ? Theme.WARN : Theme.GOOD);

        y += rows + 6;
        String note = "VortexMC never forces a garbage collection. Calling System.gc() would stall "
                + "the game for as long as the collector needs to run, which is the opposite of what "
                + "a performance mod is for, and it would not make the numbers below more accurate. "
                + "If the used percentage sits near the maximum, the fix is a larger -Xmx in the "
                + "launcher, not a forced collection. VortexMC does not change the heap size itself.";
        int noteHeight = Ui.wrappedHeight(font, note, inner, 10) + 12;
        Ui.panelWithTitle(context, font, Text.literal("Why there is no 'collect now' button"), 0, y, width, noteHeight);
        Ui.wrapped(context, font, note, 8, y + 20, inner, Theme.TEXT_FAINT, 10);

        screen.reportContentHeight(y + noteHeight + 6);
    }
}
