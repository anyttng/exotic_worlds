package com.exoticworlds.compat.simpleatlas;

import com.exoticworlds.compat.AxisCopies;

public record AtlasView(float contentX, float contentY, float contentWidth, float contentHeight) {
    public static double panKeepingCentre(double pan, float centre, float originBefore, float tileBefore,
            float origin, float tile) {
        double tilesFromOrigin = (centre - originBefore - pan) / tileBefore;
        return centre - tilesFromOrigin * tile - origin;
    }

    public static float panInside(AxisCopies axis, float worldStart, float viewStart, float viewLength,
            float pixelsPerBlock) {
        double centre = axis.min() + (viewStart + viewLength / 2.0 - worldStart) / pixelsPerBlock;
        double clamped = axis.clampView(centre, viewLength / 2.0 / pixelsPerBlock);
        return (float) ((centre - clamped) * pixelsPerBlock);
    }
}
