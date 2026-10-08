package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    @WrapOperation(
            method = "isRightDistanceToPlayerAndSpawnPoint",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private static boolean toroidal$spawnRadiusThroughSeam(BlockPos spawnPos, Position candidate, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) ServerLevel level) {
        return SeamRange.closerToCenterThan(level, spawnPos, candidate, distance);
    }

    @ModifyExpressionValue(
            method = "createState",
            at = @At(value = "NEW", target = "net/minecraft/world/level/PotentialCalculator"))
    private static PotentialCalculator toroidal$bindSpawnPotentialToLevel(PotentialCalculator spawnPotential,
            @Local(argsOnly = true) ServerLevel level) {
        ((TransformerHolder) spawnPotential).toroidal$setTransformer(WorldLoopAttachments.transformerOf(level));
        return spawnPotential;
    }
}
