package com.exoticworlds.core;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

final class SeamBoxes {
    private SeamBoxes() {}

    static boolean crosses(WrapDomain x, WrapDomain z, AABB box) {
        return !x.containsSpan(box.minX, box.maxX) || !z.containsSpan(box.minZ, box.maxZ);
    }

    static boolean crosses(WrapDomain x, WrapDomain z, BoundingBox region) {
        return x.isOver(region.minX()) || x.isOver(region.maxX())
                || z.isOver(region.minZ()) || z.isOver(region.maxZ());
    }

    static double center(double min, double max) {
        return (min + max) / 2.0;
    }
}
