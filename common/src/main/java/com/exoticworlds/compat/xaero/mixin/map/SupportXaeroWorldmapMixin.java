package com.exoticworlds.compat.xaero.mixin.map;

import java.util.List;

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
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TileArea;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;
import com.exoticworlds.core.CoordinateConstants;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.hud.render.util.MultiTextureRenderUtil;
import xaero.lib.client.graphics.GpuTextureAndView;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class SupportXaeroWorldmapMixin {
    @Unique
    private int exotic_worlds$fetchRegionX;
    @Unique
    private int exotic_worlds$fetchRegionZ;
    @Unique
    private boolean exotic_worlds$fetchIsLeaf;
    @Unique
    private int exotic_worlds$fetchLeafLayer;
    @Unique
    private int exotic_worlds$mirrorTileX;
    @Unique
    private int exotic_worlds$mirrorTileZ;
    @Unique
    private MapRegion exotic_worlds$foldedRegion;
    @Unique
    private MapProcessor exotic_worlds$processor;
    @Unique
    private @Nullable List<TileArea> exotic_worlds$areas;
    @Unique
    private @Nullable ToroidalShape exotic_worlds$shape;
    @Unique
    private AxisCopies exotic_worlds$copiesX = AxisCopies.UNBOUNDED;
    @Unique
    private AxisCopies exotic_worlds$copiesZ = AxisCopies.UNBOUNDED;
    @Unique
    private WorldCopies.@Nullable Window exotic_worlds$window;

    @Inject(method = "renderChunks", at = @At("HEAD"))
    private void exotic_worlds$resolveChunksShape(CallbackInfo ci, @Local(argsOnly = true, name = "minViewX") int minViewX,
            @Local(argsOnly = true, name = "maxViewX") int maxViewX,
            @Local(argsOnly = true, name = "minViewZ") int minViewZ,
            @Local(argsOnly = true, name = "maxViewZ") int maxViewZ) {
        ToroidalShape shape = XaeroWorldMapFold.browsedShape();
        this.exotic_worlds$shape = shape;
        this.exotic_worlds$copiesX = AxisCopies.of(shape, Direction.Axis.X);
        this.exotic_worlds$copiesZ = AxisCopies.of(shape, Direction.Axis.Z);
        this.exotic_worlds$window = shape == null ? null : WorldCopies.Window.of(shape,
                XaeroWorldMapFold.tileBlock(minViewX), XaeroWorldMapFold.tileBlock(minViewZ),
                XaeroWorldMapFold.tileBlock(maxViewX + 1), XaeroWorldMapFold.tileBlock(maxViewZ + 1));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getMinimapMapRegion(II)Lxaero/map/region/MapRegion;"))
    private @Nullable MapRegion exotic_worlds$fetchMinimapRegion(MapProcessor processor, int regX, int regZ,
            Operation<@Nullable MapRegion> original) {
        this.exotic_worlds$fetchRegionX = regX;
        this.exotic_worlds$fetchRegionZ = regZ;
        this.exotic_worlds$fetchIsLeaf = false;
        MapRegion existing = original.call(processor, regX, regZ);
        if (existing != null || this.exotic_worlds$shape == null) {
            return existing;
        }

        // A candidate value only, so the null-guarded chunk fetch runs at all; the chunk redirect re-fetches precisely.
        ChunkPos folded = XaeroWorldMapFold.foldRegion(this.exotic_worlds$shape, regX, regZ);
        return original.call(processor, folded.x(), folded.z());
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private @Nullable MapRegion exotic_worlds$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ,
            boolean create, Operation<@Nullable MapRegion> original) {
        this.exotic_worlds$fetchRegionX = regX;
        this.exotic_worlds$fetchRegionZ = regZ;
        this.exotic_worlds$fetchIsLeaf = true;
        this.exotic_worlds$fetchLeafLayer = caveLayer;
        MapRegion existing = original.call(processor, caveLayer, regX, regZ, create);
        if (existing != null || this.exotic_worlds$shape == null) {
            return existing;
        }

        ChunkPos folded = XaeroWorldMapFold.foldRegion(this.exotic_worlds$shape, regX, regZ);
        return original.call(processor, caveLayer, folded.x(), folded.z(), create);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/MapRegion;getChunk(II)Lxaero/map/region/MapTileChunk;"))
    private @Nullable MapTileChunk exotic_worlds$fetchCanonicalChunk(MapRegion region, int localX, int localZ,
            Operation<MapTileChunk> original) {
        int mirrorTileX = XaeroWorldMapFold.firstTileChunkOfRegion(this.exotic_worlds$fetchRegionX) + localX;
        int mirrorTileZ = XaeroWorldMapFold.firstTileChunkOfRegion(this.exotic_worlds$fetchRegionZ) + localZ;
        this.exotic_worlds$mirrorTileX = mirrorTileX;
        this.exotic_worlds$mirrorTileZ = mirrorTileZ;
        this.exotic_worlds$foldedRegion = null;
        this.exotic_worlds$areas = null;
        WorldCopies.Window window = this.exotic_worlds$window;
        if (window == null
                || !XaeroWorldMapFold.tileLeavesWorld(this.exotic_worlds$copiesX, this.exotic_worlds$copiesZ, mirrorTileX, mirrorTileZ)) {
            return region == null ? null : original.call(region, localX, localZ);
        }

        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            return region == null ? null : original.call(region, localX, localZ);
        }

        MapProcessor processor = session.getMapProcessor();
        this.exotic_worlds$processor = processor;
        List<TileArea> areas = XaeroWorldMapFold.tilePieces(window, mirrorTileX, mirrorTileZ);
        MapTileChunk shown = null;
        for (TileArea area : areas) {
            int canonicalTileX = area.x().canonicalTile();
            int canonicalTileZ = area.z().canonicalTile();
            MapRegion canonicalRegion = exotic_worlds$canonicalRegion(canonicalTileX, canonicalTileZ);
            if (canonicalRegion == null) {
                continue;
            }

            if (canonicalRegion != region) {
                processor.beforeMinimapRegionRender(canonicalRegion);
            }

            MapTileChunk chunk = original.call(canonicalRegion,
                    XaeroWorldMapFold.tileChunkInRegion(canonicalTileX), XaeroWorldMapFold.tileChunkInRegion(canonicalTileZ));
            if (chunk != null && (shown == null || exotic_worlds$textureOf(shown) == null && exotic_worlds$textureOf(chunk) != null)) {
                shown = chunk;
                this.exotic_worlds$foldedRegion = canonicalRegion;
            }
        }

        if (areas.size() > 1) {
            this.exotic_worlds$areas = areas;
        }

        return shown;
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
        List<TileArea> areas = this.exotic_worlds$areas;
        if (areas == null) {
            original.call(support, matrix, x, y, textureX, textureY, width, height, chunk, noLightRenderer,
                    withLightRenderer, helper);
            return;
        }

        for (TileArea area : areas) {
            TilePiece pieceX = area.x();
            TilePiece pieceZ = area.z();
            MapRegion canonicalRegion = exotic_worlds$canonicalRegion(pieceX.canonicalTile(), pieceZ.canonicalTile());
            MapTileChunk piece = canonicalRegion == null ? null : canonicalRegion.getChunk(
                    XaeroWorldMapFold.tileChunkInRegion(pieceX.canonicalTile()),
                    XaeroWorldMapFold.tileChunkInRegion(pieceZ.canonicalTile()));
            GpuTextureAndView texture = piece == null ? null : exotic_worlds$textureOf(piece);
            if (texture == null) {
                continue;
            }

            if (canonicalRegion != this.exotic_worlds$foldedRegion) {
                support.bumpLoadedRegion(this.exotic_worlds$processor, canonicalRegion);
            }

            float pieceXOnScreen = x + pieceX.rawOffset() * CoordinateConstants.CHUNK_WIDTH;
            float pieceYOnScreen = y + pieceZ.rawOffset() * CoordinateConstants.CHUNK_WIDTH;
            int pieceWidth = pieceX.count() * CoordinateConstants.CHUNK_WIDTH;
            int pieceHeight = pieceZ.count() * CoordinateConstants.CHUNK_WIDTH;
            if (pieceZ.firstInside() == 0) {
                original.call(support, matrix, pieceXOnScreen, pieceYOnScreen,
                        pieceX.firstInside() * CoordinateConstants.CHUNK_WIDTH, textureY, (float) pieceWidth,
                        (float) pieceHeight, piece, noLightRenderer, withLightRenderer, helper);
                continue;
            }

            // prepareMapTexturedRect starts every piece at the tile's first row, so a piece inside it goes direct.
            MultiTextureRenderUtil.prepareTexturedRect(matrix, pieceXOnScreen, pieceYOnScreen,
                    pieceX.firstInside() * CoordinateConstants.CHUNK_WIDTH,
                    (pieceZ.firstInside() + pieceZ.count()) * CoordinateConstants.CHUNK_WIDTH,
                    pieceWidth, pieceHeight, -pieceHeight, XaeroWorldMapFold.SLOT_BLOCKS, texture.view,
                    piece.getLeafTexture().getTextureHasLight() ? withLightRenderer : noLightRenderer);
        }
    }

    @Unique
    private @Nullable MapRegion exotic_worlds$canonicalRegion(int canonicalTileX, int canonicalTileZ) {
        int regionX = XaeroWorldMapFold.regionOfTileChunk(canonicalTileX);
        int regionZ = XaeroWorldMapFold.regionOfTileChunk(canonicalTileZ);
        return this.exotic_worlds$fetchIsLeaf
                ? this.exotic_worlds$processor.getLeafMapRegion(this.exotic_worlds$fetchLeafLayer, regionX, regionZ, false)
                : this.exotic_worlds$processor.getMinimapMapRegion(regionX, regionZ);
    }

    @Unique
    private static @Nullable GpuTextureAndView exotic_worlds$textureOf(MapTileChunk chunk) {
        return chunk.getLeafTexture().getGlColorTexture();
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getX()I"))
    private int exotic_worlds$placeAtMirrorX(MapTileChunk chunk, Operation<Integer> original) {
        return this.exotic_worlds$shape != null ? this.exotic_worlds$mirrorTileX : original.call(chunk);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getZ()I"))
    private int exotic_worlds$placeAtMirrorZ(MapTileChunk chunk, Operation<Integer> original) {
        return this.exotic_worlds$shape != null ? this.exotic_worlds$mirrorTileZ : original.call(chunk);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/common/mods/SupportXaeroWorldmap;bumpLoadedRegion(Lxaero/map/MapProcessor;Lxaero/map/region/MapRegion;)V"))
    private void exotic_worlds$bumpFoldedRegion(SupportXaeroWorldmap support, MapProcessor processor, MapRegion region,
            Operation<Void> original) {
        MapRegion actual = this.exotic_worlds$foldedRegion != null ? this.exotic_worlds$foldedRegion : region;
        if (actual != null) {
            original.call(support, processor, actual);
        }
    }
}
