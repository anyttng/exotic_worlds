package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import org.slf4j.Logger;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelHolder;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GeneratingChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;

@Mixin(ChunkGenerationTask.class)
public class ChunkGenerationTaskMixin {
    @WrapOperation(
            method = "create",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.STATIC_CACHE_2D_CREATE))
    private static StaticCache2D<GenerationChunkHolder> exotic_worlds$foldSeamSlots(
            int centerX,
            int centerZ,
            int rangeChunks,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            Operation<StaticCache2D<GenerationChunkHolder>> original,
            @Local(argsOnly = true) GeneratingChunkMap chunkMap) {
        if (!(chunkMap instanceof ChunkMap map)) {
            return original.call(centerX, centerZ, rangeChunks, initializer);
        }

        ServerLevel level = ((LevelHolder) (Object) map).exotic_worlds$getLevel();
        WorldFold fold = WorldLoopAttachments.getWrappedFold(level);
        if (fold == null) {
            return original.call(centerX, centerZ, rangeChunks, initializer);
        }

        String levelName = level.dimension().identifier().toString();
        StaticCache2D.Initializer<GenerationChunkHolder> folding = (slotX, slotZ) -> exotic_worlds$slotFor(
                map, fold, levelName, initializer, centerX, centerZ, slotX, slotZ);
        return original.call(centerX, centerZ, rangeChunks, folding);
    }

    @Unique
    private static final Logger exotic_worlds$LOGGER = LogUtils.getLogger();

    @Unique
    private static GenerationChunkHolder exotic_worlds$slotFor(
            ChunkMap map,
            WorldFold fold,
            String levelName,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            int centerX,
            int centerZ,
            int slotX,
            int slotZ) {
        if (slotX == centerX && slotZ == centerZ) {
            return initializer.get(slotX, slotZ);
        }

        ChunkPos slot = new ChunkPos(slotX, slotZ);
        if (!fold.isOver(slot)) {
            return initializer.get(slotX, slotZ);
        }

        long wrapped = fold.foldChunkKeyFrom(ChunkPos.pack(centerX, centerZ), ChunkPos.pack(slotX, slotZ));
        int wrappedX = ChunkPos.getX(wrapped);
        int wrappedZ = ChunkPos.getZ(wrapped);

        if (map.getUpdatingChunkIfPresent(wrapped) == null) {
            exotic_worlds$LOGGER.warn(
                    "missing_folded_holder level={} slot_x={} slot_z={} wrapped_x={} wrapped_z={} center_x={} center_z={}",
                    levelName, slotX, slotZ, wrappedX, wrappedZ, centerX, centerZ);
            return initializer.get(slotX, slotZ);
        }

        return initializer.get(wrappedX, wrappedZ);
    }
}
