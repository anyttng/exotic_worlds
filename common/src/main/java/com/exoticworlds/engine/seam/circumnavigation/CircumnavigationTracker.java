package com.exoticworlds.engine.seam.circumnavigation;

import com.exoticworlds.core.fold.WorldLoopAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class CircumnavigationTracker {
    public static void sample(ServerPlayer player) {
        Level level = player.level();
        SeamTravel.Step step = SeamTravel.of(player)
                .advance(WorldLoopAttachments.getWrappedFold(level), level.dimension(), player.position());
        if (step.isLapClosed()) {
            WorldLoopCriteria.CIRCUMNAVIGATE.trigger(player);
        }
    }

    private CircumnavigationTracker() {
    }
}
