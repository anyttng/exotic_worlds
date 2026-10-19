package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;

@Mixin(targets = "com.cobblemon.mod.common.api.ai.config.task.PathToBeeHiveTaskConfig$createTask$1$start$$inlined$sortedBy$1")
public class HiveSideOrderMixin {
    @Shadow
    @Final
    LivingEntity $entity$inlined;

    @WrapOperation(method = "compare", require = 2,
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double exotic_worlds$sideDistanceThroughSeam(BlockPos side, Vec3i body, Operation<Double> original) {
        return SeamRange.sqr(this.$entity$inlined, side, body);
    }
}
