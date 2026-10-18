package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;
import com.exoticworlds.compat.ftbchunks.FtbChunksInjectionTargets;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.UpdateChunkFromServerTask;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(value = UpdateChunkFromServerTask.class, remap = false)
public abstract class UpdateChunkFromServerTaskMixin {
    @Shadow
    @Final
    private MapDimension dimension;

    @WrapOperation(method = "runMapTask",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_REGION_FROM_CHUNK))
    private XZ exotic_worlds$foldRegionKey(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.regionOfChunk(this.dimension.dimension, chunkX, chunkZ);
    }

    @WrapOperation(method = "runMapTask",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_OF))
    private XZ exotic_worlds$foldChunkKey(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.chunkOf(this.dimension.dimension, chunkX, chunkZ);
    }
}
