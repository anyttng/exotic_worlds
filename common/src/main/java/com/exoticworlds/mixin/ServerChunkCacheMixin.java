package com.exoticworlds.mixin;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$bindLevel(CallbackInfo ci) {
        this.exotic_worlds$bound.bind(this.level);
    }

    @Unique
    private long exotic_worlds$physicalKey(int chunkX, int chunkZ) {
        return this.exotic_worlds$bound.getFold().foldChunkKey(ChunkPos.pack(chunkX, chunkZ));
    }

    @WrapMethod(method = "getChunkFutureMainThread")
    private CompletableFuture<ChunkResult<ChunkAccess>> exotic_worlds$requestPhysicalChunk(int chunkX, int chunkZ,
            ChunkStatus targetStatus, boolean loadOrGenerate,
            Operation<CompletableFuture<ChunkResult<ChunkAccess>>> original) {
        long folded = exotic_worlds$physicalKey(chunkX, chunkZ);
        return original.call(ChunkPos.getX(folded), ChunkPos.getZ(folded), targetStatus, loadOrGenerate);
    }

    @ModifyVariable(
            method = {"addTicketWithRadius", "removeTicketWithRadius", "addTicketAndLoadWithRadius"},
            at = @At("HEAD"),
            argsOnly = true)
    private ChunkPos exotic_worlds$foldTicketCenter(ChunkPos pos) {
        return this.exotic_worlds$bound.getFold().fold(pos);
    }

    @WrapMethod(method = "getChunkNow")
    private @Nullable LevelChunk exotic_worlds$physicalChunkNow(int chunkX, int chunkZ,
            Operation<@Nullable LevelChunk> original) {
        long folded = exotic_worlds$physicalKey(chunkX, chunkZ);
        return original.call(ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    @WrapMethod(method = "hasChunk")
    private boolean exotic_worlds$hasPhysicalChunk(int chunkX, int chunkZ, Operation<Boolean> original) {
        long folded = exotic_worlds$physicalKey(chunkX, chunkZ);
        return original.call(ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    @WrapMethod(method = "getChunkForLighting")
    private @Nullable LightChunk exotic_worlds$physicalChunkForLighting(int chunkX, int chunkZ,
            Operation<@Nullable LightChunk> original) {
        long folded = exotic_worlds$physicalKey(chunkX, chunkZ);
        return original.call(ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    @ModifyVariable(method = "onLightUpdate", at = @At("HEAD"), argsOnly = true)
    private SectionPos exotic_worlds$foldLightUpdateSection(SectionPos pos) {
        return this.exotic_worlds$bound.getFold().fold(pos);
    }

    @ModifyVariable(method = "blockChanged", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapChangedBlock(BlockPos pos) {
        return this.exotic_worlds$bound.getFold().fold(pos);
    }
}
