package com.exoticworlds.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.compat.MapCopies;

public final class SettingsFile {
    public static final String MAP_COPIES_KEY = "map_copies";

    private static final String MAP_COPIES_COMMENT = """
            How a map mod's fullscreen map draws a looped world: REPEATED fills the window with
            the world's copies, SINGLE draws one copy and stops panning and zooming out at its
            edges. Minimaps always repeat it.
            Allowed values: REPEATED, SINGLE.""";

    private static final Gson GSON = new Gson();
    private static final String INDENT = "  ";
    private static final String COMMENT_MARK = "// ";

    public static Settings load(Path file) {
        Settings defaults = Settings.defaults();
        if (!Files.isRegularFile(file)) {
            save(file, defaults);
            return defaults;
        }

        JsonObject json = read(file);
        if (json == null) {
            return defaults;
        }

        return new Settings(mapCopies(json, defaults.mapCopies()));
    }

    public static void save(Path file, Settings settings) {
        String entry = entry(MAP_COPIES_KEY, new JsonPrimitive(settings.mapCopies().name()), MAP_COPIES_COMMENT);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(file, "{\n" + entry + "\n}\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            ExoticWorlds.LOGGER.error("Could not write the settings file {}", file, e);
        }
    }

    private static String entry(String key, JsonPrimitive value, String comment) {
        StringBuilder line = new StringBuilder();
        comment.lines().forEach(text -> line.append(INDENT).append(COMMENT_MARK).append(text).append('\n'));
        return line.append(INDENT)
                .append(GSON.toJson(new JsonPrimitive(key)))
                .append(": ")
                .append(GSON.toJson(value))
                .toString();
    }

    private static JsonObject read(Path file) {
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }

            ExoticWorlds.LOGGER.warn("The settings file {} is not a JSON object; every setting falls back to its default", file);
        } catch (IOException | RuntimeException e) {
            ExoticWorlds.LOGGER.warn("Could not read the settings file {}; every setting falls back to its default", file, e);
        }

        return null;
    }

    private static MapCopies mapCopies(JsonObject json, MapCopies fallback) {
        JsonElement value = json.get(MAP_COPIES_KEY);
        MapCopies parsed = value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? MapCopies.fromName(value.getAsString()).orElse(null)
                : null;
        if (parsed != null) {
            return parsed;
        }

        ExoticWorlds.LOGGER.warn("Setting {} is {}; falling back to {}", MAP_COPIES_KEY,
                value == null ? "missing" : value, fallback);
        return fallback;
    }

    private SettingsFile() {
    }
}
