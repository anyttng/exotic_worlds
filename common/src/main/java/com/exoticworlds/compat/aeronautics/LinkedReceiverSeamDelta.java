package com.exoticworlds.compat.aeronautics;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.world.level.block.entity.BlockEntity;

public final class LinkedReceiverSeamDelta {
    public static Vector3d seat(BlockEntity receiver, Vector3d target, Vector3dc current) {
        return seat(WorldLoopAttachments.wrappedFoldOfReader(receiver.getLevel()), target, current);
    }

    static Vector3d seat(@Nullable WorldFold fold, Vector3d target, Vector3dc current) {
        return JomlVectors.write(
                NearestCopy.toward(fold, JomlVectors.read(current), JomlVectors.read(target)), target);
    }

    private LinkedReceiverSeamDelta() {
    }
}
