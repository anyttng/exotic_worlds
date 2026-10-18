package com.exoticworlds.compat.eminus.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.eminus.render.tree.NodeTable;
import com.eminus.render.tree.TreeNode;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.StoredKeyIndex;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

@Mixin(NodeTable.class)
public class NodeTableMixin implements CellFoldHolder, StoredKeyIndex {
    @Unique
    private final Long2ObjectOpenHashMap<List<TreeNode>> exotic_worlds$byStoredKey = new Long2ObjectOpenHashMap<>();

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

    @Override
    public List<TreeNode> exotic_worlds$nodesAt(long storedKey) {
        return exotic_worlds$byStoredKey.getOrDefault(storedKey, List.of());
    }

    @Inject(method = "root", at = @At("RETURN"))
    private void exotic_worlds$indexRoot(long key, CallbackInfoReturnable<TreeNode> cir) {
        exotic_worlds$index(cir.getReturnValue());
    }

    @Inject(method = "child", at = @At("RETURN"))
    private void exotic_worlds$indexChild(TreeNode parent, int octant, CallbackInfoReturnable<@Nullable TreeNode> cir) {
        TreeNode child = cir.getReturnValue();
        if (child != null) {
            exotic_worlds$index(child);
        }
    }

    @Inject(method = "removeSubtree", at = @At("HEAD"))
    private void exotic_worlds$unindex(TreeNode node, Consumer<TreeNode> removed, CallbackInfo ci) {
        long storedKey = exotic_worlds$fold.foldKey(node.key());
        List<TreeNode> nodes = exotic_worlds$byStoredKey.get(storedKey);
        if (nodes != null && nodes.remove(node) && nodes.isEmpty()) {
            exotic_worlds$byStoredKey.remove(storedKey);
        }
    }

    @Unique
    private void exotic_worlds$index(TreeNode node) {
        List<TreeNode> nodes = exotic_worlds$byStoredKey.computeIfAbsent(exotic_worlds$fold.foldKey(node.key()),
                storedKey -> new ArrayList<>(1));
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }
}
