package com.exoticworlds.compat.vista;

import java.util.function.BiPredicate;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.ChunkPos;

public final class ChainedTvSeat {
    public static BiPredicate<Integer, Integer> beside(WorldFold fold, ChunkPos viewFinder,
            BiPredicate<Integer, Integer> nearViewFinder) {
        return (chunkX, chunkZ) -> {
            ChunkPos seat = fold.nearestCopy(viewFinder, new ChunkPos(chunkX, chunkZ));
            return nearViewFinder.test(seat.x, seat.z);
        };
    }

    private ChainedTvSeat() {
    }
}
