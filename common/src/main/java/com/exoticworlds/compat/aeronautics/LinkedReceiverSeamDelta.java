package com.exoticworlds.compat.aeronautics;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class LinkedReceiverSeamDelta {
    public static Vector3d fold(BlockEntity receiver, Vector3d target, Vector3dc current,
            Operation<Vector3d> original) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOfReader(receiver.getLevel());
        if (fold == null) {
            return original.call(target, current);
        }

        return fold(fold, target, current);
    }

    static Vector3d fold(WorldFold fold, Vector3d target, Vector3dc current) {
        Vec3 folded = fold.foldDelta(JomlVectors.read(current), JomlVectors.read(target));
        return JomlVectors.write(folded, target);
    }

    private LinkedReceiverSeamDelta() {
    }
}
