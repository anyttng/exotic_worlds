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

import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.AABB;

@Mixin(OfferFlowerGoal.class)
public class OfferFlowerGoalMixin {
    @Shadow
    @Final
    private IronGolem golem;

    @WrapOperation(method = "stop", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean toroidal$giftTouchThroughSeam(AABB reach, AABB recipientBox, Operation<Boolean> original) {
        AABB folded = FoldedBoxQuery.toward(((TransformerSource) this.golem).toroidal$wrappedTransformer(),
                this.golem.position(), recipientBox);
        return original.call(reach, folded);
    }
}
