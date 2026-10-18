package com.exoticworlds.compat.supplementaries.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.mehvahdjukaar.supplementaries.common.entities.RedMerchantEntity;
import net.minecraft.core.BlockPos;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.entities.RedMerchantEntity$MoveToGoal")
public class RedMerchantWanderTargetSeatMixin {
    @Shadow
    @Final
    RedMerchantEntity trader;

    @ModifyExpressionValue(
            method = { "canUse", "tick" },
            at = @At(value = "INVOKE",
                    target = "Lnet/mehvahdjukaar/supplementaries/common/entities/RedMerchantEntity;"
                            + "getWanderTarget()Lnet/minecraft/core/BlockPos;"))
    private @Nullable BlockPos toroidal$wanderTargetNearestTrader(@Nullable BlockPos wanderTarget) {
        return wanderTarget == null ? null : SeamSteering.nearestCopy(this.trader, wanderTarget);
    }
}
