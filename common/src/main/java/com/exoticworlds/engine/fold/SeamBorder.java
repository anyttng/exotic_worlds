package com.exoticworlds.engine.fold;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.phys.Vec3;

public sealed interface SeamBorder permits LatticeBorder, CuboidBorder {
    double CLAMP_INSET = 1.0E-5F;

    static SeamBorder of(WorldFold fold, double minX, double maxX, double minZ, double maxZ) {
        return fold.getCuboidAtlas() == null
                ? new LatticeBorder(fold.getBlockLattice(), minX, maxX, minZ, maxZ)
                : new CuboidBorder(fold, minX, maxX, minZ, maxZ);
    }

    boolean isInside(double x, double z, double marginBlocks);

    double distanceToEdgeBlocks(double x, double z);

    Vec3 clamped(double x, double y, double z);
}
