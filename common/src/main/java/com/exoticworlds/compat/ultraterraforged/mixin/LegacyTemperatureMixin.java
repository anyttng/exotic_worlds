package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.LegacyTemperature;

@Mixin(value = LegacyTemperature.class, remap = false)
public abstract class LegacyTemperatureMixin {
    @Shadow
    @Final
    private float frequency;

    // The band runs along Z alone, and z arrives pre-scaled so its own frequency lands on whole turns per lap.
    @WrapMethod(method = "compute")
    private float exotic_worlds$closeOnLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null || frame.lap(Direction.Axis.Z) == RtfLap.OPEN) {
            return original.call(x, z, seed);
        }

        float snapped = frame.snappedAngular(Direction.Axis.Z, this.frequency);
        return original.call(x, z * snapped / this.frequency, seed);
    }
}
