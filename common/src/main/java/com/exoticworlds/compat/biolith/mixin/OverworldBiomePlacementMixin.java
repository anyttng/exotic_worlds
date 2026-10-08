package com.exoticworlds.compat.biolith.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.terraformersmc.biolith.impl.biome.DimensionBiomePlacement;
import com.terraformersmc.biolith.impl.biome.OverworldBiomePlacement;
import com.terraformersmc.biolith.impl.noise.OpenSimplexNoise2;
import com.exoticworlds.compat.LevelTemperature;
import com.exoticworlds.compat.biolith.ReplacementNoiseFold;

@Mixin(OverworldBiomePlacement.class)
public abstract class OverworldBiomePlacementMixin extends DimensionBiomePlacement {
    @Unique
    private static final double[] TOROIDAL_OCTAVE_WEIGHTS = {1.0, 1.0 / 8.0, 1.0 / 16.0, 1.0 / 32.0};

    @Unique
    private static final double TOROIDAL_SUM_DIVISOR = 1.21875;

    @Unique
    private final ReplacementNoiseFold toroidal$fold = new ReplacementNoiseFold(TOROIDAL_OCTAVE_WEIGHTS);

    @Unique
    private final LevelTemperature toroidal$temperature = new LevelTemperature();

    @Shadow
    private double[] scale;

    @Inject(method = "getLocalNoise", at = @At("HEAD"), cancellable = true)
    private void toroidal$foldedLocalNoise(int x, int y, int z, CallbackInfoReturnable<Double> cir) {
        OpenSimplexNoise2 noise = this.replacementNoise;
        if (noise == null) {
            return;
        }

        double sum = this.toroidal$fold.sum(noise.getSeed(), this.scale, x, z,
                this.toroidal$temperature.of(this.world));
        if (ReplacementNoiseFold.folded(sum)) {
            cir.setReturnValue(this.normalize(sum / TOROIDAL_SUM_DIVISOR));
        }
    }
}
