package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;

@Mixin(value = MapDimension.class, remap = false)
public abstract class MapDimensionMixin {
    @Shadow
    @Final
    public ResourceKey<Level> dimension;

    @ModifyVariable(method = "getOwningTeam", at = @At("HEAD"), argsOnly = true)
    private ChunkPos toroidal$foldOwnerKey(ChunkPos pos) {
        return FtbChunksFold.foldedChunkPos(this.dimension, pos);
    }
}
