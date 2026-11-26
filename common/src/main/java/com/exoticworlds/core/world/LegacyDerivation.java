package com.exoticworlds.core.world;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

public final class LegacyDerivation {

    public static @Nullable FlatShape derive(FlatShape overworld, double overworldScale, double coordinateScale) {
        if (overworldScale <= 0.0 || coordinateScale <= 0.0) {
            return null;
        }

        AxisBounds x = derivedAxis(overworld.bounds().x(), overworldScale, coordinateScale);
        AxisBounds z = derivedAxis(overworld.bounds().z(), overworldScale, coordinateScale);
        Integer skewChunks = derivedChunks(overworld.skewChunks(), overworldScale, coordinateScale);
        return x == null || z == null || skewChunks == null
                ? null
                : new FlatShape(new WorldLoopBounds(x, z), skewChunks, null);
    }

    private static @Nullable Integer derivedChunks(int chunks, double overworldScale, double coordinateScale) {
        double derived = chunks * overworldScale / coordinateScale;
        int whole = (int) derived;
        return derived == whole ? whole : null;
    }

    private static @Nullable AxisBounds derivedAxis(AxisBounds axis, double overworldScale, double coordinateScale) {
        if (!(axis instanceof AxisBounds.Looped looped)) {
            return axis;
        }

        Integer widthChunks = derivedChunks(looped.getWidthChunks(), overworldScale, coordinateScale);
        return widthChunks != null && WorldLoopSizes.isInRange(widthChunks)
                ? AxisBounds.Looped.ofWidth(widthChunks)
                : null;
    }

    private LegacyDerivation() {
    }
}
