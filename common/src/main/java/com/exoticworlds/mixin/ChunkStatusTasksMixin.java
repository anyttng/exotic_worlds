package com.exoticworlds.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.core.feature.CarvedChunkFeature;
import com.exoticworlds.core.feature.LitChunkFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.check.PeriodicityCheck;
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
                    "buildTerrain",
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
        WorldFold fold = WorldLoopAttachments.getFold(context.level());
        if (fold.isWrapped()) {
            PeriodicityCheck.verdictOf(context.level());
        }

        try (GenerationFoldContext.Context.FoldScope _ =
                GenerationFoldContext.getContext().withFold(fold, chunk.getPos())) {
            return original.call(context, step, chunks, chunk);
        }
    }

    @ModifyReturnValue(method = "buildTerrain", at = @At("RETURN"))
    private static CompletableFuture<ChunkAccess> exotic_worlds$runCarvedChunkFeatures(
            CompletableFuture<ChunkAccess> original,
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk) {
        return original.thenApply(carved -> {
            for (CarvedChunkFeature feature : ShapeFeatures.activeOn(context.level(), CarvedChunkFeature.class)) {
                feature.atCarved(context.level(), carved);
            }
            return carved;
        });
    }

    @ModifyReturnValue(method = "light", at = @At("RETURN"))
    private static CompletableFuture<ChunkAccess> exotic_worlds$runLitChunkFeatures(
            CompletableFuture<ChunkAccess> original,
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk) {
        return original.thenApply(lit -> {
            for (LitChunkFeature feature : ShapeFeatures.activeOn(context.level(), LitChunkFeature.class)) {
                feature.atLit(context.level(), lit, chunks, context.mainThreadExecutor());
            }
            return lit;
        });
    }
}
