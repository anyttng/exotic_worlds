package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.entity.npc.ai.MeleeAttackTask;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

@Mixin(MeleeAttackTask.class)
public class NpcMeleeMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private static boolean exotic_worlds$reachThroughSeam(AABB reach, AABB target, Operation<Boolean> original,
            @Local(argsOnly = true) LivingEntity entity) {
        return original.call(reach, FoldedBoxQuery.toward(entity, entity.position(), target));
    }
}
