package com.exoticworlds.engine.fold;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class ChunkSeat {
    public static BlockPos onto(WorldFold fold, ChunkPos own, BlockPos pos) {
        if (sitsIn(own, pos)) {
            return pos;
        }

        BlockPos nearest = fold.nearestCopy(own.getMiddleBlockPosition(pos.getY()), pos);
        return sitsIn(own, nearest) ? nearest : pos;
    }

    public static boolean sitsIn(ChunkPos own, int x, int z) {
        return SectionPos.blockToSectionCoord(x) == own.x() && SectionPos.blockToSectionCoord(z) == own.z();
    }

    private static boolean sitsIn(ChunkPos own, BlockPos pos) {
        return sitsIn(own, pos.getX(), pos.getZ());
    }

    private ChunkSeat() {
    }
}
