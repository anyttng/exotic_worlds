package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;

@Mixin({BlockLightEngine.class, SkyLightEngine.class})
public abstract class LightNodeStepMixin {
    @WrapOperation(
            method = {"propagateIncrease", "propagateDecrease"},
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_OFFSET_PACKED),
            expect = 2)
    private long exotic_worlds$wrapToNode(long node, Direction direction, Operation<Long> original) {
        return ((FoldHolder) (Object) this).exotic_worlds$fold().foldBlockNode(original.call(node, direction));
    }
}
