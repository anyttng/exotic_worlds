package com.exoticworlds.compat.eminus;

import java.util.List;

import com.eminus.render.tree.TreeNode;

public interface StoredKeyIndex {
    List<TreeNode> toroidal$nodesAt(long storedKey);
}
