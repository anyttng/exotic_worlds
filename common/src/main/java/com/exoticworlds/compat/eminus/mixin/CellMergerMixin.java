package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.eminus.ingest.CellMerger;
import com.eminus.ingest.SectionPyramid;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(CellMerger.class)
public class CellMergerMixin implements CellFoldHolder {
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

    @WrapMethod(method = "mergeLevel")
    private boolean exotic_worlds$mergeEveryLapCopy(SectionPyramid pyramid, int level, int blockX, int blockY, int blockZ,
            Operation<Boolean> original) {
        CellFold fold = exotic_worlds$fold;
        boolean changed = original.call(pyramid, level, blockX, blockY, blockZ);
        for (int copy = 1; copy < fold.copies(level); copy++) {
            changed |= original.call(pyramid, level, blockX + fold.copyOffsetX(level, copy), blockY,
                    blockZ + fold.copyOffsetZ(level, copy));
        }

        return changed;
    }
}
