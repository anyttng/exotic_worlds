package com.exoticworlds.compat.freeterraforged.banding;

import com.exoticworlds.compat.terraforged.RtfLap;

// The underground regions are cells of a lattice of their own, read straight off the block position: on the lap it
// closes at a whole number of regions per lap, never larger than FreeTerraForged's, and shrinks with the climate under
// Compact biomes.
public final class BandingLattice {
    public static double x(RtfLap.Frame frame, double compression, int blockX, int blockZ, int horizontalSize) {
        try (RtfLap.Frame.Scope compressed = frame.compress(compression);
                RtfLap.Frame.Scope lattice = frame.octave(1.0 / horizontalSize)) {
            return frame.latticeX((float) (blockX * compression), (float) (blockZ * compression))
                    * (double) frame.xScale();
        }
    }

    public static double z(RtfLap.Frame frame, double compression, int blockZ, int horizontalSize) {
        try (RtfLap.Frame.Scope compressed = frame.compress(compression);
                RtfLap.Frame.Scope lattice = frame.octave(1.0 / horizontalSize)) {
            return frame.shiftZ((float) (blockZ * compression)) * (double) frame.zScale();
        }
    }

    public static int foldX(RtfLap.Frame frame, double compression, int cellX, int cellZ, int horizontalSize) {
        try (RtfLap.Frame.Scope compressed = frame.compress(compression);
                RtfLap.Frame.Scope lattice = frame.octave(1.0 / horizontalSize)) {
            return frame.foldX(cellX, cellZ);
        }
    }

    public static int foldZ(RtfLap.Frame frame, double compression, int cellZ, int horizontalSize) {
        try (RtfLap.Frame.Scope compressed = frame.compress(compression);
                RtfLap.Frame.Scope lattice = frame.octave(1.0 / horizontalSize)) {
            return frame.foldZ(cellZ);
        }
    }

    private BandingLattice() {
    }
}
