package com.exoticworlds.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.gen.FloatingCrumbs;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.PeriodicityCheck;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;

@Mixin(ChunkStatusTasks.class)
public class ChunkStatusTasksMixin {
    @WrapMethod(
            method = {
                    "generateStructureStarts",
                    "generateStructureReferences",
                    "generateBiomes",
                    "generateNoise",
                    "generateSurface",
                    "generateCarvers",
                    "generateFeatures",
                    "generateSpawn",
                    "initializeLight",
                    "light"
            },
            order = BinderOrder.FOLD)
    private static CompletableFuture<ChunkAccess> exotic_worlds$bindFold(
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk,
            Operation<CompletableFuture<ChunkAccess>> original) {
        WorldFold fold = WorldLoopAttachments.foldOf(context.level());
        if (fold.isWrapped()) {
            PeriodicityCheck.runOnce(context.level(), fold);
        }

        return GenerationFoldContext.withFold(fold,
                () -> original.call(context, step, chunks, chunk));
    }

    @ModifyReturnValue(method = "generateCarvers", at = @At("RETURN"))
    private static CompletableFuture<ChunkAccess> exotic_worlds$sweepFloatingCrumbs(
            CompletableFuture<ChunkAccess> original,
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk) {
        return original.thenApply(carved -> {
            FloatingCrumbs.sweep(context.level(), carved);
            FloatingCrumbs.registerMask(context.level(), carved);
            return carved;
        });
    }

    @ModifyReturnValue(method = "light", at = @At("RETURN"))
    private static CompletableFuture<ChunkAccess> exotic_worlds$sweepBorderCrumbs(
            CompletableFuture<ChunkAccess> original,
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk) {
        return original.thenApply(lit -> {
            FloatingCrumbs.sweepAcross(context.level(), lit, chunks, context.mainThreadExecutor());
            return lit;
        });
    }
}
