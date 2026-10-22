package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.eminus.render.tree.TreeNode;

@Mixin(TreeNode.class)
public interface TreeNodeAccessor {
    @Invoker("gaveWay")
    boolean exotic_worlds$gaveWay();
}
