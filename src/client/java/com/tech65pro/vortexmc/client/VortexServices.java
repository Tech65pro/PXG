package com.tech65pro.vortexmc.client;

import com.tech65pro.vortexmc.config.VortexConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;

/**
 * The single object that holds everything VortexMC's client side needs.
 *
 * <p>The dashboard and the tick handlers all read through this rather than reaching for static
 * state, which keeps the data flow obvious and makes the pieces individually replaceable.</p>
 */
public final class VortexServices {

    private static volatile VortexServices instance;

    public final VortexConfig config;
    public final ConfigStore configStore;
    public final SettingMapper settingMapper;
    public final PerformanceMonitor performanceMonitor;
    public final SmartAutoController smartAuto;
    public final MemoryDiagnostics memoryDiagnostics;
    public final NetworkDiagnostics networkDiagnostics;
    public final CompatibilityDetector compatibility;

    private final MinecraftClient client;
    private boolean dirty;

    private VortexServices(MinecraftClient client) {
        this.client = client;
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("vortexmc.json");
        this.configStore = new ConfigStore(configPath);
        this.config = configStore.load();
        this.settingMapper = new SettingMapper(client);
        this.performanceMonitor = new PerformanceMonitor(client);
        this.smartAuto = new SmartAutoController(settingMapper, performanceMonitor, config);
        this.memoryDiagnostics = new MemoryDiagnostics();
        this.networkDiagnostics = new NetworkDiagnostics(client);
        this.compatibility = new CompatibilityDetector(FabricLoader.getInstance());
    }

    /** Creates the services if needed and returns them. */
    public static VortexServices initialize(MinecraftClient client) {
        if (instance == null) {
            synchronized (VortexServices.class) {
                if (instance == null) {
                    instance = new VortexServices(client);
                }
            }
        }
        return instance;
    }

    public static VortexServices get() {
        return instance;
    }

    public MinecraftClient client() {
        return client;
    }

    /** Marks the configuration as needing a save. */
    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Writes the configuration if it changed. Safe to call often; it is a no-op otherwise. */
    public boolean saveIfDirty() {
        if (!dirty) {
            return false;
        }
        dirty = false;
        return configStore.save(config);
    }

    /** Writes the configuration unconditionally. */
    public boolean save() {
        dirty = false;
        return configStore.save(config);
    }
}
