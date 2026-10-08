package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;
import com.exoticworlds.compat.reterraforged.RtfOctaves;
import com.exoticworlds.compat.reterraforged.SimplexOnSquare;

import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.noise.module.SimplexRidge", remap = false)
public abstract class SimplexRidgeMixin {
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
    private Interpolation interpolation;

    @Shadow
    @Final
    private float[] spectralWeights;

    @Shadow
    @Final
    private float min;

    @Shadow
    @Final
    private float max;

    @WrapMethod(method = "compute")
    private float toroidal$closeOnLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, seed);
        }

        float value = RtfOctaves.ridge(frame, SimplexOnSquare::simplex2, this.interpolation, x, z, seed, this.octaves,
                this.frequency * SimplexOnSquare.RATE, this.lacunarity, this.gain, this.spectralWeights);
        return NoiseUtil.map(value, this.min, this.max, Math.abs(this.max - this.min));
    }
}
