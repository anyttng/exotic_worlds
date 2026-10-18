package com.exoticworlds.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.FoldedTemperatureCache;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.PeriodicSimplexSampler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

@Mixin(value = Biome.class, priority = 1100)
public class BiomeMixin {
    @Unique
    private static final String exotic_worlds$GET_HEIGHT_ADJUSTED_TEMPERATURE =
            "getHeightAdjustedTemperature(Lnet/minecraft/core/BlockPos;I)F";

    @Unique
    private static final float exotic_worlds$LARGE_VARIATION_AMPLITUDE = 7.0F;

    @Unique
    private static final double exotic_worlds$ICE_PATCH_THRESHOLD = 0.3;

    @Unique
    private static final double exotic_worlds$SMALL_VARIATION_THRESHOLD = 0.8;

    @Unique
    private static final float exotic_worlds$ICE_PATCH_TEMPERATURE = 0.2F;

    @Unique
    private final ThreadLocal<FoldedTemperatureCache> exotic_worlds$foldedTemperatureCache =
            ThreadLocal.withInitial(FoldedTemperatureCache::new);

    @WrapOperation(
            method = "getTemperature(Lnet/minecraft/core/BlockPos;I)F",
            at = @At(value = "INVOKE", target = "Ljava/lang/ThreadLocal;get()Ljava/lang/Object;"),
            require = 0)
    private Object exotic_worlds$temperatureCacheOfBoundFold(ThreadLocal<?> cache, Operation<Object> original) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(cache);
        }

        return this.exotic_worlds$foldedTemperatureCache.get().temperaturesUnder(transformer);
    }

    @WrapMethod(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z")
    private boolean exotic_worlds$freezeThroughSeam(
            LevelReader level, BlockPos pos, boolean checkNeighbors, Operation<Boolean> original) {
        return exotic_worlds$boundToLevel(level, () -> original.call(level, pos, checkNeighbors));
    }

    @WrapMethod(method = "shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z")
    private boolean exotic_worlds$snowThroughSeam(LevelReader level, BlockPos pos, Operation<Boolean> original) {
        return exotic_worlds$boundToLevel(level, () -> original.call(level, pos));
    }

    @Unique
    private <T> T exotic_worlds$boundToLevel(LevelReader level, Supplier<T> action) {
        WorldFold transformer = WorldLoopAttachments.noiseTransformerOfReader(level);
        if (transformer == null) {
            return action.get();
        }

        return GenerationTransformerContext.withTransformer(transformer, action);
    }

    @WrapOperation(
            method = exotic_worlds$GET_HEIGHT_ADJUSTED_TEMPERATURE,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome$TemperatureModifier;modifyTemperature(Lnet/minecraft/core/BlockPos;F)F"))
    private float exotic_worlds$periodicTemperatureModifier(
            Biome.TemperatureModifier modifier, BlockPos pos, float baseTemperature, Operation<Float> original) {
        if (modifier != Biome.TemperatureModifier.FROZEN) {
            return original.call(modifier, pos, baseTemperature);
        }

        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(modifier, pos, baseTemperature);
        }

        return exotic_worlds$frozenPatchTemperature(transformer, pos, baseTemperature);
    }

    @SuppressWarnings("removal")
    @Unique
    private static float exotic_worlds$frozenPatchTemperature(WorldFold transformer, BlockPos pos, float baseTemperature) {
        double largeVariation = PeriodicSimplexSampler.sampleStack(transformer, NoiseConstants.FROZEN_TEMPERATURE_SCALE,
                (NoiseStack) Biome.FROZEN_TEMPERATURE_NOISE, pos.getX(), pos.getZ()) * exotic_worlds$LARGE_VARIATION_AMPLITUDE;
        SimplexNoise biomeInfo = (SimplexNoise) Biome.BIOME_INFO_NOISE;
        double edgeVariation = PeriodicSimplexSampler.sample(biomeInfo, transformer,
                NoiseConstants.BIOME_INFO_EDGE_SCALE, pos.getX(), pos.getZ());
        if (largeVariation + edgeVariation < exotic_worlds$ICE_PATCH_THRESHOLD) {
            double smallVariation = PeriodicSimplexSampler.sample(biomeInfo, transformer,
                    NoiseConstants.BIOME_INFO_PATCH_SCALE, pos.getX(), pos.getZ());
            if (smallVariation < exotic_worlds$SMALL_VARIATION_THRESHOLD) {
                return exotic_worlds$ICE_PATCH_TEMPERATURE;
            }
        }

        return baseTemperature;
    }

    @WrapOperation(
            method = exotic_worlds$GET_HEIGHT_ADJUSTED_TEMPERATURE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/synth/Noise;get(DD)F"))
    private float exotic_worlds$rawCoordinateHeightNoise(Noise noise, double x, double z, Operation<Float> original,
            @Local(argsOnly = true) BlockPos pos) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(noise, x, z);
        }

        return PeriodicSimplexSampler.sample((SimplexNoise) noise, transformer, NoiseConstants.HEIGHT_TEMPERATURE_SCALE,
                pos.getX(), pos.getZ());
    }
}
