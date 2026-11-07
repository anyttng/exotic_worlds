package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.reterraforged.ReTerraForgedRiverReach;

import raccoonman.reterraforged.world.worldgen.cell.rivermap.river.BaseRiverGenerator;

@Mixin(value = BaseRiverGenerator.class, remap = false)
public abstract class BaseRiverGeneratorMixin {
    @Unique
    private static final String FORK_MIN_LENGTH = "floatValue=300.0F";

    @Unique
    private static final String OVERLAP_DISTANCE = "floatValue=250.0F";

    @Unique
    private static final String VARIANCE_NEXT =
            "Lraccoonman/reterraforged/world/worldgen/util/Variance;next(Ljava/util/Random;)F";

    @ModifyExpressionValue(method = "generateForks", at = @At(value = "CONSTANT", args = FORK_MIN_LENGTH))
    private float exotic_worlds$forkSizedForTheLap(float blocks) {
        return blocks * ReTerraForgedRiverReach.INSTANCE.boundScale();
    }

    @ModifyExpressionValue(method = "riverOverlaps", at = @At(value = "CONSTANT", args = OVERLAP_DISTANCE))
    private float exotic_worlds$overlapSizedForTheLap(float blocks) {
        return blocks * ReTerraForgedRiverReach.INSTANCE.boundScale();
    }

    @ModifyExpressionValue(method = "generateWetlands", at = @At(value = "INVOKE", target = VARIANCE_NEXT),
            require = 2)
    private float exotic_worlds$wetlandSizedForTheLap(float blocks) {
        return blocks * ReTerraForgedRiverReach.INSTANCE.boundScale();
    }
}
