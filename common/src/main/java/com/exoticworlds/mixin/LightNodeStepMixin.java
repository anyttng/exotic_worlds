package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldHolder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;

@Mixin({BlockLightEngine.class, SkyLightEngine.class})
public abstract class LightNodeStepMixin {
    @ModifyExpressionValue(
            method = {"propagateIncrease", "propagateDecrease"},
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_OFFSET_PACKED),
            expect = 2)
    private long exotic_worlds$wrapToNode(long node) {
        return ((FoldHolder) (Object) this).exotic_worlds$fold().foldBlockNode(node);
    }
}
