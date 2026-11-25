package com.exoticworlds.core.gen;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.CarriedShape;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

public final class GeneratorShapes {

    public static @Nullable CarriedShape getCarriedShape(ChunkGenerator generator) {
        return generator instanceof ShapeStamp stamp ? wrapped(getStampedShape(stamp)) : null;
    }

    public static boolean isTilingAtSeam(ChunkGenerator generator) {
        return generator instanceof NoiseBasedChunkGenerator || generator.getClass() == FlatLevelSource.class;
    }

    public static List<GenerationFeature> activeFeatures(ChunkGenerator generator, ResourceKey<Level> level) {
        CarriedShape carried = getCarriedShape(generator);
        return carried == null
                ? List.of()
                : ShapeFeatures.activeFeatures(carried, Registries.levelToLevelStem(level));
    }

    public static @Nullable CarriedShape getCarriedShape(ServerLevel level) {
        return getCarriedShape(level.getChunkSource().getGenerator());
    }

    public static @Nullable WorldFold getWrappedFold(ChunkGenerator generator) {
        CarriedShape carried = getCarriedShape(generator);
        return carried == null ? null : carried.getFold();
    }

    public static WorldFold getFold(ChunkGenerator generator) {
        WorldFold fold = getWrappedFold(generator);
        return fold != null ? fold : WorldFolds.NOOP;
    }

    private static @Nullable CarriedShape getStampedShape(ShapeStamp stamp) {
        CarriedShape own = stamp.exotic_worlds$getCarriedShape();
        return own != null ? own : stamp.exotic_worlds$getBakedShape();
    }

    private static @Nullable CarriedShape wrapped(@Nullable CarriedShape carried) {
        return carried != null && carried.getFold().isWrapped() ? carried : null;
    }

    private GeneratorShapes() {
    }
}
