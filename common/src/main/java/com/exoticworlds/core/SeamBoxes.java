package com.exoticworlds.core;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SeamBoxes {
    private SeamBoxes() {}

    static boolean crosses(WrapDomain x, WrapDomain z, AABB box) {
        return !x.containsSpan(box.minX, box.maxX) || !z.containsSpan(box.minZ, box.maxZ);
    }

    static boolean crosses(WrapDomain x, WrapDomain z, BoundingBox region) {
        return x.isOver(region.minX()) || x.isOver(region.maxX())
                || z.isOver(region.minZ()) || z.isOver(region.maxZ());
    }

    static double centre(double min, double max) {
        return (min + max) / 2.0;
    }

    static double sqrYGap(AABB box, Vec3 point) {
        double yGap = Math.max(Math.max(box.minY - point.y, point.y - box.maxY), 0.0);
        return yGap * yGap;
    }
}
