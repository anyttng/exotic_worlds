package com.exoticworlds.compat.reterraforged;

import raccoonman.reterraforged.world.worldgen.feature.placement.poisson.FastPoissonContext;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public final class PoissonLattice {
    public static long point(RtfLap.Frame frame, int seed, float x, float z, FastPoissonContext context) {
        try (RtfLap.Frame.Scope lattice = frame.octave(context.frequency())) {
            float xFrequency = frame.xScale();
            float zFrequency = frame.zScale();
            float latticeX = frame.latticeX(x, z);
            float shiftedZ = frame.shiftZ(z);
            int cellX = NoiseUtil.floor(latticeX * xFrequency);
            int cellZ = NoiseUtil.floor(shiftedZ * zFrequency);
            NoiseUtil.Vec2f jitter = NoiseUtil.cell(seed, cellX, cellZ);
            float pointX = (cellX + context.pad() + jitter.x() * context.jitter()) * (1.0F / xFrequency);
            float pointZ = (cellZ + context.pad() + jitter.y() * context.jitter()) * (1.0F / zFrequency);
            int px = NoiseUtil.floor(frame.outerX(pointX, pointZ)) + Math.round(x - frame.outerX(latticeX, shiftedZ));
            int pz = NoiseUtil.floor(pointZ) + Math.round(z - shiftedZ);
            return PosUtil.pack(px, pz);
        }
    }

    private PoissonLattice() {
    }
}
