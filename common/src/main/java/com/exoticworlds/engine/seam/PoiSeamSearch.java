package com.exoticworlds.engine.seam;

import java.util.Comparator;
import java.util.stream.Stream;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class PoiSeamSearch {
    public static Stream<ChunkPos> chunksAround(WorldFold fold, ChunkPos center, int radiusChunks) {
        Stream<ChunkPos> wrapped = ChunkPos.rangeClosed(center, radiusChunks)
                .map(fold::fold);

        if (!isFoldedOntoItself(fold, radiusChunks)) {
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

    private static boolean isFoldedOntoItself(WorldFold fold, int radiusChunks) {
        int spanChunks = radiusChunks * 2 + 1;
        TranslationLattice chunks = fold.getChunkLattice();
        return chunks.getX().isFoldedOntoItself(spanChunks) || chunks.getZ().isFoldedOntoItself(spanChunks);
    }

    private PoiSeamSearch() {
    }
}
