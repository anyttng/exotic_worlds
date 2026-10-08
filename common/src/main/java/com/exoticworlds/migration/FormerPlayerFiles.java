package com.exoticworlds.migration;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.exoticworlds.ExoticWorlds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

public final class FormerPlayerFiles {
    private static final String PLAYER_DATA_SUFFIX = ".dat";
    private static final String PLAYER_DATA_BACKUP_SUFFIX = ".dat_old";
    private static final String ADVANCEMENTS_SUFFIX = ".json";
    private static final String TEMP_SUFFIX = ".migrating";
    private static final String REPLACED_SUFFIX = ".migrated";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @FunctionalInterface
    private interface Rewrite {
        boolean rewrite(Path source, Path target) throws IOException;
    }

    public static void migrate(Path playerData, Path advancements) {
        rewriteAll(playerData, FormerPlayerFiles::isPlayerData, FormerPlayerFiles::rewritePlayerData);
        rewriteAll(advancements, file -> nameOf(file).endsWith(ADVANCEMENTS_SUFFIX),
                FormerPlayerFiles::rewriteAdvancements);
    }

    static boolean rewritePlayerData(Path source, Path target) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(source, NbtAccounter.unlimitedHeap());
        List<String> former = tag.getAllKeys().stream().filter(key -> FormerNamespace.twinKeyOf(key) != null).toList();
        if (former.isEmpty()) {
            return false;
        }

        for (String key : former) {
            Tag value = tag.get(key);
            tag.remove(key);
            String twin = FormerNamespace.twinKeyOf(key);
            if (!tag.contains(twin)) {
                tag.put(twin, value);
            }
        }

        NbtIo.writeCompressed(tag, target);
        return true;
    }

    static boolean rewriteAdvancements(Path source, Path target) throws IOException {
        JsonElement json;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(source, StandardCharsets.UTF_8))) {
            reader.setLenient(false);
            json = Streams.parse(reader);
        }

        if (!(json instanceof JsonObject advancements)) {
            return false;
        }

        List<String> former = advancements.keySet().stream()
                .filter(key -> FormerNamespace.twinKeyOf(key) != null)
                .toList();
        if (former.isEmpty()) {
            return false;
        }

        for (String key : former) {
            JsonElement value = advancements.remove(key);
            String twin = FormerNamespace.twinKeyOf(key);
            if (!advancements.has(twin)) {
                advancements.add(twin, value);
            }
        }

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            GSON.toJson(advancements, GSON.newJsonWriter(writer));
        }

        return true;
    }

    private static void rewriteAll(Path directory, Predicate<Path> wanted, Rewrite rewrite) {
        if (!Files.isDirectory(directory)) {
            return;
        }

        List<Path> files;
        try (Stream<Path> listed = Files.list(directory)) {
            files = listed.filter(Files::isRegularFile).filter(wanted).toList();
        } catch (IOException unlisted) {
            ExoticWorlds.LOGGER.warn("[migration] cannot list {}: {}", directory, unlisted.toString());
            return;
        }

        for (Path file : files) {
            rewriteOne(file, rewrite);
        }
    }

    private static void rewriteOne(Path file, Rewrite rewrite) {
        Path temp = file.resolveSibling(nameOf(file) + TEMP_SUFFIX);
        Path replaced = file.resolveSibling(nameOf(file) + REPLACED_SUFFIX);
        try {
            if (!rewrite.rewrite(file, temp)) {
                return;
            }

            if (Util.safeReplaceOrMoveFile(file, temp, replaced, false)) {
                Files.deleteIfExists(replaced);
            } else {
                ExoticWorlds.LOGGER.warn("[migration] cannot replace {}; it keeps its {} ids", file,
                        FormerNamespace.NAMESPACE);
            }
        } catch (IOException | RuntimeException unreadable) {
            ExoticWorlds.LOGGER.warn("[migration] cannot rewrite {}: {}", file, unreadable.toString());
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException undeletable) {
                ExoticWorlds.LOGGER.warn("[migration] cannot delete {}", temp);
            }
        }
    }

    private static boolean isPlayerData(Path file) {
        String name = nameOf(file);
        return name.endsWith(PLAYER_DATA_SUFFIX) || name.endsWith(PLAYER_DATA_BACKUP_SUFFIX);
    }

    private static String nameOf(Path file) {
        return file.getFileName().toString();
    }

    private FormerPlayerFiles() {
    }
}
