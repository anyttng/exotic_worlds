package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;

import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.noise.module.Frequency", remap = false)
public abstract class FrequencyMixin {
    @Shadow
    @Final
    private Noise xFreq;

    @Shadow
    @Final
    private Noise zFreq;

    // A coordinate multiplied by a varying factor has no lap left; only a constant factor carries the frame along.
    @WrapMethod(method = "compute")
    private float toroidal$carryLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, seed);
        }

        boolean constant = this.xFreq.minValue() == this.xFreq.maxValue()
                && this.zFreq.minValue() == this.zFreq.maxValue();
        try (RtfLap.Frame.Scope scaled = constant
                ? frame.scale(this.xFreq.minValue(), this.zFreq.minValue())
                : frame.open()) {
            return original.call(x, z, seed);
        }
    }
}
