package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix4f;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.MapCopies;
import com.toroidalworld.compat.xaero.XaeroInjectionTargets;
import com.toroidalworld.compat.xaero.XaeroWorldMapFold;
import com.toroidalworld.core.CoordinateConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;

import xaero.map.graphics.MapRenderHelper;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;

@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapViewMixin {
    @Shadow
    private int mouseBlockPosX;
    @Shadow
    private int mouseBlockPosZ;
    @Shadow
    private double scale;
    @Shadow
    private double userScale;
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;
    @Shadow
    private static double destScale;

    @Shadow
    protected abstract double getScaleMultiplier(int screenShortSide);

    @Unique
    private static final int SEAM_ARGB = 0xCCFFFFFF;

    @Unique
    private MapCopies toroidal$mapCopies = MapCopies.REPEATED;
    @Unique
    private int toroidal$cursorLapX;
    @Unique
    private int toroidal$cursorLapZ;
    @Unique
    private int toroidal$selectionLapX;
    @Unique
    private int toroidal$selectionLapZ;
    @Unique
    private MapTileSelection toroidal$trackedSelection;
    @Unique
    private int toroidal$selectionEndX;
    @Unique
    private int toroidal$selectionEndZ;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void toroidal$beginFrame(CallbackInfo ci) {
        this.toroidal$mapCopies = MapCopies.current();
        double floor = toroidal$zoomFloor();
        if (floor > 0.0) {
            if (destScale < floor) {
                destScale = floor;
            }

            if (this.userScale < floor) {
                this.userScale = floor;
            }
        }
    }

    @Inject(method = "applyZoomLimits", at = @At("TAIL"))
    private void toroidal$floorZoomOut(CallbackInfo ci) {
        double floor = toroidal$zoomFloor();
        if (floor > 0.0 && destScale < floor) {
            destScale = floor;
        }
    }

    @Unique
    private double toroidal$zoomFloor() {
        Window window = Minecraft.getInstance().getWindow();
        return XaeroWorldMapFold.zoomFloorScale(this.getScaleMultiplier(Math.min(window.getWidth(), window.getHeight())),
                this.toroidal$mapCopies, window.getWidth(), window.getHeight());
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityX(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraX(Entity entity, float partialTicks, Operation<Double> original) {
        return XaeroWorldMapFold.foldCoord(Direction.Axis.X, original.call(entity, partialTicks));
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityZ(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraZ(Entity entity, float partialTicks, Operation<Double> original) {
        return XaeroWorldMapFold.foldCoord(Direction.Axis.Z, original.call(entity, partialTicks));
    }

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/WorldMapClientOnly;getMapScreenPoseStack()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private void toroidal$stopCameraAtTheEdge(CallbackInfo ci) {
        if (this.toroidal$mapCopies != MapCopies.SINGLE || !XaeroWorldMapFold.active()) {
            return;
        }

        Window window = Minecraft.getInstance().getWindow();
        this.cameraX = XaeroWorldMapFold.copies(Direction.Axis.X).clampView(this.cameraX, window.getWidth() / 2.0 / this.scale);
        this.cameraZ = XaeroWorldMapFold.copies(Direction.Axis.Z).clampView(this.cameraZ, window.getHeight() / 2.0 / this.scale);
    }

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "FIELD",
                    target = "Lxaero/map/gui/GuiMap;mouseBlockPosZ:I",
                    opcode = 181,
                    ordinal = 1,
                    shift = At.Shift.AFTER))
    private void toroidal$foldCursorBlockPos(CallbackInfo ci) {
        int rawX = this.mouseBlockPosX;
        int rawZ = this.mouseBlockPosZ;
        this.mouseBlockPosX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, rawX);
        this.mouseBlockPosZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, rawZ);
        this.toroidal$cursorLapX = rawX - this.mouseBlockPosX;
        this.toroidal$cursorLapZ = rawZ - this.mouseBlockPosZ;
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;setEnd(II)V"))
    private void toroidal$unwrapSelectionEnd(MapTileSelection selection, int endX, int endZ, Operation<Void> original) {
        boolean fresh = selection != this.toroidal$trackedSelection;
        this.toroidal$trackedSelection = selection;
        AxisCopies copiesX = XaeroWorldMapFold.chunkCopies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.chunkCopies(Direction.Axis.Z);
        int startX = selection.getStartX();
        int startZ = selection.getStartZ();
        int unwrappedX = copiesX.nearest(fresh ? startX : this.toroidal$selectionEndX, endX);
        int unwrappedZ = copiesZ.nearest(fresh ? startZ : this.toroidal$selectionEndZ, endZ);
        this.toroidal$selectionEndX = unwrappedX;
        this.toroidal$selectionEndZ = unwrappedZ;
        this.toroidal$selectionLapX = this.toroidal$cursorLapX - (unwrappedX - endX) * CoordinateConstants.CHUNK_WIDTH;
        this.toroidal$selectionLapZ = this.toroidal$cursorLapZ - (unwrappedZ - endZ) * CoordinateConstants.CHUNK_WIDTH;
        original.call(selection, copiesX.withinOneLap(startX, unwrappedX), copiesZ.withinOneLap(startZ, unwrappedZ));
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_RENDER_HELPER_RENDER_DYNAMIC_HIGHLIGHT,
                    ordinal = 0))
    private void toroidal$drawSeamGrid(
            PoseStack matrixStack, VertexConsumer overlayBuffer, int flooredCameraX, int flooredCameraZ,
            int leftX, int rightX, int topZ, int bottomZ,
            float sideR, float sideG, float sideB, float sideA, float centerR, float centerG, float centerB, float centerA,
            Operation<Void> original) {
        if (!XaeroWorldMapFold.active()) {
            original.call(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ, leftX, rightX, topZ, bottomZ,
                    sideR, sideG, sideB, sideA, centerR, centerG, centerB, centerA);
            return;
        }

        int lapX = this.toroidal$cursorLapX;
        int lapZ = this.toroidal$cursorLapZ;
        original.call(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ,
                leftX + lapX, rightX + lapX, topZ + lapZ, bottomZ + lapZ,
                sideR, sideG, sideB, sideA, centerR, centerG, centerB, centerA);
        if (this.toroidal$mapCopies == MapCopies.SINGLE) {
            return;
        }

        AxisCopies copiesX = XaeroWorldMapFold.copies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.copies(Direction.Axis.Z);
        int thickness = Math.max(1, (int) Math.ceil(1.0 / this.scale));
        Window window = Minecraft.getInstance().getWindow();
        int[] spanX = XaeroWorldMapFold.viewSpan(this.cameraX, window.getWidth(), this.scale, thickness);
        int[] spanZ = XaeroWorldMapFold.viewSpan(this.cameraZ, window.getHeight(), this.scale, thickness);
        int[] linesX = copiesX.seams(spanX[0], spanX[1]);
        int[] linesZ = copiesZ.seams(spanZ[0], spanZ[1]);
        Matrix4f matrix = matrixStack.last().pose();
        for (int lineX : linesX) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    lineX - flooredCameraX, spanZ[0] - flooredCameraZ,
                    lineX - flooredCameraX + thickness, spanZ[1] - flooredCameraZ,
                    ARGB.redFloat(SEAM_ARGB), ARGB.greenFloat(SEAM_ARGB), ARGB.blueFloat(SEAM_ARGB), ARGB.alphaFloat(SEAM_ARGB));
        }

        for (int lineZ : linesZ) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    spanX[0] - flooredCameraX, lineZ - flooredCameraZ,
                    spanX[1] - flooredCameraX, lineZ - flooredCameraZ + thickness,
                    ARGB.redFloat(SEAM_ARGB), ARGB.greenFloat(SEAM_ARGB), ARGB.blueFloat(SEAM_ARGB), ARGB.alphaFloat(SEAM_ARGB));
        }
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_RENDER_HELPER_RENDER_DYNAMIC_HIGHLIGHT,
                    ordinal = 1))
    private void toroidal$drawSelectionInCursorCopy(
            PoseStack matrixStack, VertexConsumer overlayBuffer, int flooredCameraX, int flooredCameraZ,
            int leftX, int rightX, int topZ, int bottomZ,
            float sideR, float sideG, float sideB, float sideA, float centerR, float centerG, float centerB, float centerA,
            Operation<Void> original) {
        int lapX = this.toroidal$selectionLapX;
        int lapZ = this.toroidal$selectionLapZ;
        original.call(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ,
                leftX + lapX, rightX + lapX, topZ + lapZ, bottomZ + lapZ,
                sideR, sideG, sideB, sideA, centerR, centerG, centerB, centerA);
    }
}
