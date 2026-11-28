package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.map.AtlasPointSeat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.phys.Vec3;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;

@Mixin(targets = "pepjebs.mapatlases.client.screen.DecorationListPanel")
public abstract class DecorationListPanelMixin {
    @Shadow
    private int lastViewCx;

    @Shadow
    private int lastViewCz;

    @WrapOperation(
            method = "isInView",
            at = @At(value = "INVOKE",
                    target = "Lpepjebs/mapatlases/client/screen/DecorationBookmarkButton;getWorldX()D"))
    private double exotic_worlds$bookmarkXNearView(DecorationBookmarkButton bookmark, Operation<Double> original) {
        return this.exotic_worlds$seatedNearView(bookmark, original.call(bookmark), bookmark.getWorldZ()).x;
    }

    @WrapOperation(
            method = "isInView",
            at = @At(value = "INVOKE",
                    target = "Lpepjebs/mapatlases/client/screen/DecorationBookmarkButton;getWorldZ()D"))
    private double exotic_worlds$bookmarkZNearView(DecorationBookmarkButton bookmark, Operation<Double> original) {
        return this.exotic_worlds$seatedNearView(bookmark, bookmark.getWorldX(), original.call(bookmark)).z;
    }

    @Unique
    private Vec3 exotic_worlds$seatedNearView(DecorationBookmarkButton bookmark, double x, double z) {
        return AtlasPointSeat.toward(
                ((DecorationBookmarkButtonAccessor) bookmark).exotic_worlds$getHolder().data().data.dimension,
                new Vec3(this.lastViewCx, 0.0, this.lastViewCz), x, z);
    }
}
