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
    private volatile CellFold toroidal$fold = CellFold.NONE;

    @Override
    public CellFold toroidal$fold() {
        return toroidal$fold;
    }

    @Override
    public void toroidal$setFold(CellFold fold) {
        toroidal$fold = fold;
    }

    @Inject(method = "openStore", at = @At("HEAD"))
    private void toroidal$openUnderPublishedShape(CallbackInfo ci) {
        toroidal$fold = EminusShapes.publishedFold(identity.dimension());
        EminusShapes.matchStore(folder, toroidal$fold);
    }

    @Inject(method = "openCells", at = @At("RETURN"))
    private void toroidal$foldCells(CallbackInfo ci) {
        CellFoldHolder.setFold(cells, toroidal$fold);
        CellFoldHolder.setFold(coverage, toroidal$fold);
        CellFoldHolder.setFold(merger, toroidal$fold);
    }
}
