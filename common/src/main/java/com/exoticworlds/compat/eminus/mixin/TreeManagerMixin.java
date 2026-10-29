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
import com.exoticworlds.compat.eminus.CellFoldHolder;

@Mixin(TreeManager.class)
public class TreeManagerMixin {
    @Shadow
    @Final
    private NodeTable nodes;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$foldOfTheRuntime(CallbackInfo ci) {
        CellFoldHolder.setFold(nodes, CellFoldHolder.foldOf(ClientSession.runtime()));
    }
}
