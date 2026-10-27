package com.exoticworlds.engine.noise;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;

import net.minecraft.world.level.biome.Biome;

public final class FoldedBiomeInfo {
    @SuppressWarnings("removal")
    public static double sample(Context generation, WorldFold fold, double divisor, int x, int z) {
        double scale = 1.0 / divisor;
        if (PeriodicSimplexSampler.carries(fold, scale)) {
            try (Context.ScaleScope scope = generation.withScale(scale)) {
                return Biome.BIOME_INFO_NOISE.getValue(x, z, false);
            }
        }

        TranslationLattice lattice = fold.blockLattice();
        try (Context.BindingScope bindingScope = generation.unbound()) {
            return Biome.BIOME_INFO_NOISE.getValue(lattice.foldX(x, z) / divisor, lattice.foldZ(z) / divisor, false);
        }
    }

    private FoldedBiomeInfo() {
    }
}
