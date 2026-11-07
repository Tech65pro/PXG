package com.tech65pro.vortexmc.client;

import com.tech65pro.vortexmc.config.VortexConfig;
import com.tech65pro.vortexmc.json.Json;
import com.tech65pro.vortexmc.json.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes {@link VortexConfig} to disk.
 *
 * <p>Saves are atomic: the new content is written to a sibling temporary file and then moved over
 * the real one, so a crash or a full disk in the middle of a save leaves the previous
 * configuration intact instead of a truncated file. A configuration that cannot be parsed at all
 * is treated as absent and the defaults are used; the game is never blocked from starting.</p>
 */
public final class ConfigStore {

    private static final String TEMP_SUFFIX = ".tmp";

    private final Path file;

    public ConfigStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    /** Loads the configuration, falling back to defaults when anything is wrong. */
    public VortexConfig load() {
        if (!exists()) {
            return new VortexConfig();
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) {
                return new VortexConfig();
            }
            JsonValue root = Json.parse(text);
            return VortexConfig.fromJson(root);
        } catch (IOException | RuntimeException e) {
            // A corrupt config must not stop the game from starting. Keep a copy for diagnosis
            // and start from defaults.
            backupUnreadable();
            return new VortexConfig();
        }
    }

    /** Writes the configuration atomically. Returns true on success. */
    public boolean save(VortexConfig config) {
        if (config == null) {
            return false;
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String text = Json.write(config.toJson(), true);
            Path temp = file.resolveSibling(file.getFileName() + TEMP_SUFFIX);
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private void backupUnreadable() {
        try {
            Path backup = file.resolveSibling(file.getFileName() + ".broken");
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException ignored) {
            // Nothing useful can be done if even the backup fails.
        }
    }
}
