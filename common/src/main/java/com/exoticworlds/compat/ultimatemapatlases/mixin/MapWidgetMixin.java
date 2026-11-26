package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.ultimatemapatlases.AtlasWorldView;
import com.exoticworlds.compat.ultimatemapatlases.BrowsedDimension;
import com.exoticworlds.core.fold.WorldFold;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.nanaky.ultimate_map_atlases.client.screen.AtlasOverviewScreen;
import net.nanaky.ultimate_map_atlases.client.screen.MapWidget;

@Mixin(MapWidget.class)
public abstract class MapWidgetMixin implements BrowsedDimension {
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
    public ResourceKey<Level> exotic_worlds$getBrowsedDimension() {
        return this.mapScreen.getSelectedSlice().dimension();
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

    @ModifyReturnValue(method = "getHoveredPos", at = @At("RETURN"))
    private ColumnPos exotic_worlds$foldedHover(ColumnPos hovered) {
        BlockPos raw = new BlockPos(hovered.x(), 0, hovered.z());
        BlockPos folded = ClientFolds.fold(ClientFolds.of(this.exotic_worlds$getBrowsedDimension()), raw);
        return folded.equals(raw) ? hovered : new ColumnPos(folded.getX(), folded.getZ());
    }

    @WrapOperation(
            method = "drawPlayerCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D"))
    private double exotic_worlds$cursorX(Player player, Operation<Double> original) {
        WorldFold worldFold = exotic_worlds$singleCopyFold(player);
        return worldFold != null ? worldFold.fold(player.position()).x : original.call(player);
    }

    @WrapOperation(
            method = "drawPlayerCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D"))
    private double exotic_worlds$cursorZ(Player player, Operation<Double> original) {
        WorldFold worldFold = exotic_worlds$singleCopyFold(player);
        return worldFold != null ? worldFold.fold(player.position()).z : original.call(player);
    }

    @Unique
    private static @Nullable WorldFold exotic_worlds$singleCopyFold(Player player) {
        return MapCopies.getCurrent() == MapCopies.SINGLE ? ClientFolds.of(player.level()) : null;
    }

    @Unique
    private void exotic_worlds$hold(boolean shouldFold) {
        WorldFold worldFold = ClientFolds.of(this.exotic_worlds$getBrowsedDimension());
        if (worldFold == null) {
            return;
        }

        AbstractAtlasWidgetAccessor widget = (AbstractAtlasWidgetAccessor) (Object) this;
        MapCopies copies = MapCopies.getCurrent();
        int mapBlocks = widget.exotic_worlds$getMapSizeBlocks();
        exotic_worlds$capZoom(widget, AtlasWorldView.zoomCeiling(worldFold, copies, this.width, this.height, mapBlocks));
        if (copies == MapCopies.SINGLE) {
            exotic_worlds$holdCenters(widget, worldFold, mapBlocks, shouldFold);
        }
    }

    @Unique
    private void exotic_worlds$capZoom(AbstractAtlasWidgetAccessor widget, float ceiling) {
        if (this.targetZoomLevel > ceiling) {
            this.targetZoomLevel = ceiling;
            this.cumulativeZoomValue = AtlasWorldView.discreteZoomSteps(ceiling);
        }

        if (widget.exotic_worlds$getZoomLevel() > ceiling) {
            widget.exotic_worlds$setZoomLevel(ceiling);
        }
    }

    @Unique
    private void exotic_worlds$holdCenters(AbstractAtlasWidgetAccessor widget, WorldFold worldFold, int mapBlocks,
            boolean shouldFold) {
        Vec3 current = AtlasWorldView.held(worldFold,
                new Vec3(widget.exotic_worlds$getCurrentXCenter(), 0.0, widget.exotic_worlds$getCurrentZCenter()),
                widget.exotic_worlds$getZoomLevel(), this.width, this.height, mapBlocks, shouldFold);
        widget.exotic_worlds$setCurrentXCenter(current.x);
        widget.exotic_worlds$setCurrentZCenter(current.z);
        Vec3 target = AtlasWorldView.held(worldFold, new Vec3(this.targetXCenter, 0.0, this.targetZCenter),
                this.targetZoomLevel, this.width, this.height, mapBlocks, shouldFold);
        this.targetXCenter = target.x;
        this.targetZCenter = target.z;
    }
}
