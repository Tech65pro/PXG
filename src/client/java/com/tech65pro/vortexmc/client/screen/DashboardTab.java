package com.tech65pro.vortexmc.client.screen;

import net.minecraft.client.gui.DrawContext;

/** One tab of the VortexMC dashboard. */
public interface DashboardTab {

    /** Label shown in the tab strip. */
    String title();

    /**
     * Adds the tab's widgets. Called whenever the tab becomes active or the layout changes, so a
     * tab must be able to rebuild itself from scratch.
     */
    void build(DashboardScreen screen);

    /** Draws anything that is not a widget: graphs, bars, wrapped text. */
    void render(DashboardScreen screen, DrawContext context, int mouseX, int mouseY, float delta);

    /** Handles a scroll event that happened over the tab's content area. */
    default void scroll(DashboardScreen screen, double amount) {
    }

    /** Called when the tab becomes active, to refresh cached values. */
    default void onSelected() {
    }
}
