package com.exoticworlds.shape;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.core.NetherScales;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class ShapeStems {

    public static @Nullable LoopSpans spansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            Predicate<LoopSpans> ownGeometry) {
        LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
        return spans != null && ownGeometry.test(spans) ? spans : null;
    }

    public static LoopSpans netherSpans(LoopSpans overworld, int netherScale) {
        return overworld.scaledDown(normalizeNetherScale(netherScale, overworld));
    }

    public static int readNetherScale(WorldDimensions dimensions, Predicate<LoopSpans> ownGeometry,
            LoopSpans overworld, Direction.Axis axis) {
        LoopSpans nether = spansOf(dimensions, LevelStem.NETHER, ownGeometry);
        int stored = nether != null && nether.loops(axis)
                ? overworld.chunkWidth(axis) / nether.chunkWidth(axis)
                : NetherScales.DEFAULT;
        return normalizeNetherScale(stored, overworld);
    }

    private static int normalizeNetherScale(int netherScale, LoopSpans overworld) {
        int xChunkWidth = overworld.loops(Direction.Axis.X)
                ? overworld.chunkWidth(Direction.Axis.X)
                : overworld.chunkWidth(Direction.Axis.Z);
        int zChunkWidth = overworld.loops(Direction.Axis.Z) ? overworld.chunkWidth(Direction.Axis.Z) : xChunkWidth;
        return NetherScales.normalize(netherScale, xChunkWidth, zChunkWidth);
    }

    private ShapeStems() {
    }
}
