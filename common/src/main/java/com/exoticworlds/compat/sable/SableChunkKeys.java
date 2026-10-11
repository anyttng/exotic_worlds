package com.exoticworlds.compat.sable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class SableChunkKeys {
    private static final String REGION_FILE_PREFIX = "r.";
    private static final String REGION_FILE_SEPARATOR = ".";

    public static ChunkPos physical(ServerLevel level, ChunkPos raw) {
        return physical(WorldLoopAttachments.transformerOf(level), raw);
    }

    public static ChunkPos physical(WorldFold fold, ChunkPos raw) {
        return fold.fold(raw);
    }

    public static long physical(ServerLevel level, long rawKey) {
        return physical(WorldLoopAttachments.transformerOf(level), rawKey);
    }

    public static long physical(WorldFold fold, long rawKey) {
        return fold.foldChunkKey(rawKey);
    }

    public static List<ChunkPos> rawAliases(WorldFold fold, ChunkPos physical) {
        TranslationLattice lattice = fold.chunkLattice();
        long first = lattice.nearFirst(0.0, 0.0);
        long second = lattice.nearSecond(0.0, 0.0);
        int count = lattice.nearCount();
        List<ChunkPos> aliases = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long shiftX = lattice.nearShiftX(first, second, index);
            long shiftZ = lattice.nearShiftZ(first, second, index);
            if (shiftX != 0L || shiftZ != 0L) {
                aliases.add(new ChunkPos(physical.x + Math.toIntExact(shiftX), physical.z + Math.toIntExact(shiftZ)));
            }
        }

        return aliases;
    }

    public static Path regionFile(Path folder, ChunkPos chunk) {
        return folder.resolve(REGION_FILE_PREFIX + chunk.getRegionX() + REGION_FILE_SEPARATOR + chunk.getRegionZ()
                + SubLevelRegionFile.FILE_EXTENSION);
    }

    private SableChunkKeys() {
    }
}
