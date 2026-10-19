package com.exoticworlds.compat.xaero.mixin.map;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.exoticworlds.compat.xaero.XaeroCamera;
import com.exoticworlds.compat.xaero.XaeroCursorLap;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroMapView;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import xaero.map.entity.util.EntityUtil;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;

// The injection method is GuiMap's override of Screen.render, and its NAME differs per loader jar: Mojmap "render"
// in the neoforge build, intermediary "method_25394" in the fabric build (the remap pipeline rewrites descriptors in
// the target strings but cannot rename an override it can't resolve to Screen). Both names are listed on every
// injector and defaultRequire=1 accepts whichever the running loader has — the same dual-name pattern as
// EntitySectionManagerMixin's addEntity/addEntityWithoutEvent.
@Mixin(GuiMap.class)
public abstract class GuiMapViewMixin {
    @Shadow
    private int mouseBlockPosX;
    @Shadow
    private int mouseBlockPosZ;
    @Shadow
    private double scale;
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;
    @Shadow
    private Entity player;
    @Shadow
    private MapTileSelection mapTileSelection;

    @Unique
    private static final String MAP_TILE_SELECTION_SET_END = "Lxaero/map/gui/MapTileSelection;setEnd(II)V";

    @Unique
    private final XaeroMapView exotic_worlds$view = new XaeroMapView();
    @Unique
    private final XaeroCursorLap exotic_worlds$cursor = new XaeroCursorLap();

    @Shadow
    private static double destScale;

    @Shadow
    private double getScaleMultiplier(int size) {
        throw new AssertionError();
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At("HEAD"))
    private void exotic_worlds$beginFrame(CallbackInfo ci) {
        destScale = XaeroMapView.raised(destScale,
                this.exotic_worlds$view.beginFrame(this.getScaleMultiplier(XaeroMapView.screenShortSide())));
    }

    @Inject(method = "changeZoom(DI)V", at = @At("TAIL"))
    private void exotic_worlds$floorZoomOutOnChange(CallbackInfo ci) {
        destScale = XaeroMapView.raised(destScale,
                this.exotic_worlds$view.beginFrame(this.getScaleMultiplier(XaeroMapView.screenShortSide())));
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityX(Lnet/minecraft/world/entity/Entity;F)D"))
    private double exotic_worlds$foldCameraX(double x, @Local(name = "partialTicks") float partialTicks) {
        return XaeroWorldMapFold.foldPoint(x, EntityUtil.getEntityZ(this.player, partialTicks)).x;
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityZ(Lnet/minecraft/world/entity/Entity;F)D"))
    private double exotic_worlds$foldCameraZ(double z, @Local(name = "partialTicks") float partialTicks) {
        return XaeroWorldMapFold.foldPoint(EntityUtil.getEntityX(this.player, partialTicks), z).z;
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_MAP_SAVE_LOAD,
                    ordinal = 1))
    private void exotic_worlds$stopCameraAtTheEdge(CallbackInfo ci) {
        this.cameraX = this.exotic_worlds$view.clampCameraX(this.cameraX, this.scale);
        this.cameraZ = this.exotic_worlds$view.clampCameraZ(this.cameraZ, this.scale);
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "FIELD",
                    target = "Lxaero/map/gui/GuiMap;mouseBlockPosZ:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 1,
                    shift = At.Shift.AFTER))
    private void exotic_worlds$foldCursorBlockPos(CallbackInfo ci) {
        BlockPos folded = this.exotic_worlds$cursor.foldCursor(this.mouseBlockPosX, this.mouseBlockPosZ);
        this.mouseBlockPosX = folded.getX();
        this.mouseBlockPosZ = folded.getZ();
    }

    @ModifyArg(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At(value = "INVOKE", target = MAP_TILE_SELECTION_SET_END), index = 0)
    private int exotic_worlds$unwrapSelectionEndX(int endX) {
        return this.exotic_worlds$cursor.selectionEnd(this.mapTileSelection, this.mouseBlockPosX, this.mouseBlockPosZ).x;
    }

    @ModifyArg(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At(value = "INVOKE", target = MAP_TILE_SELECTION_SET_END), index = 1)
    private int exotic_worlds$unwrapSelectionEndZ(int endZ) {
        return this.exotic_worlds$cursor.selectionEnd(this.mapTileSelection, this.mouseBlockPosX, this.mouseBlockPosZ).z;
    }

    @ModifyVariable(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At("STORE"), name = "chunkHighlightLeftX")
    private int exotic_worlds$seatHighlightLeft(int leftX) {
        return this.exotic_worlds$cursor.seatX(leftX);
    }

    @ModifyVariable(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At("STORE"), name = "chunkHighlightRightX")
    private int exotic_worlds$seatHighlightRight(int rightX) {
        return this.exotic_worlds$cursor.seatX(rightX);
    }

    @ModifyVariable(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At("STORE"), name = "chunkHighlightTopZ")
    private int exotic_worlds$seatHighlightTop(int topZ) {
        return this.exotic_worlds$cursor.seatZ(topZ);
    }

    @ModifyVariable(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At("STORE"), name = "chunkHighlightBottomZ")
    private int exotic_worlds$seatHighlightBottom(int bottomZ) {
        return this.exotic_worlds$cursor.seatZ(bottomZ);
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIIFFFFFFFF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private void exotic_worlds$drawSeamGrid(CallbackInfo ci, @Local(name = "matrixStack") PoseStack matrixStack,
            @Local(name = "overlayBuffer") VertexConsumer overlayBuffer,
            @Local(name = "flooredCameraX") int flooredCameraX, @Local(name = "flooredCameraZ") int flooredCameraZ) {
        this.exotic_worlds$view.drawSeamGrid(matrixStack.last().pose(), overlayBuffer, flooredCameraX, flooredCameraZ,
                new XaeroCamera(this.cameraX, this.cameraZ, this.scale));
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;getLeft()I"))
    private int exotic_worlds$seatSelectionLeft(int left) {
        return this.exotic_worlds$cursor.seatSelectionX(left);
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;getRight()I"))
    private int exotic_worlds$seatSelectionRight(int right) {
        return this.exotic_worlds$cursor.seatSelectionX(right);
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;getTop()I"))
    private int exotic_worlds$seatSelectionTop(int top) {
        return this.exotic_worlds$cursor.seatSelectionZ(top);
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;getBottom()I"))
    private int exotic_worlds$seatSelectionBottom(int bottom) {
        return this.exotic_worlds$cursor.seatSelectionZ(bottom);
    }
}
