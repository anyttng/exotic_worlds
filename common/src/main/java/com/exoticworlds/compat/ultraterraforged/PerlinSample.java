package com.exoticworlds.compat.ultraterraforged;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.Interpolation;

// Perlin.sample restated: its record is package-private, and Perlin2's public twin reads other gradients.
public final class PerlinSample {
    public static float sample(float x, float y, int seed, Interpolation interpolation) {
        int x0 = NoiseUtil.floor(x);
        int y0 = NoiseUtil.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        float xs = interpolation.apply(x - x0);
        float ys = interpolation.apply(y - y0);
        float xd0 = x - x0;
        float yd0 = y - y0;
        float xd1 = xd0 - 1.0F;
        float yd1 = yd0 - 1.0F;
        float xf0 = NoiseUtil.lerp(NoiseUtil.gradCoord2D(seed, x0, y0, xd0, yd0),
                NoiseUtil.gradCoord2D(seed, x1, y0, xd1, yd0), xs);
        float xf1 = NoiseUtil.lerp(NoiseUtil.gradCoord2D(seed, x0, y1, xd0, yd1),
                NoiseUtil.gradCoord2D(seed, x1, y1, xd1, yd1), xs);
        return NoiseUtil.lerp(xf0, xf1, ys);
    }

    private PerlinSample() {
    }
}
