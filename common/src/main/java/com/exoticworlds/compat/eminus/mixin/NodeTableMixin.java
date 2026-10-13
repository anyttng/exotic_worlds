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
    private final Long2ObjectOpenHashMap<List<TreeNode>> toroidal$byStoredKey = new Long2ObjectOpenHashMap<>();

    @Unique
    private CellFold toroidal$fold = CellFold.NONE;

    @Override
    public CellFold toroidal$fold() {
        return toroidal$fold;
    }

    @Override
    public void toroidal$setFold(CellFold fold) {
        toroidal$fold = fold;
    }

    @Override
    public List<TreeNode> toroidal$nodesAt(long storedKey) {
        return toroidal$byStoredKey.getOrDefault(storedKey, List.of());
    }

    @Inject(method = "root", at = @At("RETURN"))
    private void toroidal$indexRoot(long key, CallbackInfoReturnable<TreeNode> cir) {
        toroidal$index(cir.getReturnValue());
    }

    @Inject(method = "child", at = @At("RETURN"))
    private void toroidal$indexChild(TreeNode parent, int octant, CallbackInfoReturnable<@Nullable TreeNode> cir) {
        TreeNode child = cir.getReturnValue();
        if (child != null) {
            toroidal$index(child);
        }
    }

    @Inject(method = "removeSubtree", at = @At("HEAD"))
    private void toroidal$unindex(TreeNode node, Consumer<TreeNode> removed, CallbackInfo ci) {
        long storedKey = toroidal$fold.foldKey(node.key());
        List<TreeNode> nodes = toroidal$byStoredKey.get(storedKey);
        if (nodes != null && nodes.remove(node) && nodes.isEmpty()) {
            toroidal$byStoredKey.remove(storedKey);
        }
    }

    @Unique
    private void toroidal$index(TreeNode node) {
        List<TreeNode> nodes = toroidal$byStoredKey.computeIfAbsent(toroidal$fold.foldKey(node.key()),
                storedKey -> new ArrayList<>(1));
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }
}
