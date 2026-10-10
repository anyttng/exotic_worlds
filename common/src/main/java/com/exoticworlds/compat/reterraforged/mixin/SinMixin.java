package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.noise.module.Sin", remap = false)
public abstract class SinMixin {
    @Shadow
    @Final
    private float frequency;

    @Shadow
    @Final
    private Noise alpha;

    @WrapMethod(method = "compute")
    private float toroidal$closeOnLap(float x, float z, int seed, Operation<Float> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return original.call(x, z, seed);
        }

        float blend = this.alpha.compute(x, z, seed);
        float angularX = frame.snappedAngular(Direction.Axis.X, this.frequency);
        float shear = frame.angularShear(angularX);
        float sx = NoiseUtil.sin(shear == RtfLap.UNSHEARED ? x * angularX : x * angularX + z * shear);
        float sz = NoiseUtil.sin(z * frame.snappedAngular(Direction.Axis.Z, this.frequency));
        float noise = blend == 0.0F ? sx : blend == 1.0F ? sz : NoiseUtil.lerp(sx, sz, blend);
        return NoiseUtil.map(noise, -1.0F, 1.0F, 2.0F);
    }
}
