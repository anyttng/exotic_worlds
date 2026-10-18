package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.exoticworlds.compat.create.mixin.SymmetryMirrorAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SymmetryMirrorFold {
    public static SymmetryMirror nearestTo(@Nullable Level level, SymmetryMirror mirror, BlockPos anchor) {
        Vec3 canonical = mirror.getPosition();
        Vec3 nearest = CreateSeamFold.nearestCopy(level, Vec3.atLowerCornerOf(anchor), canonical);
        if (nearest.equals(canonical)) {
            return mirror;
        }

        // A copy, since the stack hands back its stored mirror and moving it would move the player's own.
        return SymmetryMirrorAccessor.exotic_worlds$create(
                mirror.getOrientationIndex(), nearest, mirror.typeName(), mirror.enable);
    }

    private SymmetryMirrorFold() {
    }
}
