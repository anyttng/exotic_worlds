package com.exoticworlds.engine.fold;

import com.exoticworlds.core.FoldOrientation;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;

public final class FoldedQuart {
    private static final double BLOCK_IN_QUARTS = 1.0 / QuartPos.SIZE;

    public static long fold(WorldFold fold, int quartX, int quartY, int quartZ) {
        long folded = fold.foldBlockNode(
                BlockPos.asLong(QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ)));
        return BlockPos.asLong(QuartPos.fromBlock(BlockPos.getX(folded)), quartY,
                QuartPos.fromBlock(BlockPos.getZ(folded)));
    }

    public static FoldOrientation orientation(WorldFold fold, int quartX, int quartY, int quartZ) {
        if (fold.preservesLocalIndices()) {
            return FoldOrientation.IDENTITY;
        }

        return fold.foldOriented(
                new BlockPos(QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ)))
                .orientation();
    }

    public static double distanceX(FoldOrientation orientation, double distanceX, double distanceZ) {
        return turned(orientation.flipsX(), orientation.swapsAxes() ? distanceZ : distanceX);
    }

    public static double distanceZ(FoldOrientation orientation, double distanceX, double distanceZ) {
        return turned(orientation.flipsZ(), orientation.swapsAxes() ? distanceX : distanceZ);
    }

    private static double turned(boolean flips, double distance) {
        return flips ? -distance - BLOCK_IN_QUARTS : distance;
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
