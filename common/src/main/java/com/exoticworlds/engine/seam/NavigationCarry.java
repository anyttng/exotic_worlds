package com.exoticworlds.engine.seam;

import com.exoticworlds.core.SeamTransform;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

public final class NavigationCarry {
    public static int footprintOf(Mob mob) {
        return Mth.floor(mob.getBbWidth() + 1.0F);
    }

    public static int nodeX(SeamTransform move, int x, int z, int footprint) {
        int far = footprint - 1;
        return Math.min(move.applyCellX(x, z), move.applyCellX(x + far, z + far));
    }

    public static int nodeZ(SeamTransform move, int x, int z, int footprint) {
        int far = footprint - 1;
        return Math.min(move.applyCellZ(x, z), move.applyCellZ(x + far, z + far));
    }

    public static Vec3i node(SeamTransform move, Vec3i node, int footprint) {
        return new Vec3i(
                nodeX(move, node.getX(), node.getZ(), footprint),
                node.getY(),
                nodeZ(move, node.getX(), node.getZ(), footprint));
    }

    private NavigationCarry() {
    }
}
