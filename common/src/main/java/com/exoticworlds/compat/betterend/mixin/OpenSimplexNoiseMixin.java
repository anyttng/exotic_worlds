package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.compat.wover.StandInHolder;

@Mixin(OpenSimplexNoise.class)
public class OpenSimplexNoiseMixin implements StandInHolder {
    @Unique
    private long toroidal$seed;

    @Unique
    private OpenSimplexStandIn toroidal$standIn;

    @Inject(method = "<init>(J)V", at = @At("RETURN"))
    private void toroidal$keepSeed(long seed, CallbackInfo ci) {
        this.toroidal$seed = seed;
    }

    @Override
    public OpenSimplexStandIn toroidal$standIn() {
        OpenSimplexStandIn standIn = this.toroidal$standIn;
        if (standIn == null) {
            standIn = new OpenSimplexStandIn(this.toroidal$seed);
            this.toroidal$standIn = standIn;
        }

        return standIn;
    }
}
