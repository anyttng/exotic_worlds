package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import net.minecraft.core.Direction;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;

@Mixin(value = FTBChunksClient.class, remap = false)
public abstract class FTBChunksClientMixin {
    @ModifyVariable(method = "onMapIconEvent", at = @At("STORE"), name = "x")
    private int toroidal$foldIconBlockX(int x) {
        return FtbChunksFold.foldBlock(Direction.Axis.X, x);
    }

    @ModifyVariable(method = "onMapIconEvent", at = @At("STORE"), name = "z")
    private int toroidal$foldIconBlockZ(int z) {
        return FtbChunksFold.foldBlock(Direction.Axis.Z, z);
    }
}
