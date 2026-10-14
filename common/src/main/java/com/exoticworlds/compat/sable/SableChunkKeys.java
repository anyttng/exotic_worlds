package com.exoticworlds.compat.sable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public static List<ChunkPos> rawAliases(WorldFold fold, ChunkPos physical) {
        long[] shifts = fold.chunkLattice().nearShifts(0.0, 0.0);
        List<ChunkPos> aliases = new ArrayList<>(shifts.length / 2);
        for (int index = 0; index < shifts.length; index += 2) {
            if (shifts[index] != 0L || shifts[index + 1] != 0L) {
                aliases.add(new ChunkPos(physical.x + Math.toIntExact(shifts[index]), physical.z + Math.toIntExact(shifts[index + 1])));
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
