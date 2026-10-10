package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import net.minecraft.core.BlockPos;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;

@Mixin(value = FTBChunksClient.class, remap = false)
public abstract class FTBChunksClientMixin {
    @ModifyVariable(method = "onMapIconEvent", at = @At("STORE"), name = "z")
    private int toroidal$foldIconBlock(int z, @Local(name = "x") LocalIntRef x) {
        BlockPos folded = FtbChunksFold.foldBlock(x.get(), z);
        x.set(folded.getX());
        return folded.getZ();
    }
}
