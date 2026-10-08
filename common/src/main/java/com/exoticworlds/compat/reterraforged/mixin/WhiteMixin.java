package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.noise.module.White;

@Mixin(value = White.class, remap = false)
public abstract class WhiteMixin {
    @Shadow
    @Final
    private float frequency;

    @WrapMethod(method = "compute")
    private float toroidal$closeOnLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, seed);
        }

        try (RtfLap.Frame.Scope lattice = frame.octave(this.frequency)) {
            float latticeX = frame.shift(Direction.Axis.X, x) * frame.xScale();
            float latticeZ = frame.shift(Direction.Axis.Z, z) * frame.zScale();
            return Math.abs(White.sample(latticeX, latticeZ, seed));
        }
    }
}
