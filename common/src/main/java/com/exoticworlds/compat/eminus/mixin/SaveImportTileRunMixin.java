package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "com.eminus.ingest.saved.SaveImport$TileRun")
public class SaveImportTileRunMixin implements CellFoldHolder {
    @Unique
    private volatile CellFold exotic_worlds$fold = CellFold.NONE;

    @Override
    public CellFold exotic_worlds$getFold() {
        return exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(CellFold fold) {
        exotic_worlds$fold = fold;
    }

    @WrapOperation(
            method = {"missingNeighbors", "accept"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_PACK))
    private long exotic_worlds$packStoredChunk(int chunkX, int chunkZ, Operation<Long> original) {
        CellFold fold = exotic_worlds$fold;
        return original.call(fold.foldChunkX(chunkX, chunkZ), fold.foldChunkZ(chunkZ));
    }

    @WrapOperation(
            method = "pump",
            at = @At(value = "INVOKE", target = "Lcom/eminus/ingest/saved/ImportTiles;regionOfChunk(II)J"))
    private long exotic_worlds$regionOfStoredChunk(int chunkX, int chunkZ, Operation<Long> original) {
        CellFold fold = exotic_worlds$fold;
        return original.call(fold.foldChunkX(chunkX, chunkZ), fold.foldChunkZ(chunkZ));
    }
}
