package com.exoticworlds.compat.wover;

import com.exoticworlds.engine.noise.PeriodicNoiseSampler;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class OpenSimplexStandIn {
    public static final double UNBOUNDED = 0.0;

    static final double AMPLITUDE = 1.252;

    static final double RATE = 1.065;

    static final double AMPLITUDE_3D = 1.162;

    static final double RATE_3D = 0.858;

    static final double RATE_3D_VERTICAL = 0.798;

    private static final double BETWEEN_PLANES = 0.5;

    private static final long MIN_CELLS = 1L;

    private static final double CELL_EPSILON = 1.0E-9;

    private final PerlinNoise noise;

    public OpenSimplexStandIn(long seed) {
        this.noise = new PerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed)));
    }

    public double eval(double x, double z, double xPeriod, double zPeriod) {
        long xCells = cells(xPeriod, RATE);
        long zCells = cells(zPeriod, RATE);
        return AMPLITUDE * PeriodicNoiseSampler.sampleLattice(this.noise.perms,
                lattice(x, xPeriod, xCells, RATE) + this.noise.offsetX, Math.floor(this.noise.offsetY) + BETWEEN_PLANES,
                lattice(z, zPeriod, zCells, RATE) + this.noise.offsetZ, xCells, zCells);
    }

    public double eval(double x, double y, double z, double xPeriod, double zPeriod) {
        long xCells = cells(xPeriod, RATE_3D);
        long zCells = cells(zPeriod, RATE_3D);
        return AMPLITUDE_3D * PeriodicNoiseSampler.sampleLattice(this.noise.perms,
                lattice(x, xPeriod, xCells, RATE_3D) + this.noise.offsetX, y * RATE_3D_VERTICAL + this.noise.offsetY,
                lattice(z, zPeriod, zCells, RATE_3D) + this.noise.offsetZ, xCells, zCells);
    }

    static long cells(double period) {
        return cells(period, RATE);
    }

    static long cells(double period, double rate) {
        return period == UNBOUNDED
                ? PeriodicNoiseSampler.UNBOUNDED_PERIOD
                : Math.max(MIN_CELLS, (long) Math.ceil(period * rate - CELL_EPSILON));
    }

    private static double lattice(double coord, double period, long cells, double rate) {
        return period == UNBOUNDED ? coord * rate : coord * (cells / period);
    }
}
