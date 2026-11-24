package com.exoticworlds.compat.eminus.mixin;

import java.util.BitSet;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.eminus.cell.ColumnCoverage;
import com.eminus.ingest.IngestService;
import com.eminus.ingest.SectionPyramid;
import com.eminus.work.WorkService;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusImports;
import com.exoticworlds.compat.eminus.FoldedRegions;
import com.exoticworlds.compat.eminus.SavedChunkOwner;
import com.exoticworlds.compat.eminus.SavedLevelSource;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Pseudo
@Mixin(targets = "com.eminus.ingest.saved.SaveImport", remap = false)
public class SaveImportMixin {
    @Shadow
    @Final
    private ColumnCoverage coverage;

    @Shadow
    private int centerChunkX;

    @Shadow
    private int centerChunkZ;

    @Unique
    private @Nullable ServerLevel exotic_worlds$savedLevel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$readTheSavedLevel(@Coerce Object source, IngestService ingest, ColumnCoverage columns,
            WorkService<SectionPyramid> service, long biomeZoomSeed, CallbackInfo ci) {
        exotic_worlds$savedLevel = source instanceof SavedLevelSource saved ? saved.exotic_worlds$getSavedLevel() : null;
    }

    @Inject(method = "saved", at = @At("HEAD"))
    private void exotic_worlds$ownedByTheSavedLevel(@Coerce Object chunk, CallbackInfo ci) {
        ServerLevel level = exotic_worlds$savedLevel;
        if (level != null) {
            SavedChunkOwner.stamp(chunk, level);
        }
    }

    @Inject(method = "start", at = @At("RETURN"))
    private void exotic_worlds$foldTheTileRun(long tile, BitSet wanted, CallbackInfoReturnable<Object> cir) {
        CellFoldHolder.setFold(cir.getReturnValue(), CellFoldHolder.getFold(coverage));
    }

    @WrapMethod(method = "tick")
    private void exotic_worlds$tickFromTheStoredChunk(int playerChunkX, int playerChunkZ, int radiusChunks,
            int clientViewChunks, Operation<Void> original) {
        CellFold fold = CellFoldHolder.getFold(coverage);
        original.call(fold.foldChunkX(playerChunkX, playerChunkZ), fold.foldChunkZ(playerChunkZ), radiusChunks,
                clientViewChunks);
    }

    @WrapOperation(
            method = {"saved", "whole"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_PACK))
    private long exotic_worlds$packStoredChunk(int chunkX, int chunkZ, Operation<Long> original) {
        CellFold fold = CellFoldHolder.getFold(coverage);
        return original.call(fold.foldChunkX(chunkX, chunkZ), fold.foldChunkZ(chunkZ));
    }

    @WrapMethod(method = "inView")
    private boolean exotic_worlds$inViewAtTheNearestCopy(int chunkX, int chunkZ, Operation<Boolean> original) {
        long copy = CellFoldHolder.getFold(coverage).copyNearChunk(chunkX, chunkZ, centerChunkX, centerChunkZ);
        return original.call(ChunkPos.getX(copy), ChunkPos.getZ(copy));
    }

    @WrapMethod(method = "markDirty(II)V")
    private void exotic_worlds$markTheNearestCopy(int chunkX, int chunkZ, Operation<Void> original) {
        long copy = CellFoldHolder.getFold(coverage).copyNearChunk(chunkX, chunkZ, centerChunkX, centerChunkZ);
        original.call(ChunkPos.getX(copy), ChunkPos.getZ(copy));
    }

    @ModifyExpressionValue(
            method = "wanted",
            at = @At(value = "INVOKE", target = "Lcom/eminus/ingest/saved/SaveImport$Candidate;centerChunkX()I"))
    private int exotic_worlds$centerXNearTheChunk(int viewX, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return ChunkPos.getX(CellFoldHolder.getFold(coverage).copyNearChunk(viewX, centerChunkZ, chunkX, chunkZ));
    }

    @ModifyExpressionValue(
            method = "wanted",
            at = @At(value = "INVOKE", target = "Lcom/eminus/ingest/saved/SaveImport$Candidate;centerChunkZ()I"))
    private int exotic_worlds$centerZNearTheChunk(int viewZ, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return ChunkPos.getZ(CellFoldHolder.getFold(coverage).copyNearChunk(centerChunkX, viewZ, chunkX, chunkZ));
    }

    @ModifyVariable(method = "wanted", at = @At("STORE"), name = "inView")
    private boolean exotic_worlds$anotherCopyCountsAsSeen(boolean isInView, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        long chunk = ChunkPos.pack(chunkX, chunkZ);
        return isInView || CellFoldHolder.getFold(coverage).copyNearChunk(chunkX, chunkZ, centerChunkX, centerChunkZ)
                != chunk;
    }

    @WrapOperation(
            method = "plan",
            at = @At(value = "INVOKE",
                    target = "Lcom/eminus/ingest/saved/ImportTiles;plan(Lit/unimi/dsi/fastutil/longs/LongSet;III)[J"))
    private long[] exotic_worlds$planOverStoredRegions(LongSet regions, int viewX, int viewZ, int radiusChunks,
            Operation<long[]> original) {
        return original.call(FoldedRegions.of(regions, CellFoldHolder.getFold(coverage), 1 << EminusImports.getTileBits()),
                viewX, viewZ, radiusChunks);
    }
}
