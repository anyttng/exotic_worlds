package com.exoticworlds.compat.wover.mixin;

import org.betterx.wover.math.api.noise.OpenSimplexNoise;
import org.betterx.wover.surface.api.Conditions;
import org.betterx.wover.surface.api.conditions.VolumeThresholdCondition;
import org.betterx.wover.surface.impl.numeric.NetherNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.LapNoise;
import com.exoticworlds.compat.StandInHolder;
import com.exoticworlds.compat.wover.WoverInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(NetherNoiseCondition.class)
public class NetherNoiseConditionMixin {
    @WrapOperation(
            method = WoverInjectionTargets.GET_NUMBER,
            at = @At(value = "INVOKE", target = "Lorg/betterx/wover/math/api/noise/OpenSimplexNoise;eval(DDD)D"))
    private double toroidal$lapNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original) {
        VolumeThresholdCondition volume = Conditions.NETHER_VOLUME_NOISE;
        return LapNoise.boundEval(((StandInHolder) volume).toroidal$standIn(), x, y, z, volume.getScaleX(),
                volume.getScaleZ(), () -> original.call(noise, x, y, z));
    }
}
