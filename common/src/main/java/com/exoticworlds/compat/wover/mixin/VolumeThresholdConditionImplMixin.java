package com.exoticworlds.compat.wover.mixin;

import org.betterx.wover.math.api.noise.OpenSimplexNoise;
import org.betterx.wover.surface.impl.conditions.VolumeThresholdConditionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.LapNoise;
import com.exoticworlds.compat.OpenSimplexStandIn;
import com.exoticworlds.compat.StandInHolder;
import com.exoticworlds.compat.wover.WoverInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.valueproviders.FloatProvider;

@Mixin(VolumeThresholdConditionImpl.class)
public class VolumeThresholdConditionImplMixin implements StandInHolder {
    @Shadow
    @Final
    public double scaleX;

    @Shadow
    @Final
    public double scaleZ;

    @Unique
    private OpenSimplexStandIn exotic_worlds$standIn;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$buildStandIn(long noiseSeed, double threshold, FloatProvider roughness, double scaleX,
            double scaleY, double scaleZ, CallbackInfo ci) {
        this.exotic_worlds$standIn = new OpenSimplexStandIn(noiseSeed);
    }

    @WrapOperation(method = "getValue(III)D", at = @At(value = "INVOKE", target = WoverInjectionTargets.NOISE_EVAL_3D))
    private double exotic_worlds$lapNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original) {
        return LapNoise.boundEval(this.exotic_worlds$standIn, x, y, z, this.scaleX, this.scaleZ,
                () -> original.call(noise, x, y, z));
    }

    @Override
    public OpenSimplexStandIn exotic_worlds$standIn() {
        return this.exotic_worlds$standIn;
    }
}
