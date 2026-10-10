package com.exoticworlds.compat.electroenergetics;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.ChunkPos;

public final class WireBoxJump {
    private static final int TWO_COPIES = 2;

    public static boolean mayReseat(WorldFold fold, ChunkPos from, int fromRadius, ChunkPos to, int toRadius) {
        return !from.equals(to) && BoxCopies.count(fold, from, fromRadius + toRadius, to) >= TWO_COPIES;
    }

    private WireBoxJump() {
    }
}
