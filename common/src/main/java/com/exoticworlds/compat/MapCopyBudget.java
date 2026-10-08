package com.exoticworlds.compat;

import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class MapCopyBudget {
    private static final int MAX_TILE_BLITS = 16_384;

    public static int copyRangeCap(int loopedAxes, int tilesWithContent) {
        int budget = MAX_TILE_BLITS / Math.max(1, tilesWithContent);
        return switch (loopedAxes) {
            case 2 -> (int) ((Math.sqrt(budget) - 1) / 2);
            case 1 -> (budget - 1) / 2;
            default -> 0;
        };
    }

    public static int[] copyRanges(AxisCopies copiesX, AxisCopies copiesZ, int gridTiles, int[] spanX, int[] spanZ,
            MapCopies copies) {
        if (copies == MapCopies.SINGLE) {
            return new int[] {0, 0};
        }

        int[] lapsX = copiesX.laps(spanX[0], spanX[1]);
        int[] lapsZ = copiesZ.laps(spanZ[0], spanZ[1]);
        int rangeX = farthestLap(lapsX);
        int rangeZ = farthestLap(lapsZ);
        if ((long) gridTiles * lapsX.length * lapsZ.length <= MAX_TILE_BLITS) {
            return new int[] {rangeX, rangeZ};
        }

        int cap = copyRangeCap((copiesX.loops() ? 1 : 0) + (copiesZ.loops() ? 1 : 0), gridTiles);
        return new int[] {Math.min(rangeX, cap), Math.min(rangeZ, cap)};
    }

    private static int farthestLap(int[] laps) {
        int farthest = 0;
        for (int lap : laps) {
            farthest = Math.max(farthest, Math.abs(lap));
        }

        return farthest;
    }

    public static int[] drawnLaps(AxisCopies copies, int spanMin, int spanMax, MapCopies mapCopies) {
        int[] laps = copies.laps(spanMin, spanMax);
        if (mapCopies != MapCopies.SINGLE) {
            return laps;
        }

        for (int lap : laps) {
            if (lap == 0) {
                return new int[] {0};
            }
        }

        return new int[0];
    }

    public static Copies painted(AxisCopies x, int rangeX, AxisCopies z, int rangeZ) {
        return new Copies(Math.max(rangeX, rangeZ), new BoundingBox(
                paintedMin(x, rangeX), Integer.MIN_VALUE, paintedMin(z, rangeZ),
                paintedMax(x, rangeX), Integer.MAX_VALUE, paintedMax(z, rangeZ)));
    }

    private static int paintedMin(AxisCopies copies, int range) {
        return copies.loops() ? copies.min() + copies.offset(-range) : Integer.MIN_VALUE;
    }

    private static int paintedMax(AxisCopies copies, int range) {
        return copies.loops() ? copies.max() + copies.offset(range) - 1 : Integer.MAX_VALUE;
    }

    private MapCopyBudget() {
    }
}
