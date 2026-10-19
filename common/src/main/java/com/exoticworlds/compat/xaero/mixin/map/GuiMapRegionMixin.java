package com.exoticworlds.compat.xaero.mixin.map;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix4f;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.xaero.XaeroCamera;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroRegionFrame;
import com.exoticworlds.compat.xaero.XaeroRegionKey;
import com.exoticworlds.compat.xaero.XaeroRegionLoads;

import xaero.map.MapProcessor;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.region.texture.RegionTexture;

// The injection method is GuiMap's override of Screen.render, and its NAME differs per loader jar: Mojmap "render"
// in the neoforge build, intermediary "method_25394" in the fabric build (the remap pipeline rewrites descriptors in
// the target strings but cannot rename an override it can't resolve to Screen). Both names are listed on every
// injector and defaultRequire=1 accepts whichever the running loader has — the same dual-name pattern as
// EntitySectionManagerMixin's addEntity/addEntityWithoutEvent.
@Mixin(GuiMap.class)
public abstract class GuiMapRegionMixin {
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
    private MapProcessor mapProcessor;
    @Shadow
    private ArrayList<BranchLeveledRegion> branchRegionBuffer;
    @Shadow
    private boolean prevWaitingForBranchCache;
    @Shadow
    private boolean[] waitingForBranchCache;

    @Unique
    private static final String RENDER = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";
    @Unique
    private static final String RENDER_INTERMEDIARY = "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";
    @Unique
    private static final String LEVELED_REGION_GET_TEXTURE =
            "Lxaero/map/region/LeveledRegion;getTexture(II)Lxaero/map/region/texture/RegionTexture;";
    @Unique
    private static final String MAP_PROCESSOR_GET_LEVELED_REGION =
            "Lxaero/map/MapProcessor;getLeveledRegion(IIII)Lxaero/map/region/LeveledRegion;";
    @Unique
    private static final String EXPRESSION = "MIXINEXTRAS:EXPRESSION";

    @Unique
    private final XaeroRegionFrame exotic_worlds$frame = new XaeroRegionFrame();
    @Unique
    private final XaeroRegionLoads exotic_worlds$loads = new XaeroRegionLoads(this.exotic_worlds$frame);

    @Inject(method = {RENDER, RENDER_INTERMEDIARY}, at = @At("HEAD"))
    private void exotic_worlds$beginRegionFrame(CallbackInfo ci) {
        this.exotic_worlds$loads.begin();
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 0))
    private @Nullable LeveledRegion<?> exotic_worlds$fetchHoverLeveledRegion(@Nullable LeveledRegion<?> existing,
            @Local(name = "renderedCaveLayer") int caveLayer, @Local(name = "mouseRegX") int regX,
            @Local(name = "mouseRegZ") int regZ, @Local(name = "textureLevel") int level) {
        return this.exotic_worlds$loads.leveledOrFold(this.mapProcessor, existing,
                new XaeroRegionKey(caveLayer, regX, regZ, level), new XaeroCamera(this.cameraX, this.cameraZ, this.scale));
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = {
                    @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)})
    private @Nullable LeveledRegion<?> exotic_worlds$fetchViewLeveledRegion(@Nullable LeveledRegion<?> existing,
            @Local(name = "renderedCaveLayer") int caveLayer, @Local(name = "leveledRegX") int regX,
            @Local(name = "leveledRegZ") int regZ, @Local(name = "textureLevel") int level) {
        return this.exotic_worlds$loads.leveledOrFold(this.mapProcessor, existing,
                new XaeroRegionKey(caveLayer, regX, regZ, level), new XaeroCamera(this.cameraX, this.cameraZ, this.scale));
    }

    @Inject(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/file/MapSaveLoad;getNextToLoadByViewing()Lxaero/map/region/LeveledRegion;",
                    ordinal = 1))
    private void exotic_worlds$maintainFannedRegions(CallbackInfo ci) {
        this.exotic_worlds$loads.keepFannedRegions(this.mapProcessor, this.prevWaitingForBranchCache,
                this.waitingForBranchCache, this.branchRegionBuffer);
    }

    @Definition(id = "leafZ", local = @Local(type = int.class, name = "leafZ"))
    @Definition(id = "leveledSideInRegions", local = @Local(type = int.class, name = "leveledSideInRegions"))
    @Expression("leafZ < @(leveledSideInRegions)")
    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = EXPRESSION),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private int exotic_worlds$extendLeafColumn(int leveledSideInRegions, @Local(name = "leveledRegX") int leveledRegX, @Local(name = "leveledRegZ") int leveledRegZ,
            @Local(name = "leafRegionMinX") int leafRegionMinX, @Local(name = "leafRegionMinZ") int leafRegionMinZ,
            @Local(name = "leafX") int leafX, @Local(name = "minLeafRegX") int minLeafRegX,
            @Local(name = "maxLeafRegX") int maxLeafRegX) {
        return this.exotic_worlds$loads.extendLeafColumn(leveledSideInRegions, leveledRegX, leveledRegZ, leafRegionMinX,
                leafRegionMinZ, leafX, minLeafRegX, maxLeafRegX);
    }

    @Definition(id = "regZ", local = @Local(type = int.class, name = "regZ"))
    @Definition(id = "maxLeafRegZ", local = @Local(type = int.class, name = "maxLeafRegZ"))
    @Expression("regZ <= @(maxLeafRegZ)")
    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = EXPRESSION),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private int exotic_worlds$admitFannedLeafRow(int maxLeafRegZ, @Local(name = "regX") int regX,
            @Local(name = "regZ") int regZ) {
        return this.exotic_worlds$loads.admitFannedLeafRow(maxLeafRegZ, regX, regZ);
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = "INVOKE", target = "Lxaero/map/MapProcessor;regionExists(III)Z"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private boolean exotic_worlds$fannedLeafExists(boolean exists, @Local(name = "regX") int regX,
            @Local(name = "regZ") int regZ) {
        return this.exotic_worlds$loads.fannedLeafExists(exists, regX, regZ);
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private @Nullable MapRegion exotic_worlds$fetchLoopLeaf(@Nullable MapRegion existing,
            @Local(name = "renderedCaveLayer") int caveLayer, @Local(name = "regX") int regX,
            @Local(name = "regZ") int regZ) {
        return this.exotic_worlds$loads.loopLeaf(this.mapProcessor, existing, caveLayer, regX, regZ);
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION, ordinal = 0))
    private @Nullable MapRegion exotic_worlds$fetchHoverLeaf(@Nullable MapRegion existing,
            @Local(name = "renderedCaveLayer") int caveLayer) {
        return this.exotic_worlds$loads.hoverLeaf(this.mapProcessor, existing, caveLayer, this.mouseBlockPosX,
                this.mouseBlockPosZ);
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION, ordinal = 3))
    private @Nullable MapRegion exotic_worlds$fetchDebugLeaf(@Nullable MapRegion existing,
            @Local(name = "renderedCaveLayer") int caveLayer, @Local(name = "regX") int regX,
            @Local(name = "regZ") int regZ) {
        return this.exotic_worlds$loads.leafOrFold(this.mapProcessor, existing, caveLayer, regX, regZ);
    }

    @WrapOperation(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/misc/Misc;addToListOfSmallest(ILjava/util/List;Ljava/lang/Comparable;)V",
                    ordinal = 0))
    private <T extends Comparable<? super T>> void exotic_worlds$queueLeafOnce(int maxSize, List<T> list, T element,
            Operation<Void> original) {
        XaeroRegionLoads.queueOnce(list, element, () -> original.call(maxSize, list, element));
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = "INVOKE", target = LEVELED_REGION_GET_TEXTURE, ordinal = 0))
    private @Nullable RegionTexture<?> exotic_worlds$foldHoverTexture(@Nullable RegionTexture<?> texture) {
        return this.exotic_worlds$frame.hoverTexture(this.mapProcessor, texture, this.mouseBlockPosX,
                this.mouseBlockPosZ);
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = "INVOKE", target = LEVELED_REGION_GET_TEXTURE, ordinal = 1))
    private @Nullable RegionTexture<?> exotic_worlds$foldLeafTexture(@Nullable RegionTexture<?> texture,
            @Local(name = "leveledRegion") LeveledRegion<?> region, @Local(name = "o") int slotX,
            @Local(name = "p") int slotZ) {
        return this.exotic_worlds$frame.slotTexture(this.mapProcessor, texture, region, slotX, slotZ);
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(value = "INVOKE", target = LEVELED_REGION_GET_TEXTURE, ordinal = 2))
    private @Nullable RegionTexture<?> exotic_worlds$suppressFoldedRootTexture(@Nullable RegionTexture<?> texture) {
        return this.exotic_worlds$frame.rootTexture(texture);
    }

    @ModifyExpressionValue(
            method = {RENDER, RENDER_INTERMEDIARY},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/texture/RegionTexture;getGlColorTexture()I",
                    ordinal = 0),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lxaero/map/gui/GuiMap;renderTexturedModalSubRectWithLighting(Lorg/joml/Matrix4f;"
                                    + "FFFFFFFFIZ"
                                    + "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V")))
    private int exotic_worlds$drawClippedPeriodCopies(int texture,
            @Local(name = "matrix") Matrix4f matrix, @Local(name = "textureX") int textureX,
            @Local(name = "textureZ") int textureZ, @Local(name = "flooredCameraX") int flooredCameraX,
            @Local(name = "flooredCameraZ") int flooredCameraZ,
            @Local(name = "regionTexture") RegionTexture<?> regionTexture,
            @Local(name = "withLightRenderer") MultiTextureRenderTypeRenderer withLightRenderer,
            @Local(name = "noLightRenderer") MultiTextureRenderTypeRenderer noLightRenderer) {
        return this.exotic_worlds$frame.drawPeriodCopies(this.mapProcessor, texture, matrix, textureX, textureZ,
                flooredCameraX, flooredCameraZ, regionTexture, withLightRenderer, noLightRenderer);
    }
}
