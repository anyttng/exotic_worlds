package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.engine.gen.SpawnSearch;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;

@Mixin(Climate.Sampler.class)
public class ClimateSamplerSpawnOriginMixin {
    @ModifyExpressionValue(method = "findSpawnPosition",
            at = @At(value = "FIELD", target = "Lnet/minecraft/core/BlockPos;ZERO:Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$originAtTheSpawnPoint(BlockPos origin) {
        SpawnPoint point = SpawnSearch.bound();
        return point.equals(SpawnPoint.ZERO) ? origin : new BlockPos(point.blockX(), origin.getY(), point.blockZ());
    }
}
