package com.exoticworlds.engine.seam;

import java.util.Comparator;
import java.util.stream.Stream;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.world.CoordinateConstants;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class PoiSeamSearch {
    private static final int ANY_Y = 0;

    public static Stream<ChunkPos> chunksAround(WorldFold fold, ChunkPos center, int radiusChunks) {
        Stream<ChunkPos> wrapped = ChunkPos.rangeClosed(center, radiusChunks)
                .map(fold::fold);

        if (!fold.isFoldedOntoItself(blocksOf(center, radiusChunks))) {
            return wrapped;
        }

        LongSet seen = new LongOpenHashSet();
        return wrapped.filter(chunkPos -> seen.add(chunkPos.pack()));
    }

    public static boolean isWithinSquare(WorldFold fold, BlockPos center, int radiusBlocks, BlockPos pos) {
        BlockPos nearest = fold.nearestCopy(center, pos);
        return Math.abs(nearest.getX() - center.getX()) <= radiusBlocks
                && Math.abs(nearest.getZ() - center.getZ()) <= radiusBlocks;
    }

    public static Comparator<BlockPos> byDistance(WorldFold fold, BlockPos center) {
        return Comparator.comparingDouble(pos -> SeamRange.sqr(fold, center, pos));
    }

    private static BoundingBox blocksOf(ChunkPos center, int radiusChunks) {
        return new BoundingBox(
                SectionPos.sectionToBlockCoord(center.x() - radiusChunks),
                ANY_Y,
                SectionPos.sectionToBlockCoord(center.z() - radiusChunks),
                SectionPos.sectionToBlockCoord(center.x() + radiusChunks, CoordinateConstants.CHUNK_WIDTH_BLOCKS - 1),
                ANY_Y,
                SectionPos.sectionToBlockCoord(center.z() + radiusChunks, CoordinateConstants.CHUNK_WIDTH_BLOCKS - 1));
    }

    private PoiSeamSearch() {
    }
}
