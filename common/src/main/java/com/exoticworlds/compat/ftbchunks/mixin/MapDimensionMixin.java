package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import net.minecraft.world.level.ChunkPos;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;

@Mixin(value = MapDimension.class, remap = false)
public abstract class MapDimensionMixin {
    @ModifyVariable(method = "getOwningTeam", at = @At("HEAD"), argsOnly = true)
    private ChunkPos toroidal$foldOwnerKey(ChunkPos pos) {
        return FtbChunksFold.foldedChunkPos(pos);
    }
}
