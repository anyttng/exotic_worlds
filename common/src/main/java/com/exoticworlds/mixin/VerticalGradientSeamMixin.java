package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$VerticalGradientConditionSource$1VerticalGradientCondition")
public class VerticalGradientSeamMixin {
    @WrapOperation(
            method = "compute",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.POSITIONAL_RANDOM_FACTORY_AT))
    private RandomSource toroidal$seedGradientFromCanonical(
            PositionalRandomFactory factory, int blockX, int blockY, int blockZ, Operation<RandomSource> original) {
        long canonical = GenerationTransformerContext.context().transformer()
                .foldBlockNode(BlockPos.asLong(blockX, blockY, blockZ));
        return original.call(factory, BlockPos.getX(canonical), blockY, BlockPos.getZ(canonical));
    }
}
