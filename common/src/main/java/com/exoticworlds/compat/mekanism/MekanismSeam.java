package com.exoticworlds.compat.mekanism;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MekanismSeam {
    private static final long NO_POSITION = Long.MAX_VALUE;

    public static long fold(@Nullable Level level, long pos) {
        return pos == NO_POSITION ? pos : WorldLoopAttachments.foldOfReader(level).foldBlockNode(pos);
    }

    public static BlockPos fold(@Nullable Level level, BlockPos pos) {
        return WorldLoopAttachments.foldOfReader(level).fold(pos);
    }

    public static long nearestCopy(@Nullable Level level, long anchor, long target) {
        if (anchor == NO_POSITION || target == NO_POSITION) {
            return target;
        }

        return WorldLoopAttachments.foldOfReader(level).nearestCopyBlockNode(anchor, target);
    }

    public static BlockPos nearestCopy(@Nullable Level level, BlockPos anchor, BlockPos target) {
        return WorldLoopAttachments.foldOfReader(level).nearestCopy(anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return WorldLoopAttachments.foldOfReader(level).nearestCopy(anchor, target);
    }

    public static AABB nearestCopy(@Nullable Level level, Vec3 anchor, AABB box) {
        return WorldLoopAttachments.foldOfReader(level).nearestCopyOriented(anchor, box).value();
    }

    public static Vec3 stopOnAxis(@Nullable Level level, Vec3 origin, Direction.Axis axis, Vec3 entity) {
        return stopOnAxis(WorldLoopAttachments.foldOfReader(level), origin, axis, entity);
    }

    static Vec3 stopOnAxis(WorldFold fold, Vec3 origin, Direction.Axis axis, Vec3 entity) {
        return origin.with(axis, NearestCopy.toward(fold, axis, origin, entity));
    }

    private MekanismSeam() {
    }
}
