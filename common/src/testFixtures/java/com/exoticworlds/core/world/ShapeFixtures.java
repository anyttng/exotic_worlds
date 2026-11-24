package com.exoticworlds.core.world;

import net.minecraft.core.Direction;

public final class ShapeFixtures {
    public static WorldLoopBounds loopedBounds(int xMinChunk, int xMaxChunk, int zMinChunk, int zMaxChunk) {
        return new WorldLoopBounds(new WorldLoopBounds.AxisBounds.Looped(xMinChunk, xMaxChunk),
                new WorldLoopBounds.AxisBounds.Looped(zMinChunk, zMaxChunk));
    }

    public static FlatShape torus(WorldLoopBounds bounds) {
        return new FlatShape(bounds, FlatShape.NO_SKEW, null);
    }

    public static FlatShape torus(int widthChunks) {
        return torus(WorldLoopBounds.ofWidth(widthChunks));
    }

    public static FlatShape torus(int xWidthChunks, int zWidthChunks) {
        return torus(WorldLoopBounds.ofWidths(xWidthChunks, zWidthChunks));
    }

    public static FlatShape latticeTorus(int xWidthChunks, int zWidthChunks, int skewChunks) {
        return FlatShape.latticeTorus(WorldLoopBounds.ofWidths(xWidthChunks, zWidthChunks), skewChunks);
    }

    public static FlatShape cylinder(WorldLoopBounds bounds) {
        return new FlatShape(bounds, FlatShape.NO_SKEW, null);
    }

    public static FlatShape cylinder(Direction.Axis axis, int widthChunks) {
        return cylinder(WorldLoopBounds.ofWidth(axis, widthChunks));
    }

    public static FlatShape mirrored(WorldLoopBounds bounds, Direction.Axis mirroredAxis, int mirrorLineChunk) {
        return new FlatShape(bounds, FlatShape.NO_SKEW, new FlatShape.Mirror(mirroredAxis, mirrorLineChunk));
    }

    private ShapeFixtures() {
    }
}
