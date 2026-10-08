package com.exoticworlds.compat.journeymap.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.exoticworlds.compat.journeymap.JourneyMapFold;

import journeymap.client.model.map.MapType;
import journeymap.client.model.region.RegionCoord;
import net.minecraft.world.level.ChunkPos;

@Mixin(targets = "journeymap.client.model.region.RegionCoord", remap = false)
public class RegionCoordMixin {
    @WrapMethod(method = "fromChunkPos")
    private static RegionCoord toroidal$foldChunk(File worldDir, MapType mapType, int chunkX, int chunkZ,
            Operation<RegionCoord> original) {
        ChunkPos folded = JourneyMapFold.foldRegionChunk(new ChunkPos(chunkX, chunkZ));
        return original.call(worldDir, mapType, folded.x, folded.z);
    }
}
