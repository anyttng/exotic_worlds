package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SeamRange {
    public static BlockPos toward(Entity levelSource, BlockPos anchor, BlockPos target) {
        return NearestCopy.toward(getWrappedFold(levelSource), anchor, target);
    }

    public static BlockPos toward(Level levelSource, BlockPos anchor, BlockPos target) {
        return NearestCopy.toward(WorldLoopAttachments.getWrappedFold(levelSource), anchor, target);
    }

    public static Vec3i toward(Entity levelSource, BlockPos anchor, Vec3i target) {
        return NearestCopy.toward(getWrappedFold(levelSource), anchor, target);
    }

    public static Vec3i toward(Level levelSource, BlockPos anchor, Vec3i target) {
        return NearestCopy.toward(WorldLoopAttachments.getWrappedFold(levelSource), anchor, target);
    }

    public static Vec3 toward(Entity levelSource, Vec3 anchor, Vec3 target) {
        return NearestCopy.toward(getWrappedFold(levelSource), anchor, target);
    }

    public static Vec3 toward(Level levelSource, Vec3 anchor, Vec3 target) {
        return NearestCopy.toward(WorldLoopAttachments.getWrappedFold(levelSource), anchor, target);
    }

    public static Position toward(Entity levelSource, Vec3 anchor, Position target) {
        return NearestCopy.toward(getWrappedFold(levelSource), anchor, target);
    }

    public static Position towardCenterOf(Entity levelSource, BlockPos block, Position target) {
        return NearestCopy.towardCenterOf(getWrappedFold(levelSource), block, target);
    }

    public static Position towardCenterOf(Level levelSource, BlockPos block, Position target) {
        return NearestCopy.towardCenterOf(WorldLoopAttachments.getWrappedFold(levelSource), block, target);
    }

    public static double sqr(@Nullable WorldFold fold, Vec3i from, Vec3i to) {
        if (fold == null) {
            return from.distSqr(to);
        }

        return fold.sqrDistanceBlocks(
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ());
    }

    private static @Nullable WorldFold getWrappedFold(Entity levelSource) {
        return WorldLoopAttachments.getWrappedFold(levelSource.level());
    }

    private SeamRange() {
    }
}
