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

import org.joml.Matrix4f;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapViewSpan;
import com.exoticworlds.compat.SeamLine;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import xaero.map.entity.util.EntityUtil;
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
    @Shadow
    private Entity player;
    @Shadow
    private MapTileSelection mapTileSelection;

    @Unique
    private static final float CHANNEL_MAX = 255.0F;

    @Unique
    private static final String MAP_TILE_SELECTION_SET_END = "Lxaero/map/gui/MapTileSelection;setEnd(II)V";

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
    @Unique
    private boolean toroidal$selectionEndPending;
    @Unique
    private ChunkPos toroidal$selectionEnd = ChunkPos.ZERO;

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

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityX(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraX(double x, @Local(name = "partialTicks") float partialTicks) {
        return XaeroWorldMapFold.foldPoint(x, EntityUtil.getEntityZ(this.player, partialTicks)).x;
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityZ(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraZ(double z, @Local(name = "partialTicks") float partialTicks) {
        return XaeroWorldMapFold.foldPoint(EntityUtil.getEntityX(this.player, partialTicks), z).z;
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
        this.cameraX = XaeroWorldMapFold.copies(Direction.Axis.X)
                .clampView(this.cameraX, MapViewSpan.halfBlocks(window.getWidth(), this.scale));
        this.cameraZ = XaeroWorldMapFold.copies(Direction.Axis.Z)
                .clampView(this.cameraZ, MapViewSpan.halfBlocks(window.getHeight(), this.scale));
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "FIELD",
                    target = "Lxaero/map/gui/GuiMap;mouseBlockPosZ:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 1,
                    shift = At.Shift.AFTER))
    private void toroidal$foldCursorBlockPos(CallbackInfo ci) {
        int rawX = this.mouseBlockPosX;
        int rawZ = this.mouseBlockPosZ;
        BlockPos folded = XaeroWorldMapFold.foldBlock(rawX, rawZ);
        this.mouseBlockPosX = folded.getX();
        this.mouseBlockPosZ = folded.getZ();
        this.toroidal$cursorLapX = rawX - this.mouseBlockPosX;
        this.toroidal$cursorLapZ = rawZ - this.mouseBlockPosZ;
    }

    @ModifyArg(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At(value = "INVOKE", target = MAP_TILE_SELECTION_SET_END), index = 0)
    private int toroidal$unwrapSelectionEndX(int endX) {
        return toroidal$unwrappedSelectionEnd().x;
    }

    @ModifyArg(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At(value = "INVOKE", target = MAP_TILE_SELECTION_SET_END), index = 1)
    private int toroidal$unwrapSelectionEndZ(int endZ) {
        return toroidal$unwrappedSelectionEnd().z;
    }

    @Unique
    private ChunkPos toroidal$unwrappedSelectionEnd() {
        if (this.toroidal$selectionEndPending) {
            this.toroidal$selectionEndPending = false;
            return this.toroidal$selectionEnd;
        }

        MapTileSelection selection = this.mapTileSelection;
        int endX = SectionPos.blockToSectionCoord(this.mouseBlockPosX);
        int endZ = SectionPos.blockToSectionCoord(this.mouseBlockPosZ);
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
        this.toroidal$selectionLapX = this.toroidal$cursorLapX - SectionPos.sectionToBlockCoord(unwrappedX - endX);
        this.toroidal$selectionLapZ = this.toroidal$cursorLapZ - SectionPos.sectionToBlockCoord(unwrappedZ - endZ);
        this.toroidal$selectionEnd = new ChunkPos(copiesX.withinOneLap(startX, unwrappedX),
                copiesZ.withinOneLap(startZ, unwrappedZ));
        this.toroidal$selectionEndPending = true;
        return this.toroidal$selectionEnd;
    }

    @ModifyVariable(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At("STORE"), name = "chunkHighlightLeftX")
    private int toroidal$seatHighlightLeft(int leftX) {
        return leftX + this.toroidal$cursorLapX;
    }

    @ModifyVariable(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At("STORE"), name = "chunkHighlightRightX")
    private int toroidal$seatHighlightRight(int rightX) {
        return rightX + this.toroidal$cursorLapX;
    }

    @ModifyVariable(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At("STORE"), name = "chunkHighlightTopZ")
    private int toroidal$seatHighlightTop(int topZ) {
        return topZ + this.toroidal$cursorLapZ;
    }

    @ModifyVariable(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}, at = @At("STORE"), name = "chunkHighlightBottomZ")
    private int toroidal$seatHighlightBottom(int bottomZ) {
        return bottomZ + this.toroidal$cursorLapZ;
    }

    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIIFFFFFFFF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private void toroidal$drawSeamGrid(CallbackInfo ci, @Local(name = "matrixStack") PoseStack matrixStack,
            @Local(name = "overlayBuffer") VertexConsumer overlayBuffer,
            @Local(name = "flooredCameraX") int flooredCameraX, @Local(name = "flooredCameraZ") int flooredCameraZ) {
        if (!this.toroidal$mapCopies.drawsSeamLines() || !XaeroWorldMapFold.active()) {
            return;
        }

        int thickness = Math.max(1, (int) Math.ceil(1.0 / this.scale));
        Window window = Minecraft.getInstance().getWindow();
        int[] spanX = MapViewSpan.of(this.cameraX, window.getWidth(), this.scale, thickness);
        int[] spanZ = MapViewSpan.of(this.cameraZ, window.getHeight(), this.scale, thickness);
        Matrix4f matrix = matrixStack.last().pose();
        for (WorldCopies.Edge seam : XaeroWorldMapFold.seams(spanX, spanZ)) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    seam.fromX() - flooredCameraX, seam.fromZ() - flooredCameraZ,
                    Math.max(seam.toX(), seam.fromX() + thickness) - flooredCameraX,
                    Math.max(seam.toZ(), seam.fromZ() + thickness) - flooredCameraZ,
                    FastColor.ARGB32.red(SeamLine.LINE_ARGB) / CHANNEL_MAX,
                    FastColor.ARGB32.green(SeamLine.LINE_ARGB) / CHANNEL_MAX,
                    FastColor.ARGB32.blue(SeamLine.LINE_ARGB) / CHANNEL_MAX,
                    FastColor.ARGB32.alpha(SeamLine.LINE_ARGB) / CHANNEL_MAX);
        }
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;getLeft()I"))
    private int toroidal$seatSelectionLeft(int left) {
        return left + SectionPos.blockToSectionCoord(this.toroidal$selectionLapX);
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;getRight()I"))
    private int toroidal$seatSelectionRight(int right) {
        return right + SectionPos.blockToSectionCoord(this.toroidal$selectionLapX);
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;getTop()I"))
    private int toroidal$seatSelectionTop(int top) {
        return top + SectionPos.blockToSectionCoord(this.toroidal$selectionLapZ);
    }

    @ModifyExpressionValue(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/gui/MapTileSelection;getBottom()I"))
    private int toroidal$seatSelectionBottom(int bottom) {
        return bottom + SectionPos.blockToSectionCoord(this.toroidal$selectionLapZ);
    }
}
