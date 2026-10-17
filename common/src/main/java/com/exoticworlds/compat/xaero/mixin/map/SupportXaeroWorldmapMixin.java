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
    private int toroidal$fetchRegionX;
    @Unique
    private int toroidal$fetchRegionZ;
    @Unique
    private boolean toroidal$fetchIsLeaf;
    @Unique
    private int toroidal$fetchLeafLayer;
    @Unique
    private int toroidal$mirrorTileX;
    @Unique
    private int toroidal$mirrorTileZ;
    @Unique
    private MapRegion toroidal$foldedRegion;
    @Unique
    private MapProcessor toroidal$processor;
    @Unique
    private @Nullable List<TileArea> toroidal$areas;
    @Unique
    private @Nullable ToroidalShape toroidal$shape;
    @Unique
    private AxisCopies toroidal$copiesX = AxisCopies.UNBOUNDED;
    @Unique
    private AxisCopies toroidal$copiesZ = AxisCopies.UNBOUNDED;
    @Unique
    private WorldCopies.@Nullable Window toroidal$window;

    @Inject(method = "renderChunks", at = @At("HEAD"))
    private void toroidal$resolveChunksShape(CallbackInfo ci, @Local(argsOnly = true, name = "minViewX") int minViewX,
            @Local(argsOnly = true, name = "maxViewX") int maxViewX,
            @Local(argsOnly = true, name = "minViewZ") int minViewZ,
            @Local(argsOnly = true, name = "maxViewZ") int maxViewZ) {
        ToroidalShape shape = XaeroWorldMapFold.browsedShape();
        this.toroidal$shape = shape;
        this.toroidal$copiesX = AxisCopies.of(shape, Direction.Axis.X);
        this.toroidal$copiesZ = AxisCopies.of(shape, Direction.Axis.Z);
        this.toroidal$window = shape == null ? null : WorldCopies.Window.of(shape,
                XaeroWorldMapFold.tileBlock(minViewX), XaeroWorldMapFold.tileBlock(minViewZ),
                XaeroWorldMapFold.tileBlock(maxViewX + 1), XaeroWorldMapFold.tileBlock(maxViewZ + 1));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getMinimapMapRegion(II)Lxaero/map/region/MapRegion;"))
    private @Nullable MapRegion toroidal$fetchMinimapRegion(MapProcessor processor, int regX, int regZ,
            Operation<@Nullable MapRegion> original) {
        this.toroidal$fetchRegionX = regX;
        this.toroidal$fetchRegionZ = regZ;
        this.toroidal$fetchIsLeaf = false;
        MapRegion existing = original.call(processor, regX, regZ);
        if (existing != null || this.toroidal$shape == null) {
            return existing;
        }

        // A candidate value only, so the null-guarded chunk fetch runs at all; the chunk redirect re-fetches precisely.
        ChunkPos folded = XaeroWorldMapFold.foldRegion(this.toroidal$shape, regX, regZ);
        return original.call(processor, folded.x(), folded.z());
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private @Nullable MapRegion toroidal$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ,
            boolean create, Operation<@Nullable MapRegion> original) {
        this.toroidal$fetchRegionX = regX;
        this.toroidal$fetchRegionZ = regZ;
        this.toroidal$fetchIsLeaf = true;
        this.toroidal$fetchLeafLayer = caveLayer;
        MapRegion existing = original.call(processor, caveLayer, regX, regZ, create);
        if (existing != null || this.toroidal$shape == null) {
            return existing;
        }

        ChunkPos folded = XaeroWorldMapFold.foldRegion(this.toroidal$shape, regX, regZ);
        return original.call(processor, caveLayer, folded.x(), folded.z(), create);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/MapRegion;getChunk(II)Lxaero/map/region/MapTileChunk;"))
    private @Nullable MapTileChunk toroidal$fetchCanonicalChunk(MapRegion region, int localX, int localZ,
            Operation<MapTileChunk> original) {
        int mirrorTileX = XaeroWorldMapFold.firstTileChunkOfRegion(this.toroidal$fetchRegionX) + localX;
        int mirrorTileZ = XaeroWorldMapFold.firstTileChunkOfRegion(this.toroidal$fetchRegionZ) + localZ;
        this.toroidal$mirrorTileX = mirrorTileX;
        this.toroidal$mirrorTileZ = mirrorTileZ;
        this.toroidal$foldedRegion = null;
        this.toroidal$areas = null;
        WorldCopies.Window window = this.toroidal$window;
        if (window == null
                || !XaeroWorldMapFold.tileLeavesWorld(this.toroidal$copiesX, this.toroidal$copiesZ, mirrorTileX, mirrorTileZ)) {
            return region == null ? null : original.call(region, localX, localZ);
        }

        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            return region == null ? null : original.call(region, localX, localZ);
        }

        MapProcessor processor = session.getMapProcessor();
        this.toroidal$processor = processor;
        List<TileArea> areas = XaeroWorldMapFold.tilePieces(window, mirrorTileX, mirrorTileZ);
        MapTileChunk shown = null;
        for (TileArea area : areas) {
            int canonicalTileX = area.x().canonicalTile();
            int canonicalTileZ = area.z().canonicalTile();
            MapRegion canonicalRegion = toroidal$canonicalRegion(canonicalTileX, canonicalTileZ);
            if (canonicalRegion == null) {
                continue;
            }

            if (canonicalRegion != region) {
                processor.beforeMinimapRegionRender(canonicalRegion);
            }

            MapTileChunk chunk = original.call(canonicalRegion,
                    XaeroWorldMapFold.tileChunkInRegion(canonicalTileX), XaeroWorldMapFold.tileChunkInRegion(canonicalTileZ));
            if (chunk != null && (shown == null || toroidal$textureOf(shown) == null && toroidal$textureOf(chunk) != null)) {
                shown = chunk;
                this.toroidal$foldedRegion = canonicalRegion;
            }
        }

        if (areas.size() > 1) {
            this.toroidal$areas = areas;
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
    private void toroidal$drawTilePieces(SupportXaeroWorldmap support, Matrix4f matrix, float x, float y, int textureX,
            int textureY, float width, float height, MapTileChunk chunk, MultiTextureRenderTypeRenderer noLightRenderer,
            MultiTextureRenderTypeRenderer withLightRenderer, MinimapRendererHelper helper, Operation<Void> original) {
        List<TileArea> areas = this.toroidal$areas;
        if (areas == null) {
            original.call(support, matrix, x, y, textureX, textureY, width, height, chunk, noLightRenderer,
                    withLightRenderer, helper);
            return;
        }

        for (TileArea area : areas) {
            TilePiece pieceX = area.x();
            TilePiece pieceZ = area.z();
            MapRegion canonicalRegion = toroidal$canonicalRegion(pieceX.canonicalTile(), pieceZ.canonicalTile());
            MapTileChunk piece = canonicalRegion == null ? null : canonicalRegion.getChunk(
                    XaeroWorldMapFold.tileChunkInRegion(pieceX.canonicalTile()),
                    XaeroWorldMapFold.tileChunkInRegion(pieceZ.canonicalTile()));
            GpuTextureAndView texture = piece == null ? null : toroidal$textureOf(piece);
            if (texture == null) {
                continue;
            }

            if (canonicalRegion != this.toroidal$foldedRegion) {
                support.bumpLoadedRegion(this.toroidal$processor, canonicalRegion);
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
    private @Nullable MapRegion toroidal$canonicalRegion(int canonicalTileX, int canonicalTileZ) {
        int regionX = XaeroWorldMapFold.regionOfTileChunk(canonicalTileX);
        int regionZ = XaeroWorldMapFold.regionOfTileChunk(canonicalTileZ);
        return this.toroidal$fetchIsLeaf
                ? this.toroidal$processor.getLeafMapRegion(this.toroidal$fetchLeafLayer, regionX, regionZ, false)
                : this.toroidal$processor.getMinimapMapRegion(regionX, regionZ);
    }

    @Unique
    private static @Nullable GpuTextureAndView toroidal$textureOf(MapTileChunk chunk) {
        return chunk.getLeafTexture().getGlColorTexture();
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getX()I"))
    private int toroidal$placeAtMirrorX(MapTileChunk chunk, Operation<Integer> original) {
        return this.toroidal$shape != null ? this.toroidal$mirrorTileX : original.call(chunk);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getZ()I"))
    private int toroidal$placeAtMirrorZ(MapTileChunk chunk, Operation<Integer> original) {
        return this.toroidal$shape != null ? this.toroidal$mirrorTileZ : original.call(chunk);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/common/mods/SupportXaeroWorldmap;bumpLoadedRegion(Lxaero/map/MapProcessor;Lxaero/map/region/MapRegion;)V"))
    private void toroidal$bumpFoldedRegion(SupportXaeroWorldmap support, MapProcessor processor, MapRegion region,
            Operation<Void> original) {
        MapRegion actual = this.toroidal$foldedRegion != null ? this.toroidal$foldedRegion : region;
        if (actual != null) {
            original.call(support, processor, actual);
        }
    }
}
