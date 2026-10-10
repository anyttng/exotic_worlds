package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;
import com.exoticworlds.compat.ftbchunks.FtbChunksInjectionTargets;

import net.minecraft.core.Direction;

import dev.ftb.mods.ftbchunks.client.minimap.MinimapRegionCutoutTexture;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(value = MinimapRegionCutoutTexture.class, remap = false)
public abstract class MinimapRegionCutoutTextureMixin {
    private static final String UPDATE =
            "update(Lnet/minecraft/resources/ResourceKey;Ldev/ftb/mods/ftblibrary/math/XZ;)V";

    private static final String REGION_SPLITS =
            "Ldev/ftb/mods/ftbchunks/client/minimap/MinimapRegionCutoutTexture;regionSplits(I)[I";

    @WrapOperation(method = UPDATE, at = @At(value = "INVOKE", target = REGION_SPLITS, ordinal = 0))
    private int[] toroidal$splitMinimapAlongX(int centreChunk, Operation<int[]> original,
            @Local(argsOnly = true) XZ chunkPos) {
        return FtbChunksFold.minimapSplits(Direction.Axis.X, chunkPos, original.call(centreChunk));
    }

    @WrapOperation(method = UPDATE, at = @At(value = "INVOKE", target = REGION_SPLITS, ordinal = 1))
    private int[] toroidal$splitMinimapAlongZ(int centreChunk, Operation<int[]> original,
            @Local(argsOnly = true) XZ chunkPos) {
        return FtbChunksFold.minimapSplits(Direction.Axis.Z, chunkPos, original.call(centreChunk));
    }

    @WrapOperation(method = UPDATE,
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_REGION_FROM_CHUNK))
    private XZ toroidal$foldMinimapPieceRegion(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.regionOfChunk(chunkX, chunkZ);
    }

    // ox and oz stay raw: a ModifyVariable writes back into its local, and ox outlives the loop over oz.
    @ModifyVariable(method = UPDATE, at = @At("STORE"), name = "srcX")
    private int toroidal$foldMinimapSourceX(int sourceX, @Local(name = "ox") int chunkX, @Local(name = "oz") int chunkZ,
            @Local(name = "chunksPerRegion") int chunksPerRegion) {
        return FtbChunksFold.regionImagePixel(FtbChunksFold.chunkOf(chunkX, chunkZ).x(), chunksPerRegion);
    }

    @ModifyVariable(method = UPDATE, at = @At("STORE"), name = "srcZ")
    private int toroidal$foldMinimapSourceZ(int sourceZ, @Local(name = "ox") int chunkX, @Local(name = "oz") int chunkZ,
            @Local(name = "chunksPerRegion") int chunksPerRegion) {
        return FtbChunksFold.regionImagePixel(FtbChunksFold.chunkOf(chunkX, chunkZ).z(), chunksPerRegion);
    }
}
