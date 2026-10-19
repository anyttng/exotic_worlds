package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.ai.CobblemonWanderControl;
import com.cobblemon.mod.common.api.ai.config.task.WanderTaskConfig;
import com.cobblemon.mod.common.api.ai.config.task.WaterWanderTaskConfig;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

@Mixin({WanderTaskConfig.class, WaterWanderTaskConfig.class})
public class WanderCenterMixin {
    @WrapOperation(
            method = "*",
            at = @At(value = "INVOKE",
                    target = "Lcom/cobblemon/mod/common/api/ai/CobblemonWanderControl;isSuitable(Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean exotic_worlds$candidateThroughSeam(CobblemonWanderControl control, BlockPos candidate,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity entity) {
        CobblemonWanderControl.WanderCenter center = control.getCenter();
        if (center == null) {
            return original.call(control, candidate);
        }

        BlockPos anchor = new BlockPos(center.getX().intValue(), center.getY().intValue(), center.getZ().intValue());
        return original.call(control, NearestCopy.toward(
                ((TransformerSource) entity).exotic_worlds$wrappedTransformer(), anchor, candidate));
    }
}
