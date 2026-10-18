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
    private double exotic_worlds$eyeX;

    @Unique
    private double exotic_worlds$eyeZ;

    @Inject(method = "walk", at = @At("HEAD"))
    private void exotic_worlds$holdTheEye(CallbackInfo ci, @Local(argsOnly = true) CameraFrame camera) {
        exotic_worlds$hold(camera);
    }

    @Inject(method = "list", at = @At("HEAD"))
    private void exotic_worlds$holdTheListEye(CallbackInfoReturnable<RenderList> cir,
            @Local(argsOnly = true) CameraFrame camera) {
        exotic_worlds$hold(camera);
    }

    @Unique
    private void exotic_worlds$hold(CameraFrame camera) {
        exotic_worlds$eyeX = camera.eyeX();
        exotic_worlds$eyeZ = camera.eyeZ();
    }

    @Inject(method = {"visit", "collectOutOfView"}, at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$refineThroughTheMargin(CallbackInfo ci, @Local(argsOnly = true) TreeNode node) {
        if (!exotic_worlds$meets(node.key(), TreeRing.MOVE_BLOCKS)) {
            ci.cancel();
        }
    }

    @Inject(method = {"listOutOfView", "draw"}, at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$drawOneLap(CallbackInfo ci, @Local(argsOnly = true) TreeNode node) {
        if (!exotic_worlds$meets(node.key(), NO_MARGIN)) {
            ci.cancel();
        }
    }

    @WrapMethod(method = "descended")
    private boolean exotic_worlds$descendedInsideTheLap(long key, Operation<Boolean> original) {
        return original.call(key) && exotic_worlds$meets(key, NO_MARGIN);
    }

    @Override
    public boolean exotic_worlds$meets(long key, double margin) {
        int minX = CellFrame.originXOf(key);
        int minZ = CellFrame.originZOf(key);
        int side = DetailLevel.blocksPerCell(CellKey.level(key));
        return CellFoldHolder.foldOf(nodes).meetsLap(minX, minZ, minX + side, minZ + side, exotic_worlds$eyeX,
                exotic_worlds$eyeZ, margin);
    }
}
