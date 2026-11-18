package com.exoticworlds.compat.lithostitched;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FNL;
import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FastNoiseConfig;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.CellularNoiseType;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.PerlinNoiseType;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.SimplexNoiseType;

class LapFastNoiseTest {
    @Test
    void foreignConfigClassIsRefusedByName() {
        LapFastNoise.Refusal refusal = LapFastNoise.refusal(FastNoiseConfig.class, FNL.NoiseType.Perlin,
                FNL.RotationType3D.None);

        assertEquals(new LapFastNoise.Refusal(LapFastNoise.Reason.UNKNOWN_CONFIG, FastNoiseConfig.class.getName()),
                refusal);
    }

    @Test
    void unfoldedNoiseTypeIsRefused() {
        LapFastNoise.Refusal refusal = LapFastNoise.refusal(PerlinNoiseType.class, FNL.NoiseType.ValueCubic,
                FNL.RotationType3D.None);

        assertEquals(new LapFastNoise.Refusal(LapFastNoise.Reason.NOISE_TYPE, FNL.NoiseType.ValueCubic.name()),
                refusal);
    }

    @Test
    void rotatedNoiseIsRefused() {
        LapFastNoise.Refusal refusal = LapFastNoise.refusal(SimplexNoiseType.class, FNL.NoiseType.OpenSimplex2S,
                FNL.RotationType3D.ImproveXZPlanes);

        assertEquals(new LapFastNoise.Refusal(LapFastNoise.Reason.ROTATION,
                FNL.RotationType3D.ImproveXZPlanes.name()), refusal);
    }

    @Test
    void shippedConfigsAreClosed() {
        assertNull(LapFastNoise.refusal(PerlinNoiseType.class, FNL.NoiseType.Perlin, FNL.RotationType3D.None));
        assertNull(LapFastNoise.refusal(SimplexNoiseType.class, FNL.NoiseType.OpenSimplex2S,
                FNL.RotationType3D.None));
        assertNull(LapFastNoise.refusal(CellularNoiseType.class, FNL.NoiseType.Cellular, FNL.RotationType3D.None));
    }
}
