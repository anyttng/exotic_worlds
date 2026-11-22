package com.exoticworlds.engine.seam;

import java.util.Comparator;
import java.util.stream.Stream;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class PoiSeamSearch {
    private static final int ANY_Y = 0;

    public static Stream<ChunkPos> chunksAround(WorldFold fold, ChunkPos center, int chunkRadius) {
        Stream<ChunkPos> wrapped = ChunkPos.rangeClosed(center, chunkRadius)
                .map(fold::fold);

        if (!fold.foldsOntoItself(blocksOf(center, chunkRadius))) {
            return wrapped;
        }

        LongSet seen = new LongOpenHashSet();
        return wrapped.filter(chunkPos -> seen.add(chunkPos.pack()));
    }

    public static boolean withinSquare(WorldFold fold, BlockPos center, int radius, BlockPos pos) {
        BlockPos nearest = fold.nearestCopy(center, pos);
        return Math.abs(nearest.getX() - center.getX()) <= radius
                && Math.abs(nearest.getZ() - center.getZ()) <= radius;
    }

    public static Comparator<BlockPos> byDistance(WorldFold fold, BlockPos center) {
        return Comparator.comparingDouble(pos -> SeamRange.sqr(fold, center, pos));
    }

    private static BoundingBox blocksOf(ChunkPos center, int chunkRadius) {
        return new BoundingBox(
                SectionPos.sectionToBlockCoord(center.x() - chunkRadius),
                ANY_Y,
                SectionPos.sectionToBlockCoord(center.z() - chunkRadius),
                SectionPos.sectionToBlockCoord(center.x() + chunkRadius, CoordinateConstants.CHUNK_WIDTH - 1),
                ANY_Y,
                SectionPos.sectionToBlockCoord(center.z() + chunkRadius, CoordinateConstants.CHUNK_WIDTH - 1));
    }

    private PoiSeamSearch() {
    }
}
