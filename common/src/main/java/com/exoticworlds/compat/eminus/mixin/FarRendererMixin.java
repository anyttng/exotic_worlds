package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.cell.CellFrame;
import com.eminus.client.render.far.FarRenderer;
import com.eminus.render.far.MeshOrder;
import com.eminus.render.tree.RenderList;
import com.eminus.session.DimensionRuntime;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.DrawnLap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(FarRenderer.class)
public class FarRendererMixin {
    @Shadow
    @Final
    private DimensionRuntime runtime;

    @Unique
    private final DrawnLap toroidal$lap = new DrawnLap();

    @WrapOperation(
            method = "stepFrame",
            at = @At(value = "INVOKE",
                    target = "Lcom/eminus/render/far/MeshOrder;update(Lcom/eminus/render/tree/RenderList;"
                            + "Lcom/eminus/cell/CellFrame;DDD)V"))
    private void toroidal$drawTheLapOfThisFrame(MeshOrder order, RenderList list, CellFrame frame, double eyeX,
            double eyeY, double eyeZ, Operation<Void> original) {
        original.call(order, toroidal$lap.of(CellFoldHolder.foldOf(runtime), list, eyeX, eyeZ), frame, eyeX, eyeY,
                eyeZ);
    }
}
