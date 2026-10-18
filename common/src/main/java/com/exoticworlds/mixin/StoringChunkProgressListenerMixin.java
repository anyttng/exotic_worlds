package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.CurrentServer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

@Mixin(StoringChunkProgressListener.class)
public class StoringChunkProgressListenerMixin {
    @ModifyVariable(method = "onStatusChange", at = @At("HEAD"), argsOnly = true)
    private ChunkPos exotic_worlds$foldStoredChunk(ChunkPos chunk) {
        MinecraftServer server = CurrentServer.get();
        WorldFold transformer =
                WorldLoopAttachments.transformerIfLevel(server == null ? null : server.getLevel(Level.OVERWORLD));
        return new ChunkPos(transformer.foldChunkKey(chunk.toLong()));
    }
}
