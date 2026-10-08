package com.exoticworlds.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.journeymap.JourneyMapFold;
import com.exoticworlds.compat.journeymap.JourneyMapInjectionTargets;

import journeymap.client.model.chunk.ChunkMD;
import journeymap.client.model.region.RegionCoord;

@Mixin(targets = "journeymap.client.cartography.render.CaveRenderer", remap = false)
public abstract class CaveRendererMixin {
    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", target = JourneyMapInjectionTargets.REGION_COORD_GET_X_OFFSET))
    private int toroidal$xOffsetOfFoldedChunk(RegionCoord region, int chunkX, Operation<Integer> original,
            @Local(argsOnly = true) ChunkMD chunkMd) {
        return original.call(region, JourneyMapFold.foldRegionChunk(chunkMd.getCoord()).x());
    }

    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", target = JourneyMapInjectionTargets.REGION_COORD_GET_Z_OFFSET))
    private int toroidal$zOffsetOfFoldedChunk(RegionCoord region, int chunkZ, Operation<Integer> original,
            @Local(argsOnly = true) ChunkMD chunkMd) {
        return original.call(region, JourneyMapFold.foldRegionChunk(chunkMd.getCoord()).z());
    }
}
