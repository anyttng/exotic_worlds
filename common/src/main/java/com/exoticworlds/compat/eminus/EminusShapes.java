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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class EminusShapes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MARKER_FILE = "exotic_worlds_shape";
    private static final String UNMARKED = "none";
    private static final List<String> STORE_SUFFIXES = List.of("", "-wal", "-shm");

    public static CellFold publishedFold(String dimension) {
        Identifier id = Identifier.tryParse(dimension);
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
            if (written.equals(fold.signature())) {
                return;
            }

            for (String suffix : STORE_SUFFIXES) {
                Files.deleteIfExists(folder.resolve(SqliteCellStore.FILE_NAME + suffix));
            }

            Files.createDirectories(folder);
            Files.writeString(marker, fold.signature(), StandardCharsets.UTF_8);
            LOGGER.info("[eminus-compat] store_reset folder={} written={} shape={}", folder, written, fold);
        } catch (IOException failure) {
            LOGGER.error("[eminus-compat] store_reset_failed folder={} shape={}", folder, fold, failure);
        }
    }

    private EminusShapes() {
    }
}
