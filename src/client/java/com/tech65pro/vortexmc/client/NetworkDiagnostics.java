package com.tech65pro.vortexmc.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;

import java.util.Collection;

/**
 * Reads whatever the game will tell us about the current connection.
 *
 * <p>Every field can be unavailable: single-player has no server entry, a connection that has not
 * finished handshaking has no player list, and a server that does not answer ping has no latency.
 * Unavailable values are reported as absent rather than as zero.</p>
 */
public final class NetworkDiagnostics {

    private final MinecraftClient client;

    public NetworkDiagnostics(MinecraftClient client) {
        this.client = client;
    }

    /** True when connected to any server, including a single-player integrated one. */
    public boolean isInGame() {
        return client != null && client.world != null;
    }

    public boolean isSinglePlayer() {
        return client != null && client.isIntegratedServerRunning();
    }

    public boolean isMultiplayer() {
        return client != null && client.getNetworkHandler() != null && !client.isIntegratedServerRunning();
    }

    /** Server display name, or {@code null} when not connected to one. */
    public String serverName() {
        if (client == null) {
            return null;
        }
        if (client.isIntegratedServerRunning()) {
            return "Single-player world";
        }
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null && info.name != null && !info.name.isBlank()) {
            return info.name;
        }
        return null;
    }

    /** Server address without the port, or {@code null} when unavailable. */
    public String serverAddress() {
        if (client == null) {
            return null;
        }
        if (client.isIntegratedServerRunning()) {
            return "local";
        }
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null && info.address != null && !info.address.isBlank()) {
            return stripPort(info.address);
        }
        return null;
    }

    /**
     * Latency to the server in milliseconds, or {@code null} when unknown.
     *
     * <p>This is the round-trip time the server itself reports through the player list. It is a
     * property of the network path and the server, not of the internet connection's advertised
     * speed, and VortexMC does not claim otherwise.</p>
     */
    public Integer latencyMillis() {
        ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
        if (handler == null) {
            return null;
        }
        Collection<PlayerListEntry> players = handler.getPlayerList();
        if (players == null) {
            return null;
        }
        int lowest = Integer.MAX_VALUE;
        boolean found = false;
        for (PlayerListEntry entry : players) {
            if (entry == null) {
                continue;
            }
            int latency = entry.getLatency();
            if (latency >= 0 && latency < lowest) {
                lowest = latency;
                found = true;
            }
        }
        return found ? lowest : null;
    }

    /** Number of entries in the server's player list, or {@code null} when unavailable. */
    public Integer knownPlayerCount() {
        ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
        if (handler == null) {
            return null;
        }
        Collection<PlayerListEntry> players = handler.getPlayerList();
        return players == null ? null : players.size();
    }

    public String protocolVersion() {
        ServerInfo info = client == null ? null : client.getCurrentServerEntry();
        if (info != null && info.version != null) {
            String version = info.version.getString();
            if (version != null && !version.isBlank()) {
                return version;
            }
        }
        return null;
    }

    /** Entities currently loaded in the client world, or {@code null} when not in a world. */
    public Integer entityCount() {
        ClientWorld world = client == null ? null : client.world;
        if (world == null) {
            return null;
        }
        try {
            return world.getRegularEntityCount();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Entities within a radius of the player, counted by walking the client entity list.
     * Used to explain what the entity-distance setting actually affects.
     */
    public Integer nearbyEntityCount(double radius) {
        ClientWorld world = client == null ? null : client.world;
        if (world == null || client.player == null) {
            return null;
        }
        try {
            double px = client.player.getX();
            double py = client.player.getY();
            double pz = client.player.getZ();
            double squared = radius * radius;
            int count = 0;
            for (Entity entity : world.getEntities()) {
                if (entity == null) {
                    continue;
                }
                double dx = entity.getX() - px;
                double dy = entity.getY() - py;
                double dz = entity.getZ() - pz;
                if (dx * dx + dy * dy + dz * dz <= squared) {
                    count++;
                }
            }
            return count;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Average packets per second in each direction, or {@code null} when unavailable. */
    public Float[] packetRates() {
        ClientConnection connection = connection();
        if (connection == null) {
            return null;
        }
        try {
            return new Float[]{connection.getAveragePacketsSent(), connection.getAveragePacketsReceived()};
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean isConnectionOpen() {
        ClientConnection connection = connection();
        return connection != null && connection.isOpen();
    }

    private ClientConnection connection() {
        ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
        if (handler == null) {
            return null;
        }
        try {
            return handler.getConnection();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String stripPort(String address) {
        int slash = address.indexOf('/');
        String host = slash >= 0 ? address.substring(slash + 1) : address;
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(':') == colon) {
            return host.substring(0, colon);
        }
        return host;
    }
}
