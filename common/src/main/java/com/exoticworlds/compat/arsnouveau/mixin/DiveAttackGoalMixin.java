package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.hollingsworth.arsnouveau.common.entity.WildenStalker;
import com.hollingsworth.arsnouveau.common.entity.goal.stalker.DiveAttackGoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Mixin(value = DiveAttackGoal.class, remap = false)
public abstract class DiveAttackGoalMixin {
    @Shadow
    WildenStalker stalker;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$hitThroughSeam(AABB stalkerBox, AABB targetBox, Operation<Boolean> original) {
        Entity stalker = (Entity) (Object) this.stalker;
        return original.call(stalkerBox, FoldedBoxQuery.toward(stalker, stalker.position(), targetBox));
    }
}
