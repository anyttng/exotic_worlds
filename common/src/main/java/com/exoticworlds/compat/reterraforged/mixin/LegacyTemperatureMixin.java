package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.noise.module.LegacyTemperature;

@Mixin(value = LegacyTemperature.class, remap = false)
public abstract class LegacyTemperatureMixin {
    @Shadow
    @Final
    private float frequency;

    // The band runs along Z alone: the original multiplies z by its own frequency, so z arrives pre-scaled to land on
    // whole turns per lap.
    @WrapMethod(method = "compute")
    private float toroidal$closeOnLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null || frame.lap(Direction.Axis.Z) == RtfLap.OPEN) {
            return original.call(x, z, seed);
        }

        float snapped = frame.snappedAngular(Direction.Axis.Z, this.frequency);
        return original.call(x, z * snapped / this.frequency, seed);
    }
}
