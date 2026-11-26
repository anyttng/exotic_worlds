package com.exoticworlds.engine.fold;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class ChunkSeat {
    public static BlockPos onto(WorldFold fold, ChunkPos own, BlockPos pos) {
        if (isSittingIn(own, pos)) {
            return pos;
        }

        BlockPos nearest = fold.nearestCopy(own.getMiddleBlockPosition(pos.getY()), pos);
        return isSittingIn(own, nearest) ? nearest : pos;
    }

    public static boolean isSittingIn(ChunkPos own, int x, int z) {
        return SectionPos.blockToSectionCoord(x) == own.x() && SectionPos.blockToSectionCoord(z) == own.z();
    }

    private static boolean isSittingIn(ChunkPos own, BlockPos pos) {
        return isSittingIn(own, pos.getX(), pos.getZ());
    }

    private ChunkSeat() {
    }
}
