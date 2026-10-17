package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Unique
    private final BoundLevelFold toroidal$bound = new BoundLevelFold();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$bindLevel(CallbackInfo ci) {
        this.toroidal$bound.bind(this.level);
    }

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapRequestedChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.toroidal$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkFutureMainThread", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapRequestedChunkZ(int chunkZ) {
        return this.toroidal$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(
            method = {"addTicketWithRadius", "removeTicketWithRadius", "addTicketAndLoadWithRadius"},
            at = @At("HEAD"),
            argsOnly = true)
    private ChunkPos toroidal$foldTicketCentre(ChunkPos pos) {
        return this.toroidal$bound.fold().fold(pos);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.toroidal$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkNow", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapChunkZ(int chunkZ) {
        return this.toroidal$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapPresenceChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.toroidal$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "hasChunk", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapPresenceChunkZ(int chunkZ) {
        return this.toroidal$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "getChunkForLighting", at = @At("HEAD"), argsOnly = true, index = 1)
    private int toroidal$wrapLightingChunkX(int chunkX, @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        return this.toroidal$bound.fold().chunkLattice().foldX(chunkX, chunkZ);
    }

    @ModifyVariable(method = "getChunkForLighting", at = @At("HEAD"), argsOnly = true, index = 2)
    private int toroidal$wrapLightingChunkZ(int chunkZ) {
        return this.toroidal$bound.fold().chunkLattice().foldZ(chunkZ);
    }

    @ModifyVariable(method = "onLightUpdate", at = @At("HEAD"), argsOnly = true)
    private SectionPos toroidal$foldLightUpdateSection(SectionPos pos) {
        return this.toroidal$bound.fold().fold(pos);
    }

    @ModifyVariable(method = "blockChanged", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapChangedBlock(BlockPos pos) {
        return this.toroidal$bound.fold().fold(pos);
    }
}
