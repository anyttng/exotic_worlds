package com.exoticworlds.compat.eminus.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.cell.DetailLevel;
import com.eminus.client.session.ClientSession;
import com.eminus.render.tree.NodeTable;
import com.eminus.render.tree.TreeManager;
import com.eminus.render.tree.TreeNode;
import com.eminus.render.tree.TreeRing;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.StoredKeyIndex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.SectionPos;

@Mixin(TreeManager.class)
public class TreeManagerMixin {
    @Shadow
    @Final
    private NodeTable nodes;

    @Shadow
    @Final
    private TreeRing ring;

    @Unique
    private CellFold toroidal$fold = CellFold.NONE;

    @Unique
    private boolean toroidal$coveringCopies;

    @Shadow
    private void requestBuild(TreeNode node) {
    }

    @Shadow
    private void applyCovered(int chunkX, int chunkZ) {
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$foldOfTheRuntime(CallbackInfo ci) {
        toroidal$fold = CellFoldHolder.foldOf(ClientSession.runtime());
        CellFoldHolder.setFold(nodes, toroidal$fold);
        CellFoldHolder.setFold(ring, toroidal$fold);
    }

    @WrapOperation(
            method = {"applyChange", "applyCovered", "requestNeighbours", "requestAcrossEdges"},
            at = @At(value = "INVOKE",
                    target = "Lcom/eminus/render/tree/NodeTable;get(J)Lcom/eminus/render/tree/TreeNode;"))
    private @Nullable TreeNode toroidal$everyNodeOfTheStoredKey(NodeTable table, long key,
            Operation<@Nullable TreeNode> original) {
        if (!toroidal$fold.repeats()) {
            return original.call(table, key);
        }

        List<TreeNode> folded = ((StoredKeyIndex) (Object) table).toroidal$nodesAt(toroidal$fold.foldKey(key));
        for (int index = 1; index < folded.size(); index++) {
            requestBuild(folded.get(index));
        }

        return folded.isEmpty() ? null : folded.getFirst();
    }

    @Inject(method = "applyCovered", at = @At("HEAD"))
    private void toroidal$coverEveryLapCopy(int chunkX, int chunkZ, CallbackInfo ci) {
        if (toroidal$coveringCopies) {
            return;
        }

        toroidal$coveringCopies = true;
        try {
            for (int copy = 1; copy < toroidal$fold.copies(DetailLevel.MAX); copy++) {
                applyCovered(chunkX + SectionPos.blockToSectionCoord(toroidal$fold.copyOffsetX(DetailLevel.MAX, copy)),
                        chunkZ + SectionPos.blockToSectionCoord(toroidal$fold.copyOffsetZ(DetailLevel.MAX, copy)));
            }
        } finally {
            toroidal$coveringCopies = false;
        }
    }
}
