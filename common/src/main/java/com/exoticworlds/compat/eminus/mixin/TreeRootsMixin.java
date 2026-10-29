package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.cell.CellFrame;
import com.eminus.cell.DetailLevel;
import com.eminus.client.session.ClientSession;
import com.eminus.render.tree.TreeRing;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Pseudo
@Mixin(targets = {
        "com.eminus.render.tree.TreeRing",
        "com.eminus.render.tree.TreeRoots"
}, remap = false)
public class TreeRootsMixin implements CellFoldHolder {
    @Unique
    private static final int MARGIN_BLOCKS = 2 * TreeRing.MOVE_BLOCKS;

    @Shadow
    private double anchorX;

    @Shadow
    private double anchorZ;

    @Unique
    private CellFold exotic_worlds$fold = CellFold.NONE;

    @Override
    public CellFold exotic_worlds$fold() {
        return exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(CellFold fold) {
        exotic_worlds$fold = fold;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$foldOfTheRuntime(CallbackInfo ci) {
        exotic_worlds$fold = CellFoldHolder.foldOf(ClientSession.runtime());
    }

    @WrapMethod(method = "inDisc")
    private boolean exotic_worlds$withinOneLap(int cellX, int cellZ, Operation<Boolean> original) {
        if (!original.call(cellX, cellZ)) {
            return false;
        }

        int minX = CellFrame.originBlockX(cellX, DetailLevel.MAX);
        int minZ = CellFrame.originBlockZ(cellZ, DetailLevel.MAX);
        int side = DetailLevel.blocksPerCell(DetailLevel.MAX);
        return exotic_worlds$fold.meetsLap(minX, minZ, minX + side, minZ + side, anchorX, anchorZ, MARGIN_BLOCKS);
    }
}
