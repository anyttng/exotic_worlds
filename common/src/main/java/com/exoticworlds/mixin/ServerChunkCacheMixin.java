package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;

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

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 1)
    private int exotic_worlds$wrapRequestedChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 2)
    private int exotic_worlds$wrapRequestedChunkZ(int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(
            method = {"addTicketWithRadius", "removeTicketWithRadius", "addTicketAndLoadWithRadius"},
            at = @At("HEAD"),
            argsOnly = true)
    private ChunkPos exotic_worlds$foldTicketCenter(ChunkPos pos) {
        return this.exotic_worlds$bound.fold().fold(pos);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 1)
    private int exotic_worlds$wrapChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 2)
    private int exotic_worlds$wrapChunkZ(int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 1)
    private int exotic_worlds$wrapPresenceChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 2)
    private int exotic_worlds$wrapPresenceChunkZ(int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "getChunkForLighting", at = @At("HEAD"), argsOnly = true, index = 1)
    private int exotic_worlds$wrapLightingChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkForLighting", at = @At("HEAD"), argsOnly = true, index = 2)
    private int exotic_worlds$wrapLightingChunkZ(int chunkZ) {
        return this.exotic_worlds$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "onLightUpdate", at = @At("HEAD"), argsOnly = true)
    private SectionPos exotic_worlds$foldLightUpdateSection(SectionPos pos) {
        return this.exotic_worlds$bound.fold().fold(pos);
    }

    @ModifyVariable(method = "blockChanged", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapChangedBlock(BlockPos pos) {
        return this.exotic_worlds$bound.fold().fold(pos);
    }

    @ModifyExpressionValue(
            method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;createState(ILjava/lang/Iterable;Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;Lnet/minecraft/world/level/LocalMobCapCalculator;)Lnet/minecraft/world/level/NaturalSpawner$SpawnState;"))
    private NaturalSpawner.SpawnState exotic_worlds$bindSpawnPotentialToLevel(NaturalSpawner.SpawnState spawnState) {
        ((TransformerHolder) spawnState.spawnPotential).exotic_worlds$setTransformer(this.exotic_worlds$bound.fold());
        return spawnState;
    }
}
