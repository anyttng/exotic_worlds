package com.exoticworlds.compat.vista.mixin;

import java.util.function.BiPredicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.compat.vista.ChainedTvSeat;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(targets = "net.mehvahdjukaar.vista.common.chunk_tracking.ServerCameraChunkManager")
public class ChainedTvSeatMixin {
    @Unique
    private static final int NEAR_VIEW_FINDER_ARGUMENT = 2;

    @ModifyArg(method = "collectViewFindersShownByTvs", index = NEAR_VIEW_FINDER_ARGUMENT, at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/vista/common/chunk_tracking/ServerCameraChunkManager;"
                    + "collectViewFindersShownByTvs(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Set;"
                    + "Ljava/util/function/BiPredicate;Ljava/util/Set;Ljava/util/Set;I)V"))
    private static BiPredicate<Integer, Integer> exotic_worlds$tvNearestViewFinder(
            BiPredicate<Integer, Integer> nearViewFinder, @Local(argsOnly = true) ServerLevel level,
            @Local(ordinal = 0) ChunkPos viewFinder) {
        return ChainedTvSeat.beside(WorldLoopAttachments.foldOf(level), viewFinder, nearViewFinder);
    }
}
