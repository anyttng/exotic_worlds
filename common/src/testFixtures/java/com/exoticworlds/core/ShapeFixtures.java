package com.exoticworlds.core;

import net.minecraft.core.Direction;

public final class ShapeFixtures {
    public static WorldLoopBounds loopedBounds(int xMinChunk, int xMaxChunk, int zMinChunk, int zMaxChunk) {
        return new WorldLoopBounds(new WorldLoopBounds.AxisBounds.Looped(xMinChunk, xMaxChunk),
                new WorldLoopBounds.AxisBounds.Looped(zMinChunk, zMaxChunk));
    }

    public static FlatShape torus(WorldLoopBounds bounds) {
        return new FlatShape(bounds, FlatShape.NO_SKEW, null);
    }

    public static FlatShape torus(int chunkWidth) {
        return torus(WorldLoopBounds.ofWidth(chunkWidth));
    }

    public static FlatShape torus(int xChunkWidth, int zChunkWidth) {
        return torus(WorldLoopBounds.ofWidths(xChunkWidth, zChunkWidth));
    }

    public static FlatShape latticeTorus(int xChunkWidth, int zChunkWidth, int skewChunks) {
        return FlatShape.latticeTorus(WorldLoopBounds.ofWidths(xChunkWidth, zChunkWidth), skewChunks);
    }

    public static FlatShape cylinder(WorldLoopBounds bounds) {
        return new FlatShape(bounds, FlatShape.NO_SKEW, null);
    }

    public static FlatShape cylinder(Direction.Axis axis, int chunkWidth) {
        return cylinder(WorldLoopBounds.ofWidth(axis, chunkWidth));
    }

    public static FlatShape mirrored(WorldLoopBounds bounds, Direction.Axis mirroredAxis, int mirrorLineChunk) {
        return new FlatShape(bounds, FlatShape.NO_SKEW, new FlatShape.Mirror(mirroredAxis, mirrorLineChunk));
    }

    private ShapeFixtures() {
    }
}
