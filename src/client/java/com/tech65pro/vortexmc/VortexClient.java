package com.tech65pro.vortexmc;

import com.tech65pro.vortexmc.client.OverlayRenderer;
import com.tech65pro.vortexmc.client.VortexKeys;
import com.tech65pro.vortexmc.client.VortexServices;
import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * VortexMC client entry point.
 *
 * <p>Everything is wired here once and then left alone: one keybinding, one tick listener, one HUD
 * element. There are no background threads and no per-frame allocations, so the mod adds no
 * measurable overhead of its own.</p>
 */
public class VortexClient implements ClientModInitializer {

    private static final Identifier OVERLAY_ID = Identifier.of("vortexmc", "overlay");

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        VortexServices services = VortexServices.initialize(client);
        VortexKeys.register();

        // One HUD element does two jobs: it samples a real frame time every rendered frame, and it
        // draws the overlay. Both only run while the HUD is on screen, which is exactly when the
        // numbers are meaningful.
        OverlayRenderer overlay = new OverlayRenderer(client, services.performanceMonitor);
        HudElementRegistry.addLast(OVERLAY_ID, (context, tickCounter) -> {
            services.performanceMonitor.onFrame();
            if (services.config.showOverlay() && client.world != null && client.currentScreen == null) {
                overlay.render(context);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(game -> {
            long now = System.currentTimeMillis();
            services.smartAuto.tick(now);
            services.saveIfDirty();
        });

        ClientTickEvents.START_CLIENT_TICK.register(game -> {
            if (VortexKeys.openDashboard != null && VortexKeys.openDashboard.wasPressed()) {
                game.setScreen(new DashboardScreen(services));
            } else if (VortexKeys.toggleOverlay != null && VortexKeys.toggleOverlay.wasPressed()) {
                services.config.showOverlay(!services.config.showOverlay());
                services.markDirty();
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(game -> services.save());
    }
}
