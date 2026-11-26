package com.exoticworlds.compat.eminus.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.render.tree.CameraFrame;
import com.eminus.render.tree.NodeTable;
import com.eminus.render.tree.TreeNode;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusInjectionTargets;
import com.exoticworlds.compat.eminus.StoredKeyRebuilds;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(targets = "com.eminus.render.tree.TreeChanges")
public class TreeChangesMixin {
    @Shadow
    @Final
    private NodeTable nodes;

    @Unique
    private boolean exotic_worlds$isCoveringCopies;

    @Unique
    private boolean exotic_worlds$isCopyWalkDue;

    @Shadow
    private void requestBuild(TreeNode node, @Nullable CameraFrame camera) {
    }

    @Shadow
    void covered(int chunkX, int chunkZ, @Nullable CameraFrame camera) {
    }

    @WrapOperation(
            method = {"changed", "covered", "requestNeighbors", "requestAcrossEdges"},
            at = @At(value = "INVOKE", target = EminusInjectionTargets.NODE_TABLE_GET))
    private @Nullable TreeNode exotic_worlds$everyNodeOfTheStoredKey(NodeTable table, long key,
            Operation<@Nullable TreeNode> original, @Local(argsOnly = true) @Nullable CameraFrame camera) {
        CellFold fold = CellFoldHolder.getFold(nodes);
        if (!fold.isRepeating()) {
            return original.call(table, key);
        }

        return StoredKeyRebuilds.firstNodeRebuildingTheRest(fold, table, key, node -> requestBuild(node, camera));
    }

    @WrapOperation(method = "changed",
            at = @At(value = "INVOKE", target = "Lcom/eminus/render/tree/TreeNode;changeOccupancy(IJ)Z"))
    private boolean exotic_worlds$changeEveryLapCopy(TreeNode node, int occupancy, long sequence,
            Operation<Boolean> original) {
        boolean isChanged = original.call(node, occupancy, sequence);
        CellFold fold = CellFoldHolder.getFold(nodes);
        if (fold.isRepeating()) {
            exotic_worlds$isCopyWalkDue = StoredKeyRebuilds.isWalkDueAfterChangingLapCopies(fold, nodes, node.key(),
                    copy -> original.call(copy, occupancy, sequence)
                            && ((TreeNodeAccessor) (Object) copy).exotic_worlds$hasGivenWay());
        }

        return isChanged;
    }

    @ModifyReturnValue(method = "changed", at = @At("RETURN"))
    private boolean exotic_worlds$walkForLapCopies(boolean isWalkDue) {
        boolean isDue = isWalkDue || exotic_worlds$isCopyWalkDue;
        exotic_worlds$isCopyWalkDue = false;
        return isDue;
    }

    @Inject(method = "covered", at = @At("HEAD"))
    private void exotic_worlds$coverEveryLapCopy(int chunkX, int chunkZ, @Nullable CameraFrame camera, CallbackInfo ci) {
        if (exotic_worlds$isCoveringCopies) {
            return;
        }

        exotic_worlds$isCoveringCopies = true;
        try {
            StoredKeyRebuilds.coverLapCopies(CellFoldHolder.getFold(nodes), chunkX, chunkZ,
                    (copyX, copyZ) -> covered(copyX, copyZ, camera));
        } finally {
            exotic_worlds$isCoveringCopies = false;
        }
    }
}
