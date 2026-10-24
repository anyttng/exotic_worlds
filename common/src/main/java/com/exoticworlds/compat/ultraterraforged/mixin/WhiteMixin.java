package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.RtfLap;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.White;

@Mixin(value = White.class, remap = false)
public abstract class WhiteMixin {
    @Shadow
    @Final
    private float frequency;

    @WrapMethod(method = "compute")
    private float exotic_worlds$closeOnLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, seed);
        }

        try (RtfLap.Frame.Scope _ = frame.octave(this.frequency)) {
            return Math.abs(White.sample(frame.latticeU(x, z), frame.latticeV(z), seed));
        }
    }
}
