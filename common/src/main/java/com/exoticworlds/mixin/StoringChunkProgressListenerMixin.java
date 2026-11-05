package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.CurrentServer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

@Mixin(StoringChunkProgressListener.class)
public class StoringChunkProgressListenerMixin {
    @Unique
    private WorldFold exotic_worlds$gridFold = WorldFolds.NOOP;

    @Inject(method = "updateSpawnPos", at = @At("HEAD"))
    private void exotic_worlds$bindGridFold(ChunkPos spawn, CallbackInfo ci) {
        MinecraftServer server = CurrentServer.get();
        this.exotic_worlds$gridFold =
                WorldLoopAttachments.foldIfLevel(server == null ? null : server.getLevel(Level.OVERWORLD));
    }

    @ModifyExpressionValue(method = "getStatus", at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_AS_LONG))
    private long exotic_worlds$physicalCellKey(long key) {
        return this.exotic_worlds$gridFold.foldChunkKey(key);
    }
}
