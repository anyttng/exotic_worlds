package com.exoticworlds.compat.xaero.mixin.map;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroMinimapTiles;
import com.exoticworlds.compat.xaero.XaeroPieceRect;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.MapProcessor;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class SupportXaeroWorldmapMixin {
    @Unique
    private final XaeroMinimapTiles exotic_worlds$tiles = new XaeroMinimapTiles();

    @Inject(method = "renderChunks", at = @At("HEAD"))
    private void exotic_worlds$resolveChunksShape(CallbackInfo ci, @Local(argsOnly = true, name = "minViewX") int minViewX,
            @Local(argsOnly = true, name = "maxViewX") int maxViewX,
            @Local(argsOnly = true, name = "minViewZ") int minViewZ,
            @Local(argsOnly = true, name = "maxViewZ") int maxViewZ) {
        this.exotic_worlds$tiles.begin(minViewX, maxViewX, minViewZ, maxViewZ);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getMinimapMapRegion(II)Lxaero/map/region/MapRegion;"))
    private @Nullable MapRegion exotic_worlds$fetchMinimapRegion(MapProcessor processor, int regX, int regZ,
            Operation<@Nullable MapRegion> original) {
        return this.exotic_worlds$tiles.minimapRegion(regX, regZ, (x, z) -> original.call(processor, x, z));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private @Nullable MapRegion exotic_worlds$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ,
            boolean create, Operation<@Nullable MapRegion> original) {
        return this.exotic_worlds$tiles.leafRegion(caveLayer, regX, regZ,
                (x, z) -> original.call(processor, caveLayer, x, z, create));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/MapRegion;getChunk(II)Lxaero/map/region/MapTileChunk;"))
    private @Nullable MapTileChunk exotic_worlds$fetchCanonicalChunk(MapRegion region, int localX, int localZ,
            Operation<MapTileChunk> original) {
        return this.exotic_worlds$tiles.canonicalChunk(region, localX, localZ, original::call);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/common/mods/SupportXaeroWorldmap;prepareMapTexturedRect(Lorg/joml/Matrix4f;FFIIFF"
                            + "Lxaero/map/region/MapTileChunk;"
                            + "Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;"
                            + "Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;"
                            + "Lxaero/common/minimap/render/MinimapRendererHelper;)V"))
    private void exotic_worlds$drawTilePieces(SupportXaeroWorldmap support, Matrix4f matrix, float x, float y, int textureX,
            int textureY, float width, float height, MapTileChunk chunk, MultiTextureRenderTypeRenderer noLightRenderer,
            MultiTextureRenderTypeRenderer withLightRenderer, MinimapRendererHelper helper, Operation<Void> original) {
        this.exotic_worlds$tiles.drawPieces(support, matrix, new XaeroPieceRect(x, y, textureX, textureY, width, height), chunk,
                noLightRenderer, withLightRenderer, (rect, piece) -> original.call(support, matrix, rect.x(), rect.y(),
                        rect.textureX(), rect.textureY(), rect.width(), rect.height(), piece, noLightRenderer,
                        withLightRenderer, helper));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getX()I"))
    private int exotic_worlds$placeAtMirrorX(MapTileChunk chunk, Operation<Integer> original) {
        return this.exotic_worlds$tiles.placedTileX(original.call(chunk));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getZ()I"))
    private int exotic_worlds$placeAtMirrorZ(MapTileChunk chunk, Operation<Integer> original) {
        return this.exotic_worlds$tiles.placedTileZ(original.call(chunk));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/common/mods/SupportXaeroWorldmap;bumpLoadedRegion(Lxaero/map/MapProcessor;Lxaero/map/region/MapRegion;)V"))
    private void exotic_worlds$bumpFoldedRegion(SupportXaeroWorldmap support, MapProcessor processor, MapRegion region,
            Operation<Void> original) {
        this.exotic_worlds$tiles.bump(region, bumped -> original.call(support, processor, bumped));
    }
}
