package com.exoticworlds.compat.reterraforged;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.feature.placement.poisson.FastPoissonContext;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public final class PoissonLattice {
    public static long point(RtfLap.Frame frame, int seed, float x, float z, FastPoissonContext context) {
        float xFrequency = frame.snappedFrequency(Direction.Axis.X, context.frequency());
        float zFrequency = frame.snappedFrequency(Direction.Axis.Z, context.frequency());
        float shiftedX = frame.shift(Direction.Axis.X, x);
        float shiftedZ = frame.shift(Direction.Axis.Z, z);
        int cellX = NoiseUtil.floor(shiftedX * xFrequency);
        int cellZ = NoiseUtil.floor(shiftedZ * zFrequency);
        NoiseUtil.Vec2f jitter;
        try (RtfLap.Frame.Scope lattice = frame.lattice(frame.cells(Direction.Axis.X, context.frequency()),
                frame.cells(Direction.Axis.Z, context.frequency()))) {
            jitter = NoiseUtil.cell(seed, cellX, cellZ);
        }

        int px = NoiseUtil.floor((cellX + context.pad() + jitter.x() * context.jitter()) * (1.0F / xFrequency))
                + Math.round(x - shiftedX);
        int pz = NoiseUtil.floor((cellZ + context.pad() + jitter.y() * context.jitter()) * (1.0F / zFrequency))
                + Math.round(z - shiftedZ);
        return PosUtil.pack(px, pz);
    }

    private PoissonLattice() {
    }
}
