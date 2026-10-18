package com.exoticworlds.compat.eminus.mixin;

import java.nio.file.Path;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.cell.ColumnCoverage;
import com.eminus.cell.cache.CellCache;
import com.eminus.ingest.CellMerger;
import com.eminus.session.DimensionRuntime;
import com.eminus.session.WorldIdentity;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusShapes;

@Mixin(DimensionRuntime.class)
public class DimensionRuntimeMixin implements CellFoldHolder {
    @Shadow
    @Final
    private WorldIdentity identity;

    @Shadow
    @Final
    private Path folder;

    @Shadow
    private CellCache cells;

    @Shadow
    private ColumnCoverage coverage;

    @Shadow
    private CellMerger merger;

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

    @Inject(method = "openStore", at = @At("HEAD"))
    private void exotic_worlds$openUnderPublishedShape(CallbackInfo ci) {
        exotic_worlds$fold = EminusShapes.publishedFold(identity.dimension());
        EminusShapes.matchStore(folder, exotic_worlds$fold);
    }

    @Inject(method = "closeStore", at = @At("RETURN"))
    private void exotic_worlds$stampClosedStore(CallbackInfo ci) {
        EminusShapes.stampStore(folder, exotic_worlds$fold);
    }

    @Inject(method = "openCells", at = @At("RETURN"))
    private void exotic_worlds$foldCells(CallbackInfo ci) {
        CellFoldHolder.setFold(cells, exotic_worlds$fold);
        CellFoldHolder.setFold(coverage, exotic_worlds$fold);
        CellFoldHolder.setFold(merger, exotic_worlds$fold);
    }
}
