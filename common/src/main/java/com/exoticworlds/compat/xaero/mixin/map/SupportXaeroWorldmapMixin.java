package com.exoticworlds.compat.xaero.mixin.map;

import java.util.List;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;
import com.exoticworlds.core.CoordinateConstants;

import net.minecraft.core.Direction;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
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
    private static final int NO_TEXTURE = -1;

    @Unique
    private MapRegion toroidal$foldedRegion;
    @Unique
    private MapProcessor toroidal$processor;
    @Unique
    private @Nullable List<TilePiece> toroidal$piecesX;
    @Unique
    private @Nullable List<TilePiece> toroidal$piecesZ;

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
        if (existing != null || !XaeroWorldMapFold.active()) {
            return existing;
        }

        // A candidate value only, so the null-guarded chunk fetch runs at all; the chunk redirect re-fetches precisely.
        return original.call(
                processor,
                XaeroWorldMapFold.foldRegion(Direction.Axis.X, regX),
                XaeroWorldMapFold.foldRegion(Direction.Axis.Z, regZ));
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
        if (existing != null || !XaeroWorldMapFold.active()) {
            return existing;
        }

        return original.call(
                processor,
                caveLayer,
                XaeroWorldMapFold.foldRegion(Direction.Axis.X, regX),
                XaeroWorldMapFold.foldRegion(Direction.Axis.Z, regZ),
                create);
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
        if (!XaeroWorldMapFold.active()) {
            return region == null ? null : original.call(region, localX, localZ);
        }

        this.toroidal$piecesX = null;
        this.toroidal$piecesZ = null;
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            return region == null ? null : original.call(region, localX, localZ);
        }

        MapProcessor processor = session.getMapProcessor();
        this.toroidal$processor = processor;
        List<TilePiece> piecesX = XaeroWorldMapFold.tilePieces(XaeroWorldMapFold.chunkCopies(Direction.Axis.X), mirrorTileX);
        List<TilePiece> piecesZ = XaeroWorldMapFold.tilePieces(XaeroWorldMapFold.chunkCopies(Direction.Axis.Z), mirrorTileZ);
        MapTileChunk shown = null;
        for (TilePiece pieceX : piecesX) {
            for (TilePiece pieceZ : piecesZ) {
                MapRegion canonicalRegion = toroidal$canonicalRegion(pieceX.canonicalTile(), pieceZ.canonicalTile());
                if (canonicalRegion == null) {
                    continue;
                }

                if (canonicalRegion != region) {
                    processor.beforeMinimapRegionRender(canonicalRegion);
                }

                MapTileChunk chunk = original.call(canonicalRegion,
                        XaeroWorldMapFold.tileChunkInRegion(pieceX.canonicalTile()),
                        XaeroWorldMapFold.tileChunkInRegion(pieceZ.canonicalTile()));
                if (chunk != null && (shown == null
                        || toroidal$textureOf(shown) == NO_TEXTURE && toroidal$textureOf(chunk) != NO_TEXTURE)) {
                    shown = chunk;
                    this.toroidal$foldedRegion = canonicalRegion;
                }
            }
        }

        if (piecesX.size() > 1 || piecesZ.size() > 1) {
            this.toroidal$piecesX = piecesX;
            this.toroidal$piecesZ = piecesZ;
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
        List<TilePiece> piecesX = this.toroidal$piecesX;
        List<TilePiece> piecesZ = this.toroidal$piecesZ;
        if (piecesX == null || piecesZ == null) {
            original.call(support, matrix, x, y, textureX, textureY, width, height, chunk, noLightRenderer,
                    withLightRenderer, helper);
            return;
        }

        for (TilePiece pieceX : piecesX) {
            for (TilePiece pieceZ : piecesZ) {
                MapRegion canonicalRegion = toroidal$canonicalRegion(pieceX.canonicalTile(), pieceZ.canonicalTile());
                MapTileChunk piece = canonicalRegion == null ? null : canonicalRegion.getChunk(
                        XaeroWorldMapFold.tileChunkInRegion(pieceX.canonicalTile()),
                        XaeroWorldMapFold.tileChunkInRegion(pieceZ.canonicalTile()));
                int texture = piece == null ? NO_TEXTURE : toroidal$textureOf(piece);
                if (texture == NO_TEXTURE) {
                    continue;
                }

                if (canonicalRegion != this.toroidal$foldedRegion) {
                    support.bumpLoadedRegion(this.toroidal$processor, canonicalRegion);
                }

                helper.prepareMyTexturedModalRect(matrix,
                        x + pieceX.rawOffset() * CoordinateConstants.CHUNK_WIDTH,
                        y + pieceZ.rawOffset() * CoordinateConstants.CHUNK_WIDTH,
                        pieceX.firstInside() * CoordinateConstants.CHUNK_WIDTH,
                        (pieceZ.firstInside() + pieceZ.count()) * CoordinateConstants.CHUNK_WIDTH,
                        pieceX.count() * CoordinateConstants.CHUNK_WIDTH,
                        pieceZ.count() * CoordinateConstants.CHUNK_WIDTH,
                        -pieceZ.count() * CoordinateConstants.CHUNK_WIDTH,
                        XaeroWorldMapFold.SLOT_BLOCKS,
                        texture,
                        piece.getLeafTexture().getTextureHasLight() ? withLightRenderer : noLightRenderer);
            }
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
    private static int toroidal$textureOf(MapTileChunk chunk) {
        return chunk.getLeafTexture().getGlColorTexture();
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getX()I"))
    private int toroidal$placeAtMirrorX(MapTileChunk chunk, Operation<Integer> original) {
        return XaeroWorldMapFold.active() ? this.toroidal$mirrorTileX : original.call(chunk);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getZ()I"))
    private int toroidal$placeAtMirrorZ(MapTileChunk chunk, Operation<Integer> original) {
        return XaeroWorldMapFold.active() ? this.toroidal$mirrorTileZ : original.call(chunk);
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
