package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;
import com.exoticworlds.compat.ftbchunks.FtbChunksInjectionTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(value = FTBChunksClient.class, remap = false)
public abstract class FTBChunksClientMixin {
    @Unique
    private static final String UPDATE_MINIMAP =
            "updateMinimapIfNeeded(IILdev/ftb/mods/ftbchunks/client/map/MapDimension;Z)V";

    @Unique
    private static final String REGION_SPLITS =
            "Ldev/ftb/mods/ftbchunks/client/FTBChunksClient;regionSplits(I)[I";

    @Unique
    private static final String IMAGE_UPLOAD = "Lcom/mojang/blaze3d/platform/NativeImage;upload(IIIIIIIZZZZ)V";

    @Unique
    private static final int SOURCE_X_ARG = 3;

    @Unique
    private static final int SOURCE_Z_ARG = 4;

    @Unique
    private static final int REGION_CHUNK_MASK = 31;

    @Unique
    private static final int CHUNK_PIXELS = 16;

    @Unique
    private static final String MAP_ICON_ENTITY =
            "lambda$mapIcons$14(Lnet/minecraft/world/entity/Entity;"
                    + "Ldev/ftb/mods/ftbchunks/api/client/event/MapIconEvent;"
                    + "Ldev/ftb/mods/ftblibrary/icon/Icon;"
                    + "Ldev/ftb/mods/ftblibrary/icon/EntityIconLoader$WidthHeight;"
                    + "Ldev/ftb/mods/ftbchunks/client/map/MapDimension;)V";

    @WrapOperation(method = UPDATE_MINIMAP, at = @At(value = "INVOKE", target = REGION_SPLITS, ordinal = 0))
    private int[] toroidal$splitMinimapAlongX(int centreChunk, Operation<int[]> original,
            @Local(argsOnly = true, ordinal = 1) int centreChunkZ) {
        return FtbChunksFold.minimapSplits(Direction.Axis.X, XZ.of(centreChunk, centreChunkZ),
                original.call(centreChunk));
    }

    @WrapOperation(method = UPDATE_MINIMAP, at = @At(value = "INVOKE", target = REGION_SPLITS, ordinal = 1))
    private int[] toroidal$splitMinimapAlongZ(int centreChunk, Operation<int[]> original,
            @Local(argsOnly = true, ordinal = 0) int centreChunkX) {
        return FtbChunksFold.minimapSplits(Direction.Axis.Z, XZ.of(centreChunkX, centreChunk),
                original.call(centreChunk));
    }

    @WrapOperation(method = UPDATE_MINIMAP,
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_REGION_FROM_CHUNK))
    private XZ toroidal$regionOfFoldedPiece(int chunkX, int chunkZ, Operation<XZ> original) {
        ChunkPos folded = FtbChunksFold.foldedChunkPos(new ChunkPos(chunkX, chunkZ));
        return original.call(folded.x, folded.z);
    }

    @ModifyArg(method = UPDATE_MINIMAP, index = SOURCE_X_ARG,
            at = @At(value = "INVOKE", target = IMAGE_UPLOAD, remap = true))
    private int toroidal$foldedPieceSourceX(int sourceX, @Local(name = "ox") int chunkX,
            @Local(name = "oz") int chunkZ) {
        return (FtbChunksFold.foldedChunkPos(new ChunkPos(chunkX, chunkZ)).x & REGION_CHUNK_MASK) * CHUNK_PIXELS;
    }

    @ModifyArg(method = UPDATE_MINIMAP, index = SOURCE_Z_ARG,
            at = @At(value = "INVOKE", target = IMAGE_UPLOAD, remap = true))
    private int toroidal$foldedPieceSourceZ(int sourceZ, @Local(name = "ox") int chunkX,
            @Local(name = "oz") int chunkZ) {
        return (FtbChunksFold.foldedChunkPos(new ChunkPos(chunkX, chunkZ)).z & REGION_CHUNK_MASK) * CHUNK_PIXELS;
    }

    @ModifyVariable(method = MAP_ICON_ENTITY, at = @At("STORE"), name = "z")
    private static int toroidal$foldIconBlock(int z, @Local(name = "x") LocalIntRef x) {
        BlockPos folded = FtbChunksFold.foldBlock(x.get(), z);
        x.set(folded.getX());
        return folded.getZ();
    }
}
