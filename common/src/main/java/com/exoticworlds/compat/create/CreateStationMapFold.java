package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CreateStationMapFold {
    public static BlockPos canonicalTarget(ResourceKey<Level> dimension, BlockPos target) {
        WorldFold fold = foldFor(dimension);
        return fold == null ? target : canonicalTarget(fold, target);
    }

    public static BlockPos canonicalTarget(WorldFold fold, BlockPos target) {
        return fold.fold(target);
    }

    public static BlockPos targetInMapFrame(ResourceKey<Level> dimension, int centerX, int centerZ, BlockPos target) {
        return targetInMapFrame(foldFor(dimension), centerX, centerZ, target);
    }

    public static BlockPos targetInMapFrame(@Nullable WorldFold fold, int centerX, int centerZ,
            BlockPos target) {
        return NearestCopy.toward(fold, new BlockPos(centerX, target.getY(), centerZ), target);
    }

    public static Vec3 centerInMapFrame(ResourceKey<Level> dimension, int centerX, int centerZ, double x, double z) {
        return centerInMapFrame(foldFor(dimension), centerX, centerZ, x, z);
    }

    public static Vec3 centerInMapFrame(@Nullable WorldFold fold, int centerX, int centerZ, double x,
            double z) {
        return NearestCopy.toward(fold, new Vec3(centerX, 0.0, centerZ), new Vec3(x, 0.0, z));
    }

    private static @Nullable WorldFold foldFor(ResourceKey<Level> dimension) {
        return CreateSeamFold.wrappedFoldOf(null, dimension);
    }

    private CreateStationMapFold() {
    }
}
