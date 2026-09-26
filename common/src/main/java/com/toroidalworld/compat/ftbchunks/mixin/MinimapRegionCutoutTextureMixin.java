package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import net.minecraft.core.Direction;

import dev.ftb.mods.ftbchunks.client.minimap.MinimapRegionCutoutTexture;

@Mixin(value = MinimapRegionCutoutTexture.class, remap = false)
public abstract class MinimapRegionCutoutTextureMixin {
    private static final String UPDATE =
            "update(Lnet/minecraft/resources/ResourceKey;Ldev/ftb/mods/ftblibrary/math/XZ;)V";

    private static final String REGION_SPLITS =
            "Ldev/ftb/mods/ftbchunks/client/minimap/MinimapRegionCutoutTexture;regionSplits(I)[I";

    @WrapOperation(method = UPDATE, at = @At(value = "INVOKE", target = REGION_SPLITS, ordinal = 0))
    private int[] toroidal$splitMinimapAlongX(int centreChunk, Operation<int[]> original) {
        return FtbChunksFold.minimapSplits(Direction.Axis.X, centreChunk, original.call(centreChunk));
    }

    @WrapOperation(method = UPDATE, at = @At(value = "INVOKE", target = REGION_SPLITS, ordinal = 1))
    private int[] toroidal$splitMinimapAlongZ(int centreChunk, Operation<int[]> original) {
        return FtbChunksFold.minimapSplits(Direction.Axis.Z, centreChunk, original.call(centreChunk));
    }

    @ModifyVariable(method = UPDATE, at = @At("STORE"), name = "ox")
    private int toroidal$foldMinimapPieceX(int chunkX) {
        return FtbChunksFold.foldChunk(Direction.Axis.X, chunkX);
    }

    @ModifyVariable(method = UPDATE, at = @At("STORE"), name = "oz")
    private int toroidal$foldMinimapPieceZ(int chunkZ) {
        return FtbChunksFold.foldChunk(Direction.Axis.Z, chunkZ);
    }
}
