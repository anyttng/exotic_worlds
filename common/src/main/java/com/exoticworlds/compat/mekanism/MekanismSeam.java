package com.exoticworlds.compat.mekanism;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MekanismSeam {
    private static final long NO_POSITION = Long.MAX_VALUE;

    public static long fold(@Nullable Level level, long pos) {
        return pos == NO_POSITION ? pos : fold(level, BlockPos.of(pos)).asLong();
    }

    public static BlockPos fold(@Nullable Level level, BlockPos pos) {
        return WorldLoopAttachments.transformerOfReader(level).fold(pos);
    }

    public static long nearestCopy(@Nullable Level level, long anchor, long target) {
        if (anchor == NO_POSITION || target == NO_POSITION) {
            return target;
        }

        return nearestCopy(level, BlockPos.of(anchor), BlockPos.of(target)).asLong();
    }

    public static BlockPos nearestCopy(@Nullable Level level, BlockPos anchor, BlockPos target) {
        return WorldLoopAttachments.transformerOfReader(level).nearestCopy(anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return WorldLoopAttachments.transformerOfReader(level).nearestCopy(anchor, target);
    }

    public static AABB nearestCopy(@Nullable Level level, Vec3 anchor, AABB box) {
        return WorldLoopAttachments.transformerOfReader(level).foldBox(anchor, box).value();
    }

    private MekanismSeam() {
    }
}
