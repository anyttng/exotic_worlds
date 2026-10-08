package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;
import com.exoticworlds.compat.reterraforged.RtfOctaves;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;
import raccoonman.reterraforged.world.worldgen.noise.module.Cubic;

@Mixin(value = Cubic.class, remap = false)
public abstract class CubicMixin {
    @Unique
    private static final Interpolation UNUSED_INTERPOLATION = Interpolation.LINEAR;

    @Shadow
    @Final
    private float frequency;

    @Shadow
    @Final
    private int octaves;

    @Shadow
    @Final
    private float lacunarity;

    @Shadow
    @Final
    private float gain;

    @Shadow
    @Final
    private float minValue;

    @Shadow
    @Final
    private float maxValue;

    @WrapMethod(method = "compute")
    private float toroidal$closeOnLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, seed);
        }

        float sum = RtfOctaves.sum(frame, (px, pz, octaveSeed, unused) -> Cubic.sample(px, pz, octaveSeed),
                UNUSED_INTERPOLATION, x, z, seed, this.octaves, this.frequency, this.lacunarity, this.gain, 1.0F);
        return NoiseUtil.map(sum, this.minValue, this.maxValue, this.maxValue - this.minValue);
    }
}
