package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.OpenSimplexStandIn;
import com.exoticworlds.compat.StandInHolder;

@Mixin(OpenSimplexNoise.class)
public class OpenSimplexNoiseMixin implements StandInHolder {
    @Unique
    private long exotic_worlds$seed;

    @Unique
    private OpenSimplexStandIn exotic_worlds$standIn;

    @Inject(method = "<init>(J)V", at = @At("RETURN"))
    private void exotic_worlds$keepSeed(long seed, CallbackInfo ci) {
        this.exotic_worlds$seed = seed;
    }

    @Override
    public OpenSimplexStandIn exotic_worlds$standIn() {
        OpenSimplexStandIn standIn = this.exotic_worlds$standIn;
        if (standIn == null) {
            standIn = new OpenSimplexStandIn(this.exotic_worlds$seed);
            this.exotic_worlds$standIn = standIn;
        }

        return standIn;
    }
}
