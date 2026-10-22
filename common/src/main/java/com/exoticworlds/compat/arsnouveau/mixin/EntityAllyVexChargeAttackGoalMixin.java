package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.hollingsworth.arsnouveau.common.entity.EntityAllyVex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "com.hollingsworth.arsnouveau.common.entity.EntityAllyVex$ChargeAttackGoal", remap = false)
public abstract class EntityAllyVexChargeAttackGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private EntityAllyVex vex;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$hitThroughSeam(AABB vexBox, AABB targetBox, Operation<Boolean> original) {
        Entity vex = (Entity) (Object) this.vex;
        return original.call(vexBox, FoldedBoxQuery.toward(vex, vex.position(), targetBox));
    }
}
