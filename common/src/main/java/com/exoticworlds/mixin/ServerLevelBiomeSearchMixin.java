package com.exoticworlds.mixin;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
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
            int maxSearchRadius,
            int sampleResolutionHorizontal,
            int sampleResolutionVertical,
            Operation<@Nullable Pair<BlockPos, Holder<Biome>>> original) {
        WorldFold fold =
                WorldLoopAttachments.wrappedFoldOf((ServerLevel) (Object) this);
        if (fold == null) {
            return original.call(biomeTest, origin, maxSearchRadius, sampleResolutionHorizontal,
                    sampleResolutionVertical);
        }

        int searchRadius = exotic_worlds$radiusToCoverTheWorld(fold, maxSearchRadius, sampleResolutionHorizontal);
        Pair<BlockPos, Holder<Biome>> found = GenerationFoldContext.withFold(fold,
                () -> original.call(biomeTest, origin, searchRadius, sampleResolutionHorizontal,
                        sampleResolutionVertical));

        return found == null ? null : Pair.of(fold.fold(found.getFirst()), found.getSecond());
    }

    @Unique
    private static int exotic_worlds$radiusToCoverTheWorld(WorldFold fold, int maxSearchRadius,
            int sampleResolutionHorizontal) {
        int steps = Math.max(
                fold.blockLattice().x().stepsToCoverTheWorld(sampleResolutionHorizontal),
                fold.blockLattice().z().stepsToCoverTheWorld(sampleResolutionHorizontal));
        return (int) Math.min(maxSearchRadius, (long) steps * sampleResolutionHorizontal);
    }
}
