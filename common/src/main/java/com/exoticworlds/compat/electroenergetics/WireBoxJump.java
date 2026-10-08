package com.exoticworlds.compat.electroenergetics;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

public final class WireBoxJump {
    public static boolean mayReseat(WorldFold fold, ChunkPos from, int fromRadius, ChunkPos to, int toRadius) {
        int reach = fromRadius + toRadius;
        return mayReseat(fold.chunkDomain(Direction.Axis.X), from.x, to.x, reach)
                || mayReseat(fold.chunkDomain(Direction.Axis.Z), from.z, to.z, reach);
    }

    private static boolean mayReseat(WrapDomain domain, int from, int to, int reach) {
        return domain.loops() && Math.abs(domain.unwrapAround(from, to) - from) >= domain.domainLength - reach;
    }

    private WireBoxJump() {
    }
}
