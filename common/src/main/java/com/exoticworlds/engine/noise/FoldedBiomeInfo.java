package com.exoticworlds.engine.noise;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.periodic.PeriodicSimplexSampler;

import net.minecraft.world.level.biome.Biome;

public final class FoldedBiomeInfo {
    @SuppressWarnings("removal")
    public static double sample(Context generation, WorldFold fold, double divisor, int x, int z) {
        double scale = 1.0 / divisor;
        if (PeriodicSimplexSampler.canCarry(fold, scale)) {
            try (Context.ScaleScope _ = generation.withScale(scale)) {
                return Biome.BIOME_INFO_NOISE.getValue(x, z, false);
            }
        }

        TranslationLattice lattice = fold.getBlockLattice();
        try (Context.BindingScope _ = generation.unbound()) {
            return Biome.BIOME_INFO_NOISE.getValue(lattice.foldX(x, z) / divisor, lattice.foldZ(z) / divisor, false);
        }
    }

    private FoldedBiomeInfo() {
    }
}
