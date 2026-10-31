package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SeamRange {
    public static BlockPos toward(Entity levelSource, BlockPos anchor, BlockPos target) {
        return NearestCopy.toward(wrappedFoldOf(levelSource), anchor, target);
    }

    public static BlockPos toward(Level levelSource, BlockPos anchor, BlockPos target) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedFoldOf(levelSource), anchor, target);
    }

    public static Vec3i toward(Entity levelSource, BlockPos anchor, Vec3i target) {
        return NearestCopy.toward(wrappedFoldOf(levelSource), anchor, target);
    }

    public static Vec3i toward(Level levelSource, BlockPos anchor, Vec3i target) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedFoldOf(levelSource), anchor, target);
    }

    public static Vec3 toward(Entity levelSource, Vec3 anchor, Vec3 target) {
        return NearestCopy.toward(wrappedFoldOf(levelSource), anchor, target);
    }

    public static Vec3 toward(Level levelSource, Vec3 anchor, Vec3 target) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedFoldOf(levelSource), anchor, target);
    }

    public static Position toward(Entity levelSource, Vec3 anchor, Position target) {
        return NearestCopy.toward(wrappedFoldOf(levelSource), anchor, target);
    }

    public static Position towardCenterOf(Entity levelSource, BlockPos block, Position target) {
        return NearestCopy.towardCenterOf(wrappedFoldOf(levelSource), block, target);
    }

    public static Position towardCenterOf(Level levelSource, BlockPos block, Position target) {
        return NearestCopy.towardCenterOf(WorldLoopAttachments.wrappedFoldOf(levelSource), block, target);
    }

    public static double sqr(@Nullable WorldFold fold, Vec3i from, Vec3i to) {
        if (fold == null) {
            return from.distSqr(to);
        }

        return fold.sqrDistance(
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ());
    }

    public static double sqr(@Nullable WorldFold fold, Vec3 from, Position to) {
        if (fold == null) {
            return from.distanceToSqr(to.x(), to.y(), to.z());
        }

        return fold.sqrDistance(from.x, from.y, from.z, to.x(), to.y(), to.z());
    }

    public static double sqr(Entity levelSource, Vec3 from, Position to) {
        return sqr(foldOf(levelSource), from, to);
    }

    public static double sqr(Entity levelSource, Vec3 from, double toX, double toY, double toZ) {
        return foldOf(levelSource).sqrDistance(from.x, from.y, from.z, toX, toY, toZ);
    }

    public static boolean closerThan(Entity levelSource, Vec3 from, Position to, double distance) {
        return sqr(levelSource, from, to) < Mth.square(distance);
    }

    public static boolean closerThan(WorldFold fold, Vec3 from, Vec3 to, double distanceXZ, double distanceY) {
        Vec3 delta = fold.foldDelta(from, to);
        return Mth.lengthSquared(delta.x, delta.z) < Mth.square(distanceXZ)
                && Mth.square(delta.y) < Mth.square(distanceY);
    }

    private static WorldFold foldOf(Entity levelSource) {
        return WorldLoopAttachments.foldOf(levelSource.level());
    }

    private static @Nullable WorldFold wrappedFoldOf(Entity levelSource) {
        return WorldLoopAttachments.wrappedFoldOf(levelSource.level());
    }

    private SeamRange() {
    }
}
