package com.exoticworlds.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedOrder;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(PlayerChunkSender.class)
public class PlayerChunkSenderMixin {
    @ModifyArg(
            method = "collectChunksToSend",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Comparators;least(ILjava/util/Comparator;)Ljava/util/stream/Collector;"),
            index = 1)
    private Comparator<Long> toroidal$nearestPendingThroughSeam(Comparator<Long> original,
            @Local(argsOnly = true) ChunkMap chunkMap, @Local(argsOnly = true) ChunkPos playerPos) {
        WorldFold transformer = toroidal$transformer(chunkMap);
        if (!transformer.isWrapped()) {
            return original;
        }

        return FoldedOrder.of(original, pending -> transformer.nearestCopy(playerPos, ChunkPos.unpack(pending)).pack());
    }

    @ModifyArg(
            method = "collectChunksToSend",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.STREAM_SORTED),
            index = 0)
    private Comparator<LevelChunk> toroidal$nearestLoadedThroughSeam(Comparator<LevelChunk> original,
            @Local(argsOnly = true) ChunkMap chunkMap, @Local(argsOnly = true) ChunkPos playerPos) {
        WorldFold transformer = toroidal$transformer(chunkMap);
        if (!transformer.isWrapped()) {
            return original;
        }

        return Comparator.comparingInt(chunk -> transformer.sqrChunkDistance(chunk.getPos(), playerPos));
    }

    @Unique
    private static WorldFold toroidal$transformer(ChunkMap chunkMap) {
        return WorldLoopAttachments.transformerOf(((LevelHolder) chunkMap).toroidal$level());
    }
}
