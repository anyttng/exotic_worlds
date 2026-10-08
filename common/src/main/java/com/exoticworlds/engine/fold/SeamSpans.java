package com.exoticworlds.engine.fold;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class SeamSpans {
    public static boolean crossesSeam(WorldFold fold, BoundingBox region) {
        return !acrossDiagonal(fold, region).equals(region) || !acrossAntiDiagonal(fold, region).equals(region);
    }

    public static BoundingBox foldAcrossSeam(WorldFold fold, BoundingBox region) {
        BoundingBox across = acrossDiagonal(fold, region);
        return across.equals(region) ? acrossAntiDiagonal(fold, region) : across;
    }

    private static BoundingBox acrossDiagonal(WorldFold fold, BoundingBox region) {
        return seatedOn(fold, region,
                new BlockPos(region.maxX(), region.minY(), region.maxZ()),
                new BlockPos(region.minX(), region.maxY(), region.minZ()));
    }

    private static BoundingBox acrossAntiDiagonal(WorldFold fold, BoundingBox region) {
        return seatedOn(fold, region,
                new BlockPos(region.maxX(), region.minY(), region.minZ()),
                new BlockPos(region.minX(), region.maxY(), region.maxZ()));
    }

    private static BoundingBox seatedOn(WorldFold fold, BoundingBox region, BlockPos anchor, BlockPos corner) {
        BlockPos seated = fold.nearestCopy(anchor, corner);
        return seated.equals(corner) ? region : BoundingBox.fromCorners(anchor, seated);
    }

    private SeamSpans() {
    }
}
