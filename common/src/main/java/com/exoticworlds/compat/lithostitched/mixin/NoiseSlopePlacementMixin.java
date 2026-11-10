package com.exoticworlds.compat.lithostitched.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.worldgen.lithostitched.impl.worldgen.placementmodifier.NoiseSlopePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;

@Mixin(NoiseSlopePlacement.class)
public class NoiseSlopePlacementMixin {
    @WrapMethod(method = "count(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/core/BlockPos;)I")
    private int exotic_worlds$foldedCount(PlacementContext context, BlockPos pos, Operation<Integer> original) {
        WorldFold fold = GenerationFoldContext.context().wrappedFold();
        if (fold == null) {
            return original.call(context, pos);
        }

        NoiseSlopePlacement self = (NoiseSlopePlacement) (Object) this;
        RandomState state = context.getLevel().getLevel().getChunkSource().randomState();
        double value = ContextScaledNoise.sample(fold, state.getOrCreateNoise(self.noise()), self.xzScale(),
                pos.getX(), pos.getY() * self.yScale(), pos.getZ());
        return (int) Math.ceil(value * self.slope()) + self.offset();
    }
}
