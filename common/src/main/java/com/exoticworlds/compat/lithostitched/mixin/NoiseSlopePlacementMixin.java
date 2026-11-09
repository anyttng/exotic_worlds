package com.exoticworlds.compat.lithostitched.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.worldgen.lithostitched.worldgen.placementmodifier.NoiseSlopePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;

@Mixin(NoiseSlopePlacement.class)
public class NoiseSlopePlacementMixin {
    @WrapMethod(method = "count(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/core/BlockPos;)I")
    private int exotic_worlds$foldedCount(PlacementContext context, BlockPos pos, Operation<Integer> original) {
        Context generation = GenerationFoldContext.context();
        if (!generation.fold().isWrapped()) {
            return original.call(context, pos);
        }

        NoiseSlopePlacement self = (NoiseSlopePlacement) (Object) this;
        RandomState state = context.getLevel().getLevel().getChunkSource().randomState();
        double value = ContextScaledNoise.sample(generation, state.getOrCreateNoise(self.noise()),
                pos.getX(), pos.getY() * self.yScale(), pos.getZ(), self.xzScale());
        return (int) Math.ceil(value * self.slope()) + self.offset();
    }
}
