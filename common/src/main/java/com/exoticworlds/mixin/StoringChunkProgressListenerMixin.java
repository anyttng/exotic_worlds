package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.CurrentServer;

import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

@Mixin(StoringChunkProgressListener.class)
public class StoringChunkProgressListenerMixin {
    @ModifyVariable(method = "onStatusChange", at = @At("HEAD"), argsOnly = true)
    private ChunkPos toroidal$foldStoredChunk(ChunkPos chunk) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(CurrentServer.get(), Level.OVERWORLD);
        return transformer == null ? chunk : new ChunkPos(transformer.foldChunkKey(chunk.toLong()));
    }
}
