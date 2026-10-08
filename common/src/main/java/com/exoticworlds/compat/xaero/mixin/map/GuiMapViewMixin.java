package com.exoticworlds.compat.xaero.mixin.map;

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
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;
import com.exoticworlds.core.CoordinateConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import xaero.map.graphics.MapRenderHelper;
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

    @Unique
    private static final int SEAM_ARGB = 0xCCFFFFFF;

    @Unique
    private static final float CHANNEL_MAX = 255.0F;

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

    @Shadow
    private static double destScale;

    @Shadow
    private double getScaleMultiplier(int size) {
        throw new AssertionError();
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At("HEAD"))
    private void toroidal$beginFrame(CallbackInfo ci) {
        toroidal$floorZoomOut();
    }

    @Inject(method = "changeZoom(DI)V", at = @At("TAIL"))
    private void toroidal$floorZoomOutOnChange(CallbackInfo ci) {
        toroidal$floorZoomOut();
    }

    @Unique
    private void toroidal$floorZoomOut() {
        Window window = Minecraft.getInstance().getWindow();
        this.toroidal$mapCopies = MapCopies.current();
        double floor = XaeroWorldMapFold.zoomFloorScale(
                this.getScaleMultiplier(Math.min(window.getWidth(), window.getHeight())),
                this.toroidal$mapCopies, window.getWidth(), window.getHeight());
        if (floor > 0.0 && destScale < floor) {
            destScale = floor;
        }
    }

    @WrapOperation(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityX(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraX(Entity entity, float partialTicks, Operation<Double> original) {
        return XaeroWorldMapFold.foldCoord(Direction.Axis.X, original.call(entity, partialTicks));
    }

    @WrapOperation(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityZ(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraZ(Entity entity, float partialTicks, Operation<Double> original) {
        return XaeroWorldMapFold.foldCoord(Direction.Axis.Z, original.call(entity, partialTicks));
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_MAP_SAVE_LOAD,
                    ordinal = 1))
    private void toroidal$stopCameraAtTheEdge(CallbackInfo ci) {
        if (this.toroidal$mapCopies != MapCopies.SINGLE || !XaeroWorldMapFold.active()) {
            return;
        }

        Window window = Minecraft.getInstance().getWindow();
        this.cameraX = XaeroWorldMapFold.copies(Direction.Axis.X).clampView(this.cameraX, window.getWidth() / 2.0 / this.scale);
        this.cameraZ = XaeroWorldMapFold.copies(Direction.Axis.Z).clampView(this.cameraZ, window.getHeight() / 2.0 / this.scale);
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
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
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;setEnd(II)V"))
    private void toroidal$unwrapSelectionEnd(MapTileSelection selection, int endX, int endZ, Operation<Void> original) {
        boolean fresh = selection != this.toroidal$trackedSelection;
        this.toroidal$trackedSelection = selection;
        AxisCopies copiesX = XaeroWorldMapFold.chunkCopies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.chunkCopies(Direction.Axis.Z);
        int startX = selection.getStartX();
        int startZ = selection.getStartZ();
        ChunkPos reference = fresh
                ? new ChunkPos(startX, startZ)
                : new ChunkPos(this.toroidal$selectionEndX, this.toroidal$selectionEndZ);
        ChunkPos unwrapped = XaeroWorldMapFold.nearestChunk(reference, endX, endZ);
        int unwrappedX = unwrapped.x;
        int unwrappedZ = unwrapped.z;
        this.toroidal$selectionEndX = unwrappedX;
        this.toroidal$selectionEndZ = unwrappedZ;
        this.toroidal$selectionLapX = this.toroidal$cursorLapX - (unwrappedX - endX) * CoordinateConstants.CHUNK_WIDTH;
        this.toroidal$selectionLapZ = this.toroidal$cursorLapZ - (unwrappedZ - endZ) * CoordinateConstants.CHUNK_WIDTH;
        original.call(selection, copiesX.withinOneLap(startX, unwrappedX), copiesZ.withinOneLap(startZ, unwrappedZ));
    }

    @WrapOperation(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
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

        int thickness = Math.max(1, (int) Math.ceil(1.0 / this.scale));
        Window window = Minecraft.getInstance().getWindow();
        int[] spanX = XaeroWorldMapFold.viewSpan(this.cameraX, window.getWidth(), this.scale, thickness);
        int[] spanZ = XaeroWorldMapFold.viewSpan(this.cameraZ, window.getHeight(), this.scale, thickness);
        Matrix4f matrix = matrixStack.last().pose();
        for (WorldCopies.Edge seam : XaeroWorldMapFold.seams(spanX, spanZ)) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    seam.fromX() - flooredCameraX, seam.fromZ() - flooredCameraZ,
                    Math.max(seam.toX(), seam.fromX() + thickness) - flooredCameraX,
                    Math.max(seam.toZ(), seam.fromZ() + thickness) - flooredCameraZ,
                    FastColor.ARGB32.red(SEAM_ARGB) / CHANNEL_MAX, FastColor.ARGB32.green(SEAM_ARGB) / CHANNEL_MAX,
                    FastColor.ARGB32.blue(SEAM_ARGB) / CHANNEL_MAX, FastColor.ARGB32.alpha(SEAM_ARGB) / CHANNEL_MAX);
        }
    }

    @WrapOperation(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
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
