package com.exoticworlds.mixin;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

@Mixin(ServerLevel.class)
public class ServerLevelBiomeSearchMixin {
    @WrapMethod(method = "findClosestBiome3d")
    private @Nullable Pair<BlockPos, Holder<Biome>> exotic_worlds$biomeSearchThroughSeam(
            Predicate<Holder<Biome>> biomeTest,
            BlockPos origin,
            int maxSearchRadiusBlocks,
            int sampleResolutionHorizontal,
            int sampleResolutionVertical,
            Operation<@Nullable Pair<BlockPos, Holder<Biome>>> original) {
        WorldFold fold =
                WorldLoopAttachments.getWrappedFold((ServerLevel) (Object) this);
        if (fold == null) {
            return original.call(biomeTest, origin, maxSearchRadiusBlocks, sampleResolutionHorizontal,
                    sampleResolutionVertical);
        }

        int searchRadiusBlocks = exotic_worlds$radiusToCoverTheWorld(fold, maxSearchRadiusBlocks, sampleResolutionHorizontal);
        Pair<BlockPos, Holder<Biome>> found;
        try (GenerationFoldContext.Context.FoldScope _ = GenerationFoldContext.getContext().withFold(fold)) {
            found = original.call(biomeTest, origin, searchRadiusBlocks, sampleResolutionHorizontal,
                    sampleResolutionVertical);
        }

        return found == null ? null : Pair.of(fold.fold(found.getFirst()), found.getSecond());
    }

    @Unique
    private static int exotic_worlds$radiusToCoverTheWorld(WorldFold fold, int maxSearchRadiusBlocks,
            int sampleResolutionHorizontal) {
        int steps = Math.max(
                fold.getBlockLattice().getX().stepsToCoverTheWorld(sampleResolutionHorizontal),
                fold.getBlockLattice().getZ().stepsToCoverTheWorld(sampleResolutionHorizontal));
        return (int) Math.min(maxSearchRadiusBlocks, (long) steps * sampleResolutionHorizontal);
    }
}
