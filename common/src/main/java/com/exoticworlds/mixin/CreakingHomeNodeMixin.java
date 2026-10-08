package com.exoticworlds.mixin;

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
import net.minecraft.world.entity.monster.creaking.Creaking;

@Mixin(targets = "net.minecraft.world.entity.monster.creaking.Creaking$HomeNodeEvaluator")
public class CreakingHomeNodeMixin {
    @Shadow
    @Final
    Creaking this$0;

    @WrapOperation(
            method = "getPathType",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR),
            require = 2)
    private double toroidal$homeDistanceThroughSeam(BlockPos home, Vec3i pos, Operation<Double> original) {
        return SeamRange.sqr(this.this$0, home, pos);
    }
}
