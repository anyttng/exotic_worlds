package com.exoticworlds.core.fold;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public final class SeamBoxes {
    private SeamBoxes() {}

    public static boolean isCrossing(WrapDomain x, WrapDomain z, AABB box) {
        return !x.isSpanInside(box.minX, box.maxX) || !z.isSpanInside(box.minZ, box.maxZ);
    }

    public static boolean isCrossing(WrapDomain x, WrapDomain z, BoundingBox region) {
        return x.isOver(region.minX()) || x.isOver(region.maxX())
                || z.isOver(region.minZ()) || z.isOver(region.maxZ());
    }

    public static double center(double min, double max) {
        return (min + max) / 2.0;
    }
}
