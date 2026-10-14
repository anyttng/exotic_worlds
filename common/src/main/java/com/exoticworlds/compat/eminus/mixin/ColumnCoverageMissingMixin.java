package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Pseudo
@Mixin(targets = "com.eminus.cell.ColumnCoverage", remap = false)
public class ColumnCoverageMissingMixin {
    @WrapOperation(method = "missingColumns", at = @At(value = "INVOKE", target = EminusInjectionTargets.COVERAGE_PACK))
    private long toroidal$packFoldedMissingChunk(int chunkX, int chunkZ, Operation<Long> original) {
        CellFold fold = CellFoldHolder.foldOf((Object) this);
        return original.call(fold.foldChunkX(chunkX, chunkZ), fold.foldChunkZ(chunkZ));
    }
}
