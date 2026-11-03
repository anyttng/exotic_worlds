package com.exoticworlds.compat.terraforged;

public final class PoissonLattice {
    public static long point(TerraForgedNoise noise, RtfLap.Frame frame, int seed, float x, float z, float frequency,
            float pad, float jitter) {
        try (RtfLap.Frame.Scope lattice = frame.octave(frequency)) {
            float xFrequency = frame.xScale();
            float zFrequency = frame.zScale();
            float latticeX = frame.latticeX(x, z);
            float shiftedZ = frame.shiftZ(z);
            int cellX = noise.floor(latticeX * xFrequency);
            int cellZ = noise.floor(shiftedZ * zFrequency);
            float pointX = (cellX + pad + noise.jitterX(seed, cellX, cellZ) * jitter) * (1.0F / xFrequency);
            float pointZ = (cellZ + pad + noise.jitterZ(seed, cellX, cellZ) * jitter) * (1.0F / zFrequency);
            int px = noise.floor(frame.outerX(pointX, pointZ)) + Math.round(x - frame.outerX(latticeX, shiftedZ));
            int pz = noise.floor(pointZ) + Math.round(z - shiftedZ);
            return noise.pack(px, pz);
        }
    }

    private PoissonLattice() {
    }
}
