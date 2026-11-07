package com.tech65pro.vortexmc.client;

import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Detects other installed mods so VortexMC can adapt to them instead of fighting them.
 *
 * <p>Only {@link FabricLoader#isModLoaded(String)} is used: VortexMC reads no other mod's classes,
 * copies none of their code, and never reflects into them. The information is used purely to
 * explain which of VortexMC's changes will and will not have an effect.</p>
 */
public final class CompatibilityDetector {

    /** Mods that change how the renderer works and therefore overlap with VortexMC. */
    private static final String[] RENDERER_MODS = {
            "sodium", "embeddium", "rubidium", "oculus", "iris", "indium", "morefps", "betterfps"
    };

    /** Mods that rewrite game internals. */
    private static final String[] ENGINE_MODS = {
            "lithium", "ferritecore", "starlight", "krypton", "alternate-current", "dynamic_fps"
    };

    /** Mods that add their own dashboards or overlays. */
    private static final String[] HUD_MODS = {
            "modmenu", "essential", "replaymod", "wi-zoom", "cotton-resources"
    };

    private final FabricLoader loader;

    public CompatibilityDetector(FabricLoader loader) {
        this.loader = loader;
    }

    public boolean isLoaded(String modId) {
        try {
            return loader != null && loader.isModLoaded(modId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean anyRendererModPresent() {
        return anyPresent(RENDERER_MODS);
    }

    public boolean anyEngineModPresent() {
        return anyPresent(ENGINE_MODS);
    }

    /** Mods that are installed, grouped by category, for display. */
    public Map<String, java.util.List<String>> presentByCategory() {
        Map<String, java.util.List<String>> result = new LinkedHashMap<>();
        result.put("Renderer replacements", present(RENDERER_MODS));
        result.put("Engine optimizations", present(ENGINE_MODS));
        result.put("Interface / HUD", present(HUD_MODS));
        return result;
    }

    /**
     * A short explanation of what VortexMC's changes will do given what is installed.
     * Honest by construction: it says what is known and does not guess at interactions.
     */
    public String interactionNote() {
        if (isLoaded("sodium")) {
            return "Sodium is installed. It replaces the chunk renderer, so render distance and "
                    + "particle changes still apply, but chunk-builder work is already handled by "
                    + "Sodium. VortexMC only touches options the game itself owns.";
        }
        if (isLoaded("embeddium") || isLoaded("rubidium")) {
            return "A Sodium-family renderer is installed. VortexMC changes the same vanilla "
                    + "options that renderer reads; it does not hook the renderer itself.";
        }
        if (anyRendererModPresent()) {
            return "Another renderer mod is installed. VortexMC only changes vanilla video "
                    + "options, so any option that mod overrides takes precedence.";
        }
        if (anyEngineModPresent()) {
            return "Engine optimization mods are installed. VortexMC's settings changes are "
                    + "independent of them and can be combined freely.";
        }
        return "No known renderer or engine optimization mods detected. VortexMC is working with "
                + "the vanilla renderer.";
    }

    private boolean anyPresent(String[] ids) {
        for (String id : ids) {
            if (isLoaded(id)) {
                return true;
            }
        }
        return false;
    }

    private java.util.List<String> present(String[] ids) {
        java.util.List<String> found = new java.util.ArrayList<>();
        for (String id : ids) {
            if (isLoaded(id)) {
                found.add(id);
            }
        }
        return found;
    }
}
