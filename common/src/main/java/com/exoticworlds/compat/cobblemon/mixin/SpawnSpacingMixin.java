package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.spawning.SpawningZone;
import com.cobblemon.mod.common.api.spawning.position.AreaSpawnablePositionResolver;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;

@Mixin(AreaSpawnablePositionResolver.class)
public interface SpawnSpacingMixin {
    @WrapOperation(method = "resolve", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_CLOSER_THAN))
    private boolean exotic_worlds$entitySpacingThroughSeam(Vec3 entityPosition, Position spawnPosition,
            double distance, Operation<Boolean> original, @Local(argsOnly = true) SpawningZone zone) {
        return SeamRange.closerThan(zone.getWorld(), entityPosition, spawnPosition, distance);
    }
}
