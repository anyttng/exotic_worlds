package com.exoticworlds.compat.eminus.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
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

@Pseudo
@Mixin(targets = "com.eminus.render.tree.TreeChanges", remap = false)
public class TreeChangesMixin {
    @Shadow
    @Final
    private NodeTable nodes;

    @Unique
    private boolean exotic_worlds$coveringCopies;

    @Unique
    private boolean exotic_worlds$copyWalkDue;

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
        CellFold fold = CellFoldHolder.foldOf(nodes);
        if (!fold.repeats()) {
            return original.call(table, key);
        }

        return StoredKeyRebuilds.firstNodeRebuildingTheRest(fold, table, key, node -> requestBuild(node, camera));
    }

    @WrapOperation(method = "changed",
            at = @At(value = "INVOKE", target = "Lcom/eminus/render/tree/TreeNode;changeOccupancy(IJ)Z"))
    private boolean exotic_worlds$changeEveryLapCopy(TreeNode node, int occupancy, long sequence,
            Operation<Boolean> original) {
        boolean changed = original.call(node, occupancy, sequence);
        CellFold fold = CellFoldHolder.foldOf(nodes);
        if (fold.repeats()) {
            exotic_worlds$copyWalkDue = StoredKeyRebuilds.changeLapCopies(fold, nodes, node.key(),
                    copy -> original.call(copy, occupancy, sequence)
                            && ((TreeNodeAccessor) (Object) copy).exotic_worlds$gaveWay());
        }

        return changed;
    }

    @ModifyReturnValue(method = "changed", at = @At("RETURN"))
    private boolean exotic_worlds$walkForLapCopies(boolean walkDue) {
        boolean due = walkDue || exotic_worlds$copyWalkDue;
        exotic_worlds$copyWalkDue = false;
        return due;
    }

    @Inject(method = "covered", at = @At("HEAD"))
    private void exotic_worlds$coverEveryLapCopy(int chunkX, int chunkZ, @Nullable CameraFrame camera, CallbackInfo ci) {
        if (exotic_worlds$coveringCopies) {
            return;
        }

        exotic_worlds$coveringCopies = true;
        try {
            StoredKeyRebuilds.coverLapCopies(CellFoldHolder.foldOf(nodes), chunkX, chunkZ,
                    (copyX, copyZ) -> covered(copyX, copyZ, camera));
        } finally {
            exotic_worlds$coveringCopies = false;
        }
    }
}
