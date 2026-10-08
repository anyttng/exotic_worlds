package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
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
    private WorldFold toroidal$transformer;

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapRequestedChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return toroidal$transformer().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapRequestedChunkZ(int chunkZ) {
        return toroidal$transformer().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(
            method = {"addTicketWithRadius", "removeTicketWithRadius", "addTicketAndLoadWithRadius"},
            at = @At("HEAD"),
            argsOnly = true)
    private ChunkPos toroidal$foldTicketCentre(ChunkPos pos) {
        return toroidal$transformer().fold(pos);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return toroidal$transformer().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapChunkZ(int chunkZ) {
        return toroidal$transformer().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapPresenceChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return toroidal$transformer().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapPresenceChunkZ(int chunkZ) {
        return toroidal$transformer().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "getChunkForLighting", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapLightingChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return toroidal$transformer().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkForLighting", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapLightingChunkZ(int chunkZ) {
        return toroidal$transformer().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "onLightUpdate", at = @At("HEAD"), argsOnly = true)
    private SectionPos toroidal$foldLightUpdateSection(SectionPos pos) {
        return toroidal$transformer().fold(pos);
    }

    @ModifyVariable(method = "blockChanged", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapChangedBlock(BlockPos pos) {
        return toroidal$transformer().fold(pos);
    }

    @ModifyExpressionValue(
            method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;createState(ILjava/lang/Iterable;Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;Lnet/minecraft/world/level/LocalMobCapCalculator;)Lnet/minecraft/world/level/NaturalSpawner$SpawnState;"))
    private NaturalSpawner.SpawnState toroidal$bindSpawnPotentialToLevel(NaturalSpawner.SpawnState spawnState) {
        ((TransformerHolder) spawnState.spawnPotential).toroidal$setTransformer(toroidal$transformer());
        return spawnState;
    }

    @Unique
    private WorldFold toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = WorldLoopAttachments.transformerOf(this.level);
        }

        return this.toroidal$transformer;
    }
}
