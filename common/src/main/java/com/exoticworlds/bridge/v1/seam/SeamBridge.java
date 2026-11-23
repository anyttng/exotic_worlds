package com.exoticworlds.bridge.v1.seam;

import java.util.OptionalDouble;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.circumnavigation.SeamTravel;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

public final class SeamBridge {

    public static OptionalDouble travelOf(ServerPlayer player, Direction.Axis axis) {
        if (axis == Direction.Axis.Y) {
            return OptionalDouble.empty();
        }

        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(player.level());
        if (fold == null || !fold.bounds().loops(axis)) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(
                SeamTravel.of(player).in(player.level().dimension()).on(axis));
    }

    private SeamBridge() {
    }
}
