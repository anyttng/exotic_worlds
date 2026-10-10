package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.noise.module.Warp", remap = false)
public abstract class WarpMixin {
    @WrapMethod(method = "compute")
    private float toroidal$shiftBeforeWarp(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, seed);
        }

        return original.call(frame.shiftX(x, z), frame.shiftZ(z), seed);
    }
}
