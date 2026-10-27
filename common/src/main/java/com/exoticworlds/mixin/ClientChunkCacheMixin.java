package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.client.engine.ClientFrame;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.CurrentRay;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {
    @Shadow
    @Final
    private ClientLevel level;

    @Shadow
    @Final
    private LevelChunk emptyChunk;

    @ModifyReturnValue(
            method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)"
                    + "Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("RETURN"))
    private @Nullable LevelChunk exotic_worlds$heldChunk(@Nullable LevelChunk chunk,
            @Local(argsOnly = true, ordinal = 0) int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ,
            @Local(argsOnly = true) ChunkStatus status, @Local(argsOnly = true) boolean loadOrGenerate) {
        if (chunk != null && chunk != this.emptyChunk) {
            return chunk;
        }

        if (this.level != Minecraft.getInstance().level) {
            return chunk;
        }

        WorldFold fold = ClientFrame.fold();
        if (fold == null || CurrentRay.meetsForeignFrame(fold)) {
            return chunk;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        long copy = ClientFrame.nearestChunkKey(fold, player == null ? null : player.chunkPosition(), chunkX, chunkZ);
        if (copy == ChunkPos.asLong(chunkX, chunkZ)) {
            return chunk;
        }

        return ((ClientChunkCache) (Object) this).getChunk(ChunkPos.getX(copy), ChunkPos.getZ(copy), status,
                loadOrGenerate);
    }
}
