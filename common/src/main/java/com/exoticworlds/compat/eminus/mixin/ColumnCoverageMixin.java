package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.cell.ColumnCoverage;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(ColumnCoverage.class)
public class ColumnCoverageMixin implements CellFoldHolder {
    @Unique
    private volatile CellFold exotic_worlds$fold = CellFold.NONE;

    @Override
    public CellFold exotic_worlds$fold() {
        return exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(CellFold fold) {
        exotic_worlds$fold = fold;
    }

    @WrapOperation(
            method = {"cover", "covers", "persist", "fill"},
            at = @At(value = "INVOKE", target = EminusInjectionTargets.COVERAGE_PACK))
    private long exotic_worlds$packFoldedChunk(int chunkX, int chunkZ, Operation<Long> original) {
        CellFold fold = exotic_worlds$fold;
        return original.call(fold.foldChunkX(chunkX, chunkZ), fold.foldChunkZ(chunkZ));
    }
}
