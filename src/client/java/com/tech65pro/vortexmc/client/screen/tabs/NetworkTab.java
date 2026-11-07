package com.tech65pro.vortexmc.client.screen.tabs;

import com.tech65pro.vortexmc.client.NetworkDiagnostics;
import com.tech65pro.vortexmc.client.Theme;
import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import com.tech65pro.vortexmc.client.screen.DashboardTab;
import com.tech65pro.vortexmc.client.screen.Ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/** Connection information the game is willing to report. */
public final class NetworkTab implements DashboardTab {

    @Override
    public String title() {
        return "Network";
    }

    @Override
    public void build(DashboardScreen screen) {
        // Read-only tab.
    }

    @Override
    public void render(DashboardScreen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        NetworkDiagnostics network = screen.services().networkDiagnostics;
        TextRenderer font = screen.font();
        int width = screen.contentWidth();
        int inner = width - 16;

        int statusPanel = 50;
        Ui.panelWithTitle(context, font, Text.literal("Connection"), 0, 0, width, statusPanel);
        String state;
        int stateColor;
        if (!network.isInGame()) {
            state = "Not in a world";
            stateColor = Theme.TEXT_FAINT;
        } else if (network.isSinglePlayer()) {
            state = "Single-player world";
            stateColor = Theme.GOOD;
        } else if (network.isMultiplayer()) {
            state = network.isConnectionOpen() ? "Connected to a server" : "Connecting";
            stateColor = network.isConnectionOpen() ? Theme.GOOD : Theme.WARN;
        } else {
            state = "In a world, connection state unknown";
            stateColor = Theme.TEXT_DIM;
        }
        context.drawTextWithShadow(font, Text.literal(state), 8, 22, stateColor);
        String server = network.serverName();
        context.drawTextWithShadow(font, Text.literal("Server: " + (server == null ? "n/a" : server)),
                8, 33, Theme.TEXT_DIM);

        int y = statusPanel + 6;
        int rows = 132;
        Ui.panelWithTitle(context, font, Text.literal("Detail"), 0, y, width, rows);
        int rowY = y + 22;
        int half = inner / 2 - 4;

        Integer latency = network.latencyMillis();
        Ui.row(context, font, 8, rowY, half, "Latency",
                latency == null ? "n/a" : latency + " ms",
                latency == null ? Theme.TEXT_FAINT
                        : latency < 60 ? Theme.GOOD : latency < 150 ? Theme.WARN : Theme.BAD);
        Ui.row(context, font, 8 + half + 8, rowY, half, "Server address",
                orUnknown(network.serverAddress()), Theme.TEXT);
        rowY += 13;

        Ui.row(context, font, 8, rowY, half, "Version", orUnknown(network.protocolVersion()), Theme.TEXT);
        Integer players = network.knownPlayerCount();
        Ui.row(context, font, 8 + half + 8, rowY, half, "Known players",
                players == null ? "n/a" : String.valueOf(players), Theme.TEXT);
        rowY += 13;

        Float[] rates = network.packetRates();
        Ui.row(context, font, 8, rowY, half, "Packets sent / s",
                rates == null ? "n/a" : String.format("%.1f", rates[0]), Theme.TEXT);
        Ui.row(context, font, 8 + half + 8, rowY, half, "Packets received / s",
                rates == null ? "n/a" : String.format("%.1f", rates[1]), Theme.TEXT);
        rowY += 13;

        Integer entities = network.entityCount();
        Ui.row(context, font, 8, rowY, half, "Entities loaded",
                entities == null ? "n/a" : String.valueOf(entities), Theme.TEXT);
        Integer nearby = network.nearbyEntityCount(64.0);
        Ui.row(context, font, 8 + half + 8, rowY, half, "Entities within 64 blocks",
                nearby == null ? "n/a" : String.valueOf(nearby), Theme.TEXT);
        rowY += 13;

        Ui.row(context, font, 8, rowY, half, "Connection open",
                String.valueOf(network.isConnectionOpen()), Theme.TEXT);
        Ui.row(context, font, 8 + half + 8, rowY, half, "In world",
                String.valueOf(network.isInGame()), Theme.TEXT);

        y += rows + 6;
        String note = "Latency is the round-trip time the server reports for you through the player "
                + "list. It describes the path between you and that server, not the speed of your "
                + "internet connection, and VortexMC does not change either. Packet rates are what "
                + "the game's own connection object averages; they are a useful signal for spotting "
                + "a server that is sending you far more data than it needs to, but they are not a "
                + "bandwidth measurement.";
        int noteHeight = Ui.wrappedHeight(font, note, inner, 10) + 12;
        Ui.panelWithTitle(context, font, Text.literal("What these numbers mean"), 0, y, width, noteHeight);
        Ui.wrapped(context, font, note, 8, y + 20, inner, Theme.TEXT_FAINT, 10);

        screen.reportContentHeight(y + noteHeight + 6);
    }

    private static String orUnknown(String value) {
        return value == null || value.isBlank() ? "n/a" : value;
    }
}
