package com.tech65pro.vortexmc.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Entry point for VortexMC's JSON layer. */
public final class Json {

    private Json() {
    }

    public static JsonValue parse(String text) {
        return JsonReader.parse(text);
    }

    public static JsonValue parse(Reader reader) throws IOException {
        return JsonReader.parse(reader);
    }

    public static JsonValue parseFile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonReader.parse(reader);
        }
    }

    /** Returns {@code null} when the file does not exist or cannot be parsed. */
    public static JsonValue parseFileQuietly(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            return parseFile(path);
        } catch (RuntimeException | IOException e) {
            return null;
        }
    }

    public static String write(JsonValue value, boolean pretty) {
        StringWriter writer = new StringWriter();
        JsonWriter.write(value, writer, pretty);
        return writer.toString();
    }

    public static void writeFile(Path path, JsonValue value) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String text = write(value, true) + "\n";
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    public static void writeFileQuietly(Path path, JsonValue value) {
        try {
            writeFile(path, value);
        } catch (IOException | RuntimeException e) {
            // Configuration persistence is best-effort: never crash the game loop for it.
        }
    }
}
