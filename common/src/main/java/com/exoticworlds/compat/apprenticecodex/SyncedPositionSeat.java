package com.exoticworlds.compat.apprenticecodex;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SyncedPositionSeat {
    public static Vec3 besideEntity(Entity entity, Vec3 synced) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedClientBoundsFoldOf(entity.level()), entity.position(),
                synced);
    }

    private SyncedPositionSeat() {
    }
}
