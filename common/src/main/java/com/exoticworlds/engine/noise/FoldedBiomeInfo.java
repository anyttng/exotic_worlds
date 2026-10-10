package com.exoticworlds.engine.noise;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

import net.minecraft.world.level.biome.Biome;

public final class FoldedBiomeInfo {
    @SuppressWarnings("removal")
    public static double sample(Context generation, WorldFold transformer, double divisor, int x, int z) {
        double scale = 1.0 / divisor;
        if (PeriodicSimplexSampler.carries(transformer, scale)) {
            try (Context.ScaleScope _ = generation.withScale(scale)) {
                return Biome.BIOME_INFO_NOISE.getValue(x, z, false);
            }
        }

        TranslationLattice lattice = transformer.blockLattice();
        try (Context.BindingScope _ = generation.unbound()) {
            return Biome.BIOME_INFO_NOISE.getValue(lattice.foldX(x, z) / divisor, lattice.foldZ(z) / divisor, false);
        }
    }

    private FoldedBiomeInfo() {
    }
}
