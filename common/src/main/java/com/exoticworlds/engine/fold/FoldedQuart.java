package com.exoticworlds.engine.fold;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;

public final class FoldedQuart {
    public static long fold(WorldFold fold, int quartX, int quartY, int quartZ) {
        long folded = fold.foldBlockNode(
                BlockPos.asLong(QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ)));
        return BlockPos.asLong(QuartPos.fromBlock(BlockPos.getX(folded)), quartY,
                QuartPos.fromBlock(BlockPos.getZ(folded)));
    }

    public static int x(long quartNode) {
        return BlockPos.getX(quartNode);
    }

    public static int z(long quartNode) {
        return BlockPos.getZ(quartNode);
    }

    private FoldedQuart() {
    }
}
