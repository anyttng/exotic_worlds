package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.eminus.cell.CellFrame;
import com.eminus.cell.CellKey;
import com.eminus.cell.DetailLevel;
import com.eminus.render.tree.CameraFrame;
import com.eminus.render.tree.NodeTable;
import com.eminus.render.tree.RenderList;
import com.eminus.render.tree.TreeNode;
import com.eminus.render.tree.TreeRing;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.LapTest;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(targets = "com.eminus.render.tree.TreeTraversal")
public class TreeTraversalMixin implements LapTest {
    @Shadow
    @Final
    private NodeTable nodes;

    @Unique
    private double toroidal$eyeX;

    @Unique
    private double toroidal$eyeZ;

    @Inject(method = "walk", at = @At("HEAD"))
    private void toroidal$holdTheEye(CallbackInfo ci, @Local(argsOnly = true) CameraFrame camera) {
        toroidal$hold(camera);
    }

    @Inject(method = "list", at = @At("HEAD"))
    private void toroidal$holdTheListEye(CallbackInfoReturnable<RenderList> cir,
            @Local(argsOnly = true) CameraFrame camera) {
        toroidal$hold(camera);
    }

    @Unique
    private void toroidal$hold(CameraFrame camera) {
        toroidal$eyeX = camera.eyeX();
        toroidal$eyeZ = camera.eyeZ();
    }

    @Inject(method = {"visit", "collectOutOfView"}, at = @At("HEAD"), cancellable = true)
    private void toroidal$refineThroughTheMargin(CallbackInfo ci, @Local(argsOnly = true) TreeNode node) {
        if (!toroidal$meets(node.key(), TreeRing.MOVE_BLOCKS)) {
            ci.cancel();
        }
    }

    @Inject(method = {"listOutOfView", "draw"}, at = @At("HEAD"), cancellable = true)
    private void toroidal$drawOneLap(CallbackInfo ci, @Local(argsOnly = true) TreeNode node) {
        if (!toroidal$meets(node.key(), NO_MARGIN)) {
            ci.cancel();
        }
    }

    @WrapMethod(method = "descended")
    private boolean toroidal$descendedInsideTheLap(long key, Operation<Boolean> original) {
        return original.call(key) && toroidal$meets(key, NO_MARGIN);
    }

    @Override
    public boolean toroidal$meets(long key, double margin) {
        int minX = CellFrame.originXOf(key);
        int minZ = CellFrame.originZOf(key);
        int side = DetailLevel.blocksPerCell(CellKey.level(key));
        return CellFoldHolder.foldOf(nodes).meetsLap(minX, minZ, minX + side, minZ + side, toroidal$eyeX,
                toroidal$eyeZ, margin);
    }
}
