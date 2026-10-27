package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

@Mixin(PokemonBattle.class)
public class FleeDistanceMixin {
    @WrapOperation(method = "checkFlee()V", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double exotic_worlds$fleeDistanceThroughSeam(Vec3 wild, Vec3 player, Operation<Double> original,
            @Local ServerLevel world) {
        return original.call(wild, WorldLoopAttachments.foldOf(world).nearestCopy(wild, player));
    }
}
