package com.exoticworlds.core.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.CarriedShape;
import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

public interface ShapedChunkGenerator {
    String SETTINGS_KEY = "settings";

    MapCodec<CarriedShape> CARRIED_CODEC = CarriedShape.mapCodec(CarriedShape.NO_KEY_PREFIX);

    CarriedShape getCarriedShape();

    ChunkGenerator getUnshaped();

    default WorldFold getFold() {
        return getCarriedShape().getFold();
    }

    static @Nullable CarriedShape getCarriedShape(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator shaped) {
            return wrapped(shaped.getCarriedShape());
        }

        return generator instanceof ShapeStamp stamp ? wrapped(getStampedShape(stamp)) : null;
    }

    private static @Nullable CarriedShape getStampedShape(ShapeStamp stamp) {
        CarriedShape own = stamp.exotic_worlds$getCarriedShape();
        return own != null ? own : stamp.exotic_worlds$getBakedShape();
    }

    static boolean isTilingAtSeam(ChunkGenerator generator) {
        return generator instanceof ShapedChunkGenerator
                || generator instanceof NoiseBasedChunkGenerator
                || generator.getClass() == FlatLevelSource.class;
    }

    static @Nullable CarriedShape getCarriedShape(ServerLevel level) {
        return getCarriedShape(level.getChunkSource().getGenerator());
    }

    static @Nullable WorldFold getWrappedFold(ChunkGenerator generator) {
        CarriedShape carried = getCarriedShape(generator);
        return carried == null ? null : carried.getFold();
    }

    static WorldFold getFold(ChunkGenerator generator) {
        WorldFold fold = getWrappedFold(generator);
        return fold != null ? fold : WorldFolds.NOOP;
    }

    private static @Nullable CarriedShape wrapped(@Nullable CarriedShape carried) {
        return carried != null && carried.getFold().isWrapped() ? carried : null;
    }
}
