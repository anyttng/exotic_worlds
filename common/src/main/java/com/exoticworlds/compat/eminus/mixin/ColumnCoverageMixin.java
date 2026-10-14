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
    private volatile CellFold toroidal$fold = CellFold.NONE;

    @Override
    public CellFold toroidal$fold() {
        return toroidal$fold;
    }

    @Override
    public void toroidal$setFold(CellFold fold) {
        toroidal$fold = fold;
    }

    @WrapOperation(
            method = {"cover", "covers", "persist", "fill"},
            at = @At(value = "INVOKE", target = EminusInjectionTargets.COVERAGE_PACK))
    private long toroidal$packFoldedChunk(int chunkX, int chunkZ, Operation<Long> original) {
        CellFold fold = toroidal$fold;
        return original.call(fold.foldChunkX(chunkX, chunkZ), fold.foldChunkZ(chunkZ));
    }
}
