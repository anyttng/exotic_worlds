package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.reterraforged.ReTerraForgedRiverReach;

import raccoonman.reterraforged.world.worldgen.cell.rivermap.river.RiverWarp;

// The carve reads the warp under the open frame, so a warp keeps the scale of the lap it was built on.
@Mixin(value = RiverWarp.class, remap = false)
public abstract class RiverWarpMixin {
    @Unique
    private static final String CONSTRUCTOR = "<init>(IFFFF)V";

    @Unique
    private static final String NEW_WARP =
            "Lraccoonman/reterraforged/world/worldgen/cell/rivermap/river/RiverWarp;<init>(IFFFF)V";

    @Unique
    private static final String CREATE = "create(FFLjava/util/Random;)"
            + "Lraccoonman/reterraforged/world/worldgen/cell/rivermap/river/RiverWarp;";

    @Unique
    private static final String NOISE_CLAMP = "Lraccoonman/reterraforged/world/worldgen/noise/NoiseUtil;clamp(FFF)F";

    @Unique
    private static final String WIGGLE_PER_BLOCK = "floatValue=4.0E-4F";

    @Unique
    private static final int FREQUENCY = 3;

    @Unique
    private static final int SCALE = 4;

    @Unique
    private float exotic_worlds$lapScale;

    @Inject(method = CONSTRUCTOR, at = @At("RETURN"))
    private void exotic_worlds$keepTheLapScale(int seed, float lower, float upper, float frequency, float scale,
            CallbackInfo ci) {
        this.exotic_worlds$lapScale = ReTerraForgedRiverReach.INSTANCE.boundScale();
    }

    @ModifyArg(method = CREATE, at = @At(value = "INVOKE", target = NEW_WARP), index = FREQUENCY)
    private static float exotic_worlds$frequencyForTheLap(float frequency) {
        return frequency / ReTerraForgedRiverReach.INSTANCE.boundScale();
    }

    @ModifyArg(method = CREATE, at = @At(value = "INVOKE", target = NEW_WARP), index = SCALE)
    private static float exotic_worlds$scaleForTheLap(float scale) {
        return scale * ReTerraForgedRiverReach.INSTANCE.boundScale();
    }

    @ModifyExpressionValue(method = "getOffset", at = @At(value = "CONSTANT", args = WIGGLE_PER_BLOCK))
    private float exotic_worlds$wiggleOfTheFullSizeRiver(float perBlock) {
        return perBlock / this.exotic_worlds$lapScale;
    }

    @ModifyExpressionValue(method = "getOffset", at = @At(value = "INVOKE", target = NOISE_CLAMP))
    private float exotic_worlds$wiggleForTheLap(float distance) {
        return distance * this.exotic_worlds$lapScale;
    }
}
