package com.exoticworlds.engine.seam;

import java.util.Comparator;
import java.util.stream.Stream;

import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class PoiSeamSearch {
    public static Stream<ChunkPos> chunksAround(WorldFold fold, ChunkPos center, int chunkRadius) {
        Stream<ChunkPos> wrapped = ChunkPos.rangeClosed(center, chunkRadius)
                .map(pos -> fold.isOver(pos) ? fold.fold(pos) : pos);

        if (!foldsOntoItself(fold, chunkRadius)) {
            return wrapped;
        }

        LongSet seen = new LongOpenHashSet();
        return wrapped.filter(chunkPos -> seen.add(chunkPos.toLong()));
    }

    public static boolean withinSquare(WorldFold fold, BlockPos center, int radius, BlockPos pos) {
        BlockPos nearest = fold.nearestCopy(center, pos);
        return Math.abs(nearest.getX() - center.getX()) <= radius
                && Math.abs(nearest.getZ() - center.getZ()) <= radius;
    }

    public static Comparator<BlockPos> byDistance(WorldFold fold, BlockPos center) {
        return Comparator.comparingDouble(pos -> SeamRange.sqr(fold, center, pos));
    }

    private static boolean foldsOntoItself(WorldFold fold, int chunkRadius) {
        int span = chunkRadius * 2 + 1;
        return fold.bounds().x().foldsOntoItself(span) || fold.bounds().z().foldsOntoItself(span);
    }

    private PoiSeamSearch() {
    }
}
