package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.ultimatemapatlases.AtlasWorldView;
import com.exoticworlds.compat.ultimatemapatlases.BrowsedDimension;
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
    public ResourceKey<Level> exotic_worlds$browsedDimension() {
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
        BlockPos folded = ClientShapes.fold(ClientShapes.of(this.exotic_worlds$browsedDimension()), raw);
        return folded.equals(raw) ? hovered : new ColumnPos(folded.getX(), folded.getZ());
    }

    @WrapOperation(
            method = "drawPlayerCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D"))
    private double exotic_worlds$cursorX(Player player, Operation<Double> original) {
        return MapCopies.current() == MapCopies.SINGLE
                ? ClientShapes.fold(ClientShapes.of(player.level()), player.position()).x
                : original.call(player);
    }

    @WrapOperation(
            method = "drawPlayerCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D"))
    private double exotic_worlds$cursorZ(Player player, Operation<Double> original) {
        return MapCopies.current() == MapCopies.SINGLE
                ? ClientShapes.fold(ClientShapes.of(player.level()), player.position()).z
                : original.call(player);
    }

    @Unique
    private void exotic_worlds$hold(boolean fold) {
        ToroidalShape shape = ClientShapes.of(this.exotic_worlds$browsedDimension());
        if (shape == null) {
            return;
        }

        AbstractAtlasWidgetAccessor widget = (AbstractAtlasWidgetAccessor) (Object) this;
        MapCopies copies = MapCopies.current();
        int mapBlocks = widget.exotic_worlds$mapBlocksSize();
        float ceiling = AtlasWorldView.zoomCeiling(shape, copies, this.width, this.height, mapBlocks);
        if (this.targetZoomLevel > ceiling) {
            this.targetZoomLevel = ceiling;
            this.cumulativeZoomValue = AtlasWorldView.discreteZoomSteps(ceiling);
        }

        if (widget.exotic_worlds$zoomLevel() > ceiling) {
            widget.exotic_worlds$setZoomLevel(ceiling);
        }

        if (copies != MapCopies.SINGLE) {
            return;
        }

        Vec3 current = AtlasWorldView.held(shape,
                new Vec3(widget.exotic_worlds$currentXCenter(), 0.0, widget.exotic_worlds$currentZCenter()),
                widget.exotic_worlds$zoomLevel(), this.width, this.height, mapBlocks, fold);
        widget.exotic_worlds$setCurrentXCenter(current.x);
        widget.exotic_worlds$setCurrentZCenter(current.z);
        Vec3 target = AtlasWorldView.held(shape, new Vec3(this.targetXCenter, 0.0, this.targetZCenter),
                this.targetZoomLevel, this.width, this.height, mapBlocks, fold);
        this.targetXCenter = target.x;
        this.targetZCenter = target.z;
    }
}
