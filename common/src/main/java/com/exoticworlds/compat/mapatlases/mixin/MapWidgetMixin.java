package com.exoticworlds.compat.mapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.compat.map.AtlasWorldView;
import com.exoticworlds.compat.map.AtlasWorldWidget;
import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.core.fold.WorldFold;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.phys.Vec3;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.MapWidget;

@Mixin(MapWidget.class)
public abstract class MapWidgetMixin implements AtlasWorldWidget {
    @Shadow
    @Final
    private AtlasOverviewScreen mapScreen;

    @Shadow
    @Final
    private int width;

    @Shadow
    @Final
    private int height;

    @Shadow
    private float cumulativeZoomValue;

    @Shadow
    private double targetXCenter;

    @Shadow
    private double targetZCenter;

    @Shadow
    private float targetZoomLevel;

    @Override
    public int exotic_worlds$getMapBlocks() {
        return this.exotic_worlds$getDisplay().exotic_worlds$getMapSizeBlocks();
    }

    @Override
    public int exotic_worlds$getWidthPixels() {
        return this.width;
    }

    @Override
    public int exotic_worlds$getHeightPixels() {
        return this.height;
    }

    @Override
    public float exotic_worlds$getZoom() {
        return this.exotic_worlds$getDisplay().exotic_worlds$getZoomLevel();
    }

    @Override
    public void exotic_worlds$setZoom(float zoom) {
        this.exotic_worlds$getDisplay().exotic_worlds$setZoomLevel(zoom);
    }

    @Override
    public float exotic_worlds$getTargetZoom() {
        return this.targetZoomLevel;
    }

    @Override
    public void exotic_worlds$setTargetZoom(float zoom) {
        this.targetZoomLevel = zoom;
    }

    @Override
    public void exotic_worlds$setZoomSteps(float steps) {
        this.cumulativeZoomValue = steps;
    }

    @Override
    public Vec3 exotic_worlds$getCenter() {
        AbstractAtlasDisplayAccessor display = this.exotic_worlds$getDisplay();
        return new Vec3(display.exotic_worlds$getCurrentXCenter(), 0.0, display.exotic_worlds$getCurrentZCenter());
    }

    @Override
    public void exotic_worlds$setCenter(Vec3 center) {
        AbstractAtlasDisplayAccessor display = this.exotic_worlds$getDisplay();
        display.exotic_worlds$setCurrentXCenter(center.x);
        display.exotic_worlds$setCurrentZCenter(center.z);
    }

    @Override
    public Vec3 exotic_worlds$getTargetCenter() {
        return new Vec3(this.targetXCenter, 0.0, this.targetZCenter);
    }

    @Override
    public void exotic_worlds$setTargetCenter(Vec3 center) {
        this.targetXCenter = center.x;
        this.targetZCenter = center.z;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void exotic_worlds$holdBeforeFrame(CallbackInfo ci) {
        this.exotic_worlds$hold(true);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void exotic_worlds$holdAfterTick(CallbackInfo ci) {
        this.exotic_worlds$hold(true);
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void exotic_worlds$holdAfterZoom(CallbackInfoReturnable<Boolean> cir) {
        this.exotic_worlds$hold(true);
    }

    @Inject(method = "mouseDragged", at = @At("RETURN"))
    private void exotic_worlds$holdAfterDrag(CallbackInfoReturnable<Boolean> cir) {
        this.exotic_worlds$hold(false);
    }

    @ModifyExpressionValue(
            method = "renderPositionText",
            at = @At(value = "INVOKE", target = "Lpepjebs/mapatlases/client/screen/MapWidget;getHoveredPos(DD)"
                    + "Lnet/minecraft/server/level/ColumnPos;"))
    private ColumnPos exotic_worlds$foldedPositionText(ColumnPos hovered) {
        return this.exotic_worlds$folded(hovered);
    }

    @ModifyExpressionValue(
            method = "mouseClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ColumnPos;x()I"))
    private int exotic_worlds$foldedTeleportX(int x, @Local ColumnPos hovered) {
        return this.exotic_worlds$folded(hovered).x();
    }

    @ModifyExpressionValue(
            method = "mouseClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ColumnPos;z()I"))
    private int exotic_worlds$foldedTeleportZ(int z, @Local ColumnPos hovered) {
        return this.exotic_worlds$folded(hovered).z();
    }

    @Unique
    private void exotic_worlds$hold(boolean shouldFold) {
        AtlasWorldView.hold(this.exotic_worlds$getBrowsedFold(), MapCopies.getCurrent(), this, shouldFold);
    }

    @Unique
    private ColumnPos exotic_worlds$folded(ColumnPos hovered) {
        BlockPos folded = ClientFolds.fold(this.exotic_worlds$getBrowsedFold(), new BlockPos(hovered.x(), 0, hovered.z()));
        return new ColumnPos(folded.getX(), folded.getZ());
    }

    @Unique
    private @Nullable WorldFold exotic_worlds$getBrowsedFold() {
        return ClientFolds.of(this.mapScreen.getSelectedSlice().dimension());
    }

    @Unique
    private AbstractAtlasDisplayAccessor exotic_worlds$getDisplay() {
        return (AbstractAtlasDisplayAccessor) (Object) this;
    }
}
