package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.distanthorizons.DhKeys;
import com.exoticworlds.compat.distanthorizons.DhLattice;
import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree;
import com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;

@Mixin(RenderBufferHandler.class)
public class RenderBufferHandlerMixin {
    @Shadow
    @Final
    public LodQuadTree lodQuadTree;

    @WrapOperation(
            method = "buildRenderList",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/render/QuadTree/LodRenderSection;getRenderingEnabled()Z"))
    private boolean toroidal$drawOnlyTheNearestCopy(LodRenderSection section, Operation<Boolean> original) {
        if (!original.call(section)) {
            return false;
        }

        DhLattice lattice = DhShapes.current();
        if (lattice == null) {
            return true;
        }

        DhBlockPos2D center = this.lodQuadTree.getCenterBlockPos();
        return DhKeys.isNearestCopy(lattice, center.x, center.z, section.pos);
    }
}
