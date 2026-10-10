package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(BlendedNoise.class)
public class BlendedNoiseMixin {
    @Unique
    private static final int MAIN_OCTAVES = 8;

    @Unique
    private static final int LIMIT_OCTAVES = 16;
    @Shadow
    @Final
    private PerlinNoise minLimitNoise;

    @Shadow
    @Final
    private PerlinNoise maxLimitNoise;

    @Shadow
    @Final
    private PerlinNoise mainNoise;

    @Shadow
    @Final
    private double xzMultiplier;

    @Shadow
    @Final
    private double yMultiplier;

    @Shadow
    @Final
    private double xzFactor;

    @Shadow
    @Final
    private double yFactor;

    @Shadow
    @Final
    private double smearScaleMultiplier;

    @SuppressWarnings("deprecation")
    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(context);
        }

        double blockX = context.blockX();
        double blockZ = context.blockZ();
        double limitY = context.blockY() * this.yMultiplier;
        double mainY = limitY / this.yFactor;
        double mainScale = this.xzMultiplier / this.xzFactor;
        double limitSmear = this.yMultiplier * this.smearScaleMultiplier;
        double mainSmear = limitSmear / this.yFactor;
        TranslationLattice lattice = generation.transformer().blockLattice();
        double limitX = lattice.foldX(context.blockX(), context.blockZ()) * this.xzMultiplier;
        double limitZ = lattice.foldZ(context.blockZ()) * this.xzMultiplier;
        double mainX = limitX / this.xzFactor;
        double mainZ = limitZ / this.xzFactor;
        boolean mainIndexable = toroidal$indexable(generation, this.mainNoise, MAIN_OCTAVES, mainScale);
        boolean minIndexable = toroidal$indexable(generation, this.minLimitNoise, LIMIT_OCTAVES, this.xzMultiplier);
        boolean maxIndexable = toroidal$indexable(generation, this.maxLimitNoise, LIMIT_OCTAVES, this.xzMultiplier);
        double blendMin = 0.0;
        double blendMax = 0.0;
        double mainNoiseValue = 0.0;
        double pow = 1.0;

        try (Context.ScaleScope scope = generation.openScale()) {
            for (int i = 0; i < MAIN_OCTAVES; i++) {
                ImprovedNoise noise = this.mainNoise.getOctaveNoise(i);
                if (noise != null) {
                    scope.rescale(mainScale * pow);
                    double wy = PerlinNoise.wrap(mainY * pow);
                    mainNoiseValue += (mainIndexable
                            ? noise.noise(blockX, wy, blockZ, mainSmear * pow, mainY * pow)
                            : toroidal$vanilla(generation, noise, PerlinNoise.wrap(mainX * pow), wy,
                                    PerlinNoise.wrap(mainZ * pow), mainSmear * pow, mainY * pow)) / pow;
                }

                pow /= 2.0;
            }

            double factor = (mainNoiseValue / 10.0 + 1.0) / 2.0;
            boolean isMax = factor >= 1.0;
            boolean isMin = factor <= 0.0;
            pow = 1.0;

            for (int i = 0; i < LIMIT_OCTAVES; i++) {
                double wx = PerlinNoise.wrap(limitX * pow);
                double wy = PerlinNoise.wrap(limitY * pow);
                double wz = PerlinNoise.wrap(limitZ * pow);
                double yScalePow = limitSmear * pow;
                scope.rescale(this.xzMultiplier * pow);
                if (!isMax) {
                    ImprovedNoise minNoise = this.minLimitNoise.getOctaveNoise(i);
                    if (minNoise != null) {
                        blendMin += (minIndexable
                                ? minNoise.noise(blockX, wy, blockZ, yScalePow, limitY * pow)
                                : toroidal$vanilla(generation, minNoise, wx, wy, wz, yScalePow, limitY * pow)) / pow;
                    }
                }

                if (!isMin) {
                    ImprovedNoise maxNoise = this.maxLimitNoise.getOctaveNoise(i);
                    if (maxNoise != null) {
                        blendMax += (maxIndexable
                                ? maxNoise.noise(blockX, wy, blockZ, yScalePow, limitY * pow)
                                : toroidal$vanilla(generation, maxNoise, wx, wy, wz, yScalePow, limitY * pow)) / pow;
                    }
                }

                pow /= 2.0;
            }

            return Mth.clampedLerp(factor, blendMin / 512.0, blendMax / 512.0) / 128.0;
        }
    }

    // The highest-frequency octave comes first here, and the period grows with the scale.
    @Unique
    private static boolean toroidal$indexable(Context generation, PerlinNoise stack, int octaves, double scale) {
        double pow = 1.0;
        for (int i = 0; i < octaves; i++) {
            ImprovedNoise noise = stack.getOctaveNoise(i);
            if (noise != null) {
                try (Context.ScaleScope scope = generation.openScale()) {
                    scope.rescale(scale * pow);
                    return ((IndexableNoise) (Object) noise).toroidal$indexable(generation);
                }
            }

            pow /= 2.0;
        }

        return true;
    }

    @Unique
    @SuppressWarnings("deprecation")
    private static double toroidal$vanilla(Context generation, ImprovedNoise noise, double x, double y, double z,
            double yScale, double yFudge) {
        try (Context.BindingScope _ = generation.unbound()) {
            return noise.noise(x, y, z, yScale, yFudge);
        }
    }
}
