package com.exoticworlds.compat.sable;

import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public final class SableSeamDistance {
    public static Vector3dc toward(@Nullable LevelReader level, Vector3dc from, Vector3dc to) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedFoldOfReader(level), from, to);
    }

    public static Vec3 toward(@Nullable LevelReader level, Vector3dc receiver, double x, double y, double z) {
        return toward(WorldLoopAttachments.wrappedFoldOfReader(level), JomlVectors.read(receiver), x, y, z);
    }

    public static Vec3 toward(@Nullable LevelReader level, Vec3 receiver, double x, double y, double z) {
        return toward(WorldLoopAttachments.wrappedFoldOfReader(level), receiver, x, y, z);
    }

    static Vec3 toward(@Nullable WorldFold fold, Vec3 receiver, double x, double y, double z) {
        return NearestCopy.toward(fold, receiver, new Vec3(x, y, z));
    }

    private SableSeamDistance() {
    }
}
