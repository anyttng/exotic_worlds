package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.client.session.ClientSession;
import com.eminus.render.tree.NodeTable;
import com.eminus.render.tree.TreeManager;
import com.eminus.render.tree.TreeRing;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;

@Mixin(TreeManager.class)
public class TreeManagerMixin {
    @Shadow
    @Final
    private NodeTable nodes;

    @Shadow
    @Final
    private TreeRing ring;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$foldOfTheRuntime(CallbackInfo ci) {
        CellFold fold = CellFoldHolder.foldOf(ClientSession.runtime());
        CellFoldHolder.setFold(nodes, fold);
        CellFoldHolder.setFold(ring, fold);
    }
}
