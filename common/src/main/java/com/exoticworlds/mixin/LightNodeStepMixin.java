package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.accessors.LightStepFrame;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.LightStep;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;

@Mixin({BlockLightEngine.class, SkyLightEngine.class})
public abstract class LightNodeStepMixin {
    @ModifyExpressionValue(
            method = {"propagateIncrease", "propagateDecrease"},
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_OFFSET_PACKED),
            expect = 2)
    private long exotic_worlds$wrapToNode(long node) {
        WorldFold fold = ((FoldHolder) (Object) this).exotic_worlds$fold();
        long folded = fold.foldBlockNode(node);
        ((LightStepFrame) (Object) this).exotic_worlds$setStepOrientation(LightStep.orientationOf(fold, node, folded));
        return folded;
    }

    @ModifyExpressionValue(
            method = {"propagateIncrease", "propagateDecrease"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/Direction;getOpposite()Lnet/minecraft/core/Direction;"),
            expect = 3)
    private Direction exotic_worlds$turnBackDirection(Direction backDirection) {
        return LightStep.inTargetFrame(
                ((LightStepFrame) (Object) this).exotic_worlds$stepOrientation(), backDirection);
    }

    @ModifyArg(
            method = "propagateIncrease",
            at = @At(
                    value = "INVOKE",
                    target = "shapeOccludes(Lnet/minecraft/world/level/block/state/BlockState;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"),
            index = 1)
    private BlockState exotic_worlds$readTargetInSourceFrame(BlockState toState) {
        return LightStep.inSourceFrame(((LightStepFrame) (Object) this).exotic_worlds$stepOrientation(), toState);
    }
}
