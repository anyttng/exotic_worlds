package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(value = MapChunk.class, remap = false)
public abstract class MapChunkMixin {
    @Shadow
    @Final
    private MapRegion region;

    @ModifyExpressionValue(method = "offsetBlocking",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/math/XZ;offset(II)Ldev/ftb/mods/ftblibrary/math/XZ;"))
    private XZ toroidal$foldNeighbour(XZ neighbour) {
        return FtbChunksFold.chunkOf(this.region.dimension.dimension, neighbour);
    }
}
