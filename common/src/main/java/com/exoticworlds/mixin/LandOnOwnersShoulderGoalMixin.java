package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.ai.goal.LandOnOwnersShoulderGoal;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import net.minecraft.world.phys.AABB;

@Mixin(LandOnOwnersShoulderGoal.class)
public class LandOnOwnersShoulderGoalMixin {
    @Shadow
    @Final
    private ShoulderRidingEntity entity;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean toroidal$touchOwnerThroughSeam(AABB riderBox, AABB ownerBox, Operation<Boolean> original) {
        AABB folded = FoldedBoxQuery.toward(((TransformerSource) this.entity).toroidal$wrappedTransformer(),
                this.entity.position(), ownerBox);
        return original.call(riderBox, folded);
    }
}
