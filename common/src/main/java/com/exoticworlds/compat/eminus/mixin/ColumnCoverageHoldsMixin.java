package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.cell.ColumnCoverage;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(ColumnCoverage.class)
public class ColumnCoverageHoldsMixin {
    @WrapOperation(method = "holds", at = @At(value = "INVOKE", target = EminusInjectionTargets.COVERAGE_PACK))
    private long exotic_worlds$packFoldedHeldChunk(int chunkX, int chunkZ, Operation<Long> original) {
        CellFold fold = CellFoldHolder.getFold((Object) this);
        return original.call(fold.foldChunkX(chunkX, chunkZ), fold.foldChunkZ(chunkZ));
    }
}
