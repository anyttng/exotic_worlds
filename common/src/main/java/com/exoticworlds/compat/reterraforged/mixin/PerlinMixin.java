package com.exoticworlds.compat.reterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.terraforged.LapOctaves;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.reterraforged.ReTerraForgedInjectionTargets;

import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.noise.module.Perlin", remap = false)
public abstract class PerlinMixin {
    @Shadow
    @Final
    private float frequency;

    @Shadow
    @Final
    private float lacunarity;

    @WrapMethod(method = "compute")
    private float exotic_worlds$closeOnLap(float x, float z, int seed, Operation<Float> original,
            @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame,
            @Share(LapOctaves.BLOCK_X) LocalFloatRef blockX, @Share(LapOctaves.BLOCK_Z) LocalFloatRef blockZ,
            @Share(LapOctaves.FREQUENCY) LocalDoubleRef frequency) {
        LapOctaves.open(frame, blockX, blockZ, frequency, x, z, this.frequency);
        return original.call(x, z, seed);
    }

    @WrapOperation(method = "compute",
            at = @At(value = "INVOKE", target = ReTerraForgedInjectionTargets.PERLIN_SAMPLE))
    private float exotic_worlds$octaveOnLap(float x, float z, int seed, Interpolation interpolation,
            Operation<Float> original, @Share(LapOctaves.FRAME) LocalRef<RtfLap.@Nullable Frame> frame,
            @Share(LapOctaves.BLOCK_X) LocalFloatRef blockX, @Share(LapOctaves.BLOCK_Z) LocalFloatRef blockZ,
            @Share(LapOctaves.FREQUENCY) LocalDoubleRef frequency) {
        RtfLap.Frame lap = frame.get();
        if (lap == null) {
            return original.call(x, z, seed, interpolation);
        }

        try (RtfLap.Frame.Scope lattice = LapOctaves.next(lap, frequency, this.lacunarity)) {
            return original.call(lap.latticeU(blockX.get(), blockZ.get()), lap.latticeV(blockZ.get()), seed,
                    interpolation);
        }
    }
}
