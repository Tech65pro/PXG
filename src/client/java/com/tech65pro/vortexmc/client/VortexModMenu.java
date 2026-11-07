package com.tech65pro.vortexmc.client;

import com.tech65pro.vortexmc.client.screen.DashboardScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Opens the VortexMC dashboard from ModMenu's mod list.
 *
 * <p>ModMenu is an optional dependency: this class is only referenced from the {@code modmenu}
 * entrypoint in fabric.mod.json, so it is never loaded when ModMenu is absent.</p>
 */
public class VortexModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            VortexServices services = VortexServices.get();
            return services == null ? parent : new DashboardScreen(services);
        };
    }
}
