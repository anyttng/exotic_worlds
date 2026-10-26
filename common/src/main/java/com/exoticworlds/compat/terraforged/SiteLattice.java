package com.exoticworlds.compat.terraforged;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

public record SiteLattice(double xLap, double zLap, double skew, float spacing, int xCells, int zCells, float xSpacing,
        float zSpacing) {
    public interface Corner {
        float at(int cellX, int cellZ);
    }

    public record Seat(int cellX, int cellZ, float x, float z) {
    }

    public static @Nullable SiteLattice bound(float spacing) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? null : over(frame.lap(Direction.Axis.X), frame.lap(Direction.Axis.Z),
                frame.entrySkew(), spacing);
    }

    public static SiteLattice over(double xLap, double zLap, double skew, float spacing) {
        boolean torus = xLap != RtfLap.OPEN && zLap != RtfLap.OPEN;
        int xCells = RtfLap.cellsOver(xLap, 1.0 / spacing, torus);
        int zCells = RtfLap.cellsOver(zLap, 1.0 / spacing, torus);
        return new SiteLattice(xLap, zLap, skew, spacing, xCells, zCells, snapped(xLap, xCells, spacing),
                snapped(zLap, zCells, spacing));
    }

    public static float spacingOn(float spacing, Direction.Axis axis) {
        SiteLattice lattice = bound(spacing);
        return lattice == null ? spacing : lattice.spacing(axis);
    }

    public static float sharedOn(float spacing) {
        SiteLattice lattice = bound(spacing);
        return lattice == null ? spacing : lattice.shared();
    }

    public static float scaledOn(float length, float spacing) {
        SiteLattice lattice = bound(spacing);
        return lattice == null ? length : length * lattice.scale();
    }

    public float spacing(Direction.Axis axis) {
        return axis == Direction.Axis.X ? this.xSpacing : this.zSpacing;
    }

    public float shared() {
        return Math.min(this.xSpacing, this.zSpacing);
    }

    public float scale() {
        return shared() / this.spacing;
    }

    public int cellX(float x) {
        return (int) Math.floor(x / this.xSpacing);
    }

    public int cellZ(float z) {
        return (int) Math.floor(z / this.zSpacing);
    }

    // A window row past the Z seam is the canonical row moved by the skew, so its columns are counted from the moved point.
    public int copyX(int cellX, int cellZ, int pointCellX, float x) {
        int laps = zLaps(cellZ);
        return laps == 0 ? cellX : (int) Math.floor((x - laps * this.skew) / this.xSpacing) + cellX - pointCellX;
    }

    public int canonicalX(int copyX) {
        return foldX(copyX);
    }

    public int canonicalZ(int copyZ) {
        return this.zCells == RtfLap.NO_PERIOD ? copyZ : Math.floorMod(copyZ, this.zCells);
    }

    public float copySkew(int copyZ) {
        return (float) (zLaps(copyZ) * this.skew);
    }

    public Seat seat(int cellX, int cellZ, int pointCellX, int pointCellZ, float x, float z) {
        double seatedX = x;
        double seatedZ = z;
        int column = copyX(cellX, cellZ, pointCellX, x);
        int row = cellZ;
        int zLaps = zLaps(cellZ);
        if (zLaps != 0) {
            seatedX -= zLaps * this.skew;
            seatedZ -= zLaps * this.zLap;
            row -= zLaps * this.zCells;
        }

        if (this.xCells != RtfLap.NO_PERIOD) {
            int laps = Math.floorDiv(column, this.xCells);
            seatedX -= laps * this.xLap;
            column -= laps * this.xCells;
        }

        return new Seat(column, row, (float) seatedX, (float) seatedZ);
    }

    public float interpolate(float x, float z, Corner corner) {
        float cellZ = z / this.zSpacing;
        int row = (int) Math.floor(cellZ);
        return lerp(row(x, row, corner), row(x, row + 1, corner), cellZ - row);
    }

    private float row(float x, int row, Corner corner) {
        int laps = zLaps(row);
        float cellX = (float) ((x - laps * this.skew) / this.xSpacing);
        int column = (int) Math.floor(cellX);
        int canonicalRow = row - laps * this.zCells;
        return lerp(corner.at(foldX(column), canonicalRow), corner.at(foldX(column + 1), canonicalRow),
                cellX - column);
    }

    private int foldX(int column) {
        return this.xCells == RtfLap.NO_PERIOD ? column : Math.floorMod(column, this.xCells);
    }

    private int zLaps(int cellZ) {
        return this.zCells == RtfLap.NO_PERIOD ? 0 : Math.floorDiv(cellZ, this.zCells);
    }

    private static float lerp(float from, float to, float t) {
        return from + t * (to - from);
    }

    private static float snapped(double lap, int cells, float spacing) {
        return cells == RtfLap.NO_PERIOD ? spacing : (float) (lap / cells);
    }
}
