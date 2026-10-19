package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(targets = "com.cobblemon.mod.common.api.spawning.influence.detector.HabitatBlockDetector$detectFromInput$3$1")
public class HabitatRangeMixin {
    @WrapOperation(method = "appliesTo", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double exotic_worlds$habitatDistanceThroughSeam(BlockPos spawnPosition, Vec3i habitat,
            Operation<Double> original, @Local(argsOnly = true) SpawnablePosition spawnablePosition) {
        return SeamRange.sqr(spawnablePosition.getWorld(), spawnPosition, habitat);
    }
}
