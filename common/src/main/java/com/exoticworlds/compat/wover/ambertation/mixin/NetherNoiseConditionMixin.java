package com.exoticworlds.compat.wover.ambertation.mixin;

import de.ambertation.wover.math.api.noise.OpenSimplexNoise;
import de.ambertation.wover.surface.api.Conditions;
import de.ambertation.wover.surface.api.conditions.VolumeThresholdCondition;
import de.ambertation.wover.surface.impl.numeric.NetherNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.worldgen.LapNoise;
import com.exoticworlds.compat.worldgen.StandInHolder;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(NetherNoiseCondition.class)
public class NetherNoiseConditionMixin {
    @WrapOperation(
            method = "getNumber(Lde/ambertation/wover/surface/api/conditions/SurfaceRulesContext;)I",
            at = @At(value = "INVOKE", target = "Lde/ambertation/wover/math/api/noise/OpenSimplexNoise;eval(DDD)D"))
    private double exotic_worlds$lapNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        if (fold == null) {
            return original.call(noise, x, y, z);
        }

        VolumeThresholdCondition volume = Conditions.NETHER_VOLUME_NOISE;
        return LapNoise.eval(((StandInHolder) volume).exotic_worlds$getStandIn(), fold, x, y, z, volume.getScaleX(),
                volume.getScaleZ());
    }
}
