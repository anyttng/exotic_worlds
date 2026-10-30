package com.exoticworlds.compat.aeronautics;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.simulated_team.simulated.util.SimMovementContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MagnetSeamFrame {
    private static final double HALF = 0.5;

    public static Vector3d midpoint(@Nullable Level level, Vec3 tip1, Vec3 tip2, Vector3d average) {
        return midpoint(WorldLoopAttachments.wrappedFoldOfReader(level), tip1, tip2, average);
    }

    static Vector3d midpoint(@Nullable WorldFold fold, Vec3 tip1, Vec3 tip2, Vector3d average) {
        if (fold == null) {
            return average;
        }

        Vec3 seated = fold.nearestCopy(tip2, tip1);
        Vec3 midpoint = fold.fold(tip2.add(seated).scale(HALF));
        if (midpoint.x == average.x && midpoint.z == average.z) {
            return average;
        }

        return JomlVectors.write(midpoint, average);
    }

    public static Object seatNearbyMagnet(BlockEntity self, Object nearby) {
        Level level = self.getLevel();
        WorldFold fold = WorldLoopAttachments.wrappedFoldOfReader(level);
        if (fold == null || !(nearby instanceof Vector3d)) {
            return nearby;
        }

        Vec3 anchor = SimMovementContext.getMovementContext(level, Vec3.atCenterOf(self.getBlockPos())).globalPosition();
        return seatNearbyMagnet(fold, anchor, nearby);
    }

    static Object seatNearbyMagnet(@Nullable WorldFold fold, Vec3 anchor, Object nearby) {
        if (fold == null || !(nearby instanceof Vector3d position)) {
            return nearby;
        }

        return JomlVectors.seat(fold, anchor, position);
    }

    private MagnetSeamFrame() {
    }
}
