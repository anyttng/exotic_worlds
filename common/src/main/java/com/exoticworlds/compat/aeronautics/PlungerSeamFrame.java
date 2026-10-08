package com.exoticworlds.compat.aeronautics;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PlungerSeamFrame {
    public static Vec3 seatOther(Level level, Vec3 own, Vec3 other) {
        return seatOther(WorldLoopAttachments.wrappedTransformerOfReader(level), own, other);
    }

    static Vec3 seatOther(@Nullable WorldFold fold, Vec3 own, Vec3 other) {
        return NearestCopy.toward(fold, own, other);
    }

    private PlungerSeamFrame() {
    }
}
