package com.exoticworlds.compat.eminus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;

import com.eminus.store.SqliteCellStore;
import com.exoticworlds.client.engine.PublishedShapes;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class EminusShapes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MARKER_FILE = "exotic_worlds_shape";
    private static final String UNMARKED = "none";
    private static final String ABSENT = "absent";
    private static final String FIELD_SEPARATOR = " ";
    private static final String SIZE_TIME_SEPARATOR = "@";
    private static final List<String> STORE_SUFFIXES = List.of("", "-wal", "-shm");

    public static CellFold publishedFold(String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        return id == null
                ? CellFold.NONE
                : CellFold.of(PublishedShapes.foldOf(ResourceKey.create(Registries.DIMENSION, id)));
    }

    public static void matchStore(Path folder, CellFold fold) {
        if (!fold.repeats()) {
            return;
        }

        Path marker = folder.resolve(MARKER_FILE);
        try {
            String written = Files.exists(marker) ? Files.readString(marker, StandardCharsets.UTF_8).strip() : UNMARKED;
            if (written.equals(stamp(folder, fold))) {
                return;
            }

            for (String suffix : STORE_SUFFIXES) {
                Files.deleteIfExists(folder.resolve(SqliteCellStore.FILE_NAME + suffix));
            }

            Files.createDirectories(folder);
            Files.writeString(marker, stamp(folder, fold), StandardCharsets.UTF_8);
            LOGGER.info("[ew/eminus-compat] store_reset folder={} written={} shape={}", folder, written, fold);
        } catch (IOException failure) {
            LOGGER.error("[ew/eminus-compat] store_reset_failed folder={} shape={}", folder, fold, failure);
        }
    }

    public static void stampStore(Path folder, CellFold fold) {
        if (!fold.repeats()) {
            return;
        }

        try {
            Files.writeString(folder.resolve(MARKER_FILE), stamp(folder, fold), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            LOGGER.error("[ew/eminus-compat] store_stamp_failed folder={} shape={}", folder, fold, failure);
        }
    }

    private static String stamp(Path folder, CellFold fold) throws IOException {
        StringBuilder stamp = new StringBuilder(fold.signature());
        for (String suffix : STORE_SUFFIXES) {
            Path file = folder.resolve(SqliteCellStore.FILE_NAME + suffix);
            stamp.append(FIELD_SEPARATOR).append(Files.exists(file)
                    ? Files.size(file) + SIZE_TIME_SEPARATOR + Files.getLastModifiedTime(file).toMillis()
                    : ABSENT);
        }

        return stamp.toString();
    }

    private EminusShapes() {
    }
}
