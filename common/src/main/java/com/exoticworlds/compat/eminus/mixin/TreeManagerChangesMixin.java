package com.exoticworlds.compat.eminus.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.render.tree.NodeTable;
import com.eminus.render.tree.TreeManager;
import com.eminus.render.tree.TreeNode;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusInjectionTargets;
import com.exoticworlds.compat.eminus.StoredKeyRebuilds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(TreeManager.class)
public class TreeManagerChangesMixin {
    @Shadow
    @Final
    private NodeTable nodes;

    @Unique
    private boolean toroidal$coveringCopies;

    @Shadow
    private void requestBuild(TreeNode node) {
    }

    @Shadow
    private void applyCovered(int chunkX, int chunkZ) {
    }

    @WrapOperation(
            method = {"applyChange", "applyCovered", "requestNeighbours", "requestAcrossEdges"},
            at = @At(value = "INVOKE", target = EminusInjectionTargets.NODE_TABLE_GET))
    private @Nullable TreeNode toroidal$everyNodeOfTheStoredKey(NodeTable table, long key,
            Operation<@Nullable TreeNode> original) {
        CellFold fold = CellFoldHolder.foldOf(nodes);
        if (!fold.repeats()) {
            return original.call(table, key);
        }

        return StoredKeyRebuilds.firstNodeRebuildingTheRest(fold, table, key, this::requestBuild);
    }

    @Inject(method = "applyCovered", at = @At("HEAD"))
    private void toroidal$coverEveryLapCopy(int chunkX, int chunkZ, CallbackInfo ci) {
        if (toroidal$coveringCopies) {
            return;
        }

        toroidal$coveringCopies = true;
        try {
            StoredKeyRebuilds.coverLapCopies(CellFoldHolder.foldOf(nodes), chunkX, chunkZ, this::applyCovered);
        } finally {
            toroidal$coveringCopies = false;
        }
    }
}
