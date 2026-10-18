package com.exoticworlds.compat.sable;

import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SableSeamQuery {
    private static final double HALF = 0.5;

    public static Vec3 centerOf(BoundingBox3dc query) {
        return JomlVectors.center(query.minX(), query.minY(), query.minZ(), query.maxX(), query.maxY(), query.maxZ());
    }

    public static boolean intersects(WorldFold fold, Vec3 queryCenter, BoundingBox3dc subLevelBox, BoundingBox3dc query) {
        if (subLevelBox.maxY() < query.minY() || subLevelBox.minY() > query.maxY()) {
            return false;
        }

        if (subLevelBox.intersects(query)) {
            return true;
        }

        double reachX = (subLevelBox.maxX() - subLevelBox.minX() + query.maxX() - query.minX()) * HALF;
        double reachZ = (subLevelBox.maxZ() - subLevelBox.minZ() + query.maxZ() - query.minZ()) * HALF;
        double centerX = (subLevelBox.minX() + subLevelBox.maxX()) * HALF;
        double centerZ = (subLevelBox.minZ() + subLevelBox.maxZ()) * HALF;
        if (fold.sqrDistance(queryCenter.x, 0.0, queryCenter.z, centerX, 0.0, centerZ) > reachX * reachX + reachZ * reachZ) {
            return false;
        }

        AABB subLevelAabb = new AABB(subLevelBox.minX(), subLevelBox.minY(), subLevelBox.minZ(),
                subLevelBox.maxX(), subLevelBox.maxY(), subLevelBox.maxZ());
        AABB nearest = fold.foldBox(queryCenter, subLevelAabb).value();
        return nearest != subLevelAabb && query.intersects(nearest);
    }

    private SableSeamQuery() {
    }
}
