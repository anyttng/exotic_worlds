package com.exoticworlds.engine.noise;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class FoldedPlacementCounts {
    private static final SimplexSlot THRESHOLD_SLOT = new SimplexSlot();

    public static int noiseBased(SimplexSlot slot, WorldFold fold, int x, int z, double noiseFactor,
            double noiseOffset, int noiseToCountRatio) {
        return (int) Math.ceil((biomeInfo(slot, fold, noiseFactor, x, z) + noiseOffset) * noiseToCountRatio);
    }

    public static int noiseThreshold(WorldFold fold, int x, int z, double noiseLevel, int belowNoise, int aboveNoise) {
        return biomeInfo(THRESHOLD_SLOT, fold, NoiseConstants.BIOME_INFO_THRESHOLD_DIVISOR, x, z) < noiseLevel
                ? belowNoise
                : aboveNoise;
    }

    @SuppressWarnings("removal")
    private static double biomeInfo(SimplexSlot slot, WorldFold fold, double divisor, int x, int z) {
        SimplexNoise noise = (SimplexNoise) Biome.BIOME_INFO_NOISE;
        SimplexField field = slot.field(fold, 1.0 / divisor, noise);
        if (field.carries()) {
            return (float) field.sample(noise, x, z);
        }

        TranslationLattice translations = fold.blockLattice();
        return noise.get(translations.foldX(x, z) / divisor, translations.foldZ(z) / divisor);
    }

    private FoldedPlacementCounts() {
    }
}
