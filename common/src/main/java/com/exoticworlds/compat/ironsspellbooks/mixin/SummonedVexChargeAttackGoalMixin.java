package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.SummonedVex;

import net.minecraft.world.phys.AABB;

@Mixin(targets = "io.redspace.ironsspellbooks.entity.mobs.SummonedVex$VexChargeAttackGoal", remap = false)
public abstract class SummonedVexChargeAttackGoalMixin {

    @Shadow
    @Final
    SummonedVex this$0;

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$touchThroughSeam(AABB vexBox, AABB targetBox, Operation<Boolean> original) {
        return original.call(vexBox, FoldedBoxQuery.toward(this.this$0, this.this$0.position(), targetBox));
    }
}
