package com.exoticworlds.compat.simpleatlas;

import com.exoticworlds.compat.map.AxisCopies;

public record AtlasView(float contentX, float contentY, float contentWidth, float contentHeight) {
    public static double panKeepingCenter(double pan, float center, float originBefore, float tileBefore,
            float origin, float tile) {
        double tilesFromOrigin = (center - originBefore - pan) / tileBefore;
        return center - tilesFromOrigin * tile - origin;
    }

    public static float panInside(AxisCopies axis, float worldStart, float viewStart, float viewLength,
            float pixelsPerBlock) {
        double center = axis.getMin() + (viewStart + viewLength / 2.0 - worldStart) / pixelsPerBlock;
        double clamped = axis.clampView(center, viewLength / 2.0 / pixelsPerBlock);
        return (float) ((center - clamped) * pixelsPerBlock);
    }
}
