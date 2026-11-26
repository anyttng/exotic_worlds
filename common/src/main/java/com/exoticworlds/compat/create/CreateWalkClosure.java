package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;

public final class CreateWalkClosure {
    private final WorldFold fold;

    private @Nullable BlockPos legStart;
    private @Nullable BlockPos lastQuery;
    private @Nullable Vec3i step;
    private @Nullable BlockPos origin;

    public static @Nullable CreateWalkClosure of(LevelReader world) {
        WorldFold fold = WorldLoopAttachments.getWrappedFoldOfReader(world);
        return fold == null ? null : new CreateWalkClosure(fold);
    }

    public static boolean isClosingAt(LevelReader world, BlockPos pos, LocalRef<CreateWalkClosure> closureRef,
            LocalBooleanRef resolved) {
        if (!resolved.get()) {
            closureRef.set(of(world));
            resolved.set(true);
        }

        CreateWalkClosure closure = closureRef.get();
        return closure != null && closure.isClosing(pos);
    }

    CreateWalkClosure(WorldFold fold) {
        this.fold = fold;
    }

    public boolean isClosing(BlockPos query) {
        if (lastQuery != null && isContinuing(query, lastQuery)) {
            lastQuery = query;
            BlockPos legOrigin = origin;
            return legOrigin != null && !query.equals(legOrigin)
                    && fold.fold(query).equals(fold.fold(legOrigin));
        }

        openLeg(query);
        return false;
    }

    private boolean isContinuing(BlockPos query, BlockPos previous) {
        Vec3i delta = query.subtract(previous);
        Vec3i legStep = step;
        if (legStep != null) {
            return delta.equals(legStep);
        }

        BlockPos first = legStart;
        if (first == null || !isUnitStep(delta)) {
            return false;
        }

        step = delta;
        origin = first.subtract(delta);
        return true;
    }

    private void openLeg(BlockPos query) {
        legStart = query;
        lastQuery = query;
        step = null;
        origin = null;
    }

    private static boolean isUnitStep(Vec3i delta) {
        return Math.abs(delta.getX()) + Math.abs(delta.getY()) + Math.abs(delta.getZ()) == 1;
    }
}
