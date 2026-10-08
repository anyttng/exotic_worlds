package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.material.MaterialSystem;
import net.minecraft.world.level.levelgen.synth.Noise;

@Mixin(MaterialSystem.class)
public class MaterialSystemMixin {
    @Shadow
    @Final
    private Noise badlandsPillarNoise;

    @Shadow
    @Final
    private Noise badlandsPillarRoofNoise;

    @Shadow
    @Final
    private Noise icebergPillarNoise;

    @Shadow
    @Final
    private Noise icebergPillarRoofNoise;

    @WrapOperation(
            method = {"getSurfaceDepth", "getSurfaceSecondary", "getBand"},
            at = @At(value = "INVOKE", target = InjectionTargets.NOISE_GET))
    private float toroidal$blockPositionNoise(Noise noise, double x, double y, double z, Operation<Float> original) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(noise, x, y, z);
        }

        return ContextScaledNoise.sample(transformer, noise, NoiseConstants.UNSCALED, x, y, z);
    }

    @WrapOperation(method = "erodedBadlandsExtension", at = @At(value = "INVOKE", target = InjectionTargets.NOISE_GET))
    private float toroidal$rawBadlandsNoise(Noise noise, double x, double y, double z, Operation<Float> original,
            @Local(argsOnly = true, ordinal = 0) int blockX, @Local(argsOnly = true, ordinal = 1) int blockZ) {
        return this.toroidal$rawCoordinateNoise(noise, x, y, z, blockX, blockZ, original);
    }

    @WrapOperation(method = "frozenOceanExtension", at = @At(value = "INVOKE", target = InjectionTargets.NOISE_GET))
    private float toroidal$rawIcebergNoise(Noise noise, double x, double y, double z, Operation<Float> original,
            @Local(argsOnly = true, ordinal = 1) int blockX, @Local(argsOnly = true, ordinal = 2) int blockZ) {
        return this.toroidal$rawCoordinateNoise(noise, x, y, z, blockX, blockZ, original);
    }

    @Unique
    private float toroidal$rawCoordinateNoise(Noise noise, double x, double y, double z, int blockX, int blockZ,
            Operation<Float> original) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(noise, x, y, z);
        }

        return ContextScaledNoise.sample(transformer, noise, this.toroidal$scaleOf(noise), blockX, y, blockZ);
    }

    @Unique
    private double toroidal$scaleOf(Noise noise) {
        if (noise == this.badlandsPillarNoise) {
            return NoiseConstants.BADLANDS_PILLAR_SCALE;
        }

        if (noise == this.badlandsPillarRoofNoise) {
            return NoiseConstants.BADLANDS_PILLAR_ROOF_SCALE;
        }

        if (noise == this.icebergPillarNoise) {
            return NoiseConstants.ICEBERG_PILLAR_SCALE;
        }

        if (noise == this.icebergPillarRoofNoise) {
            return NoiseConstants.ICEBERG_PILLAR_ROOF_SCALE;
        }

        return NoiseConstants.UNSCALED;
    }
}
