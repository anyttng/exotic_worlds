package com.exoticworlds.compat.xaero;

import java.util.List;
import java.util.function.Consumer;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TileArea;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;

public final class XaeroMinimapTiles {
    private static final int NO_TEXTURE = -1;

    private int fetchRegionX;
    private int fetchRegionZ;
    private boolean fetchIsLeaf;
    private int fetchLeafLayer;
    private int mirrorTileX;
    private int mirrorTileZ;
    private @Nullable MapRegion foldedRegion;
    private @Nullable MapProcessor processor;
    private @Nullable List<TileArea> areas;
    private @Nullable ToroidalShape shape;
    private AxisCopies copiesX = AxisCopies.UNBOUNDED;
    private AxisCopies copiesZ = AxisCopies.UNBOUNDED;
    private WorldCopies.@Nullable Window window;

    @FunctionalInterface
    public interface RegionFetch {
        @Nullable MapRegion fetch(int regX, int regZ);
    }

    @FunctionalInterface
    public interface ChunkFetch {
        @Nullable MapTileChunk fetch(MapRegion region, int localX, int localZ);
    }

    @FunctionalInterface
    public interface TileDraw {
        void draw(XaeroPieceRect rect, MapTileChunk chunk);
    }

    public void begin(int minViewX, int maxViewX, int minViewZ, int maxViewZ) {
        ToroidalShape shape = XaeroWorldMapFold.browsedShape();
        this.shape = shape;
        this.copiesX = AxisCopies.of(shape, Direction.Axis.X);
        this.copiesZ = AxisCopies.of(shape, Direction.Axis.Z);
        this.window = shape == null ? null : WorldCopies.Window.of(shape,
                XaeroWorldMapFold.tileBlock(minViewX), XaeroWorldMapFold.tileBlock(minViewZ),
                XaeroWorldMapFold.tileBlock(maxViewX + 1), XaeroWorldMapFold.tileBlock(maxViewZ + 1));
    }

    public @Nullable MapRegion minimapRegion(int regX, int regZ, RegionFetch original) {
        this.fetchRegionX = regX;
        this.fetchRegionZ = regZ;
        this.fetchIsLeaf = false;
        return foldedFetch(regX, regZ, original);
    }

    public @Nullable MapRegion leafRegion(int caveLayer, int regX, int regZ, RegionFetch original) {
        this.fetchRegionX = regX;
        this.fetchRegionZ = regZ;
        this.fetchIsLeaf = true;
        this.fetchLeafLayer = caveLayer;
        return foldedFetch(regX, regZ, original);
    }

    private @Nullable MapRegion foldedFetch(int regX, int regZ, RegionFetch original) {
        MapRegion existing = original.fetch(regX, regZ);
        if (existing != null || this.shape == null) {
            return existing;
        }

        // A candidate value only, so the null-guarded chunk fetch runs at all; the chunk fetch re-fetches precisely.
        ChunkPos folded = XaeroWorldMapFold.foldRegion(this.shape, regX, regZ);
        return original.fetch(folded.x, folded.z);
    }

    public @Nullable MapTileChunk canonicalChunk(@Nullable MapRegion region, int localX, int localZ,
            ChunkFetch original) {
        int mirrorTileX = XaeroWorldMapFold.firstTileChunkOfRegion(this.fetchRegionX) + localX;
        int mirrorTileZ = XaeroWorldMapFold.firstTileChunkOfRegion(this.fetchRegionZ) + localZ;
        this.mirrorTileX = mirrorTileX;
        this.mirrorTileZ = mirrorTileZ;
        this.foldedRegion = null;
        this.areas = null;
        WorldCopies.Window window = this.window;
        WorldMapSession session = window == null
                || !XaeroWorldMapFold.tileLeavesWorld(this.copiesX, this.copiesZ, mirrorTileX, mirrorTileZ)
                ? null : WorldMapSession.getCurrentSession();
        if (window == null || session == null) {
            return region == null ? null : original.fetch(region, localX, localZ);
        }

        MapProcessor processor = session.getMapProcessor();
        this.processor = processor;
        List<TileArea> areas = XaeroWorldMapFold.tilePieces(window, mirrorTileX, mirrorTileZ);
        MapTileChunk shown = null;
        for (TileArea area : areas) {
            int canonicalTileX = area.x().canonicalTile();
            int canonicalTileZ = area.z().canonicalTile();
            MapRegion canonicalRegion = canonicalRegion(processor, canonicalTileX, canonicalTileZ);
            if (canonicalRegion == null) {
                continue;
            }

            if (canonicalRegion != region) {
                processor.beforeMinimapRegionRender(canonicalRegion);
            }

            MapTileChunk chunk = original.fetch(canonicalRegion, XaeroWorldMapFold.tileChunkInRegion(canonicalTileX),
                    XaeroWorldMapFold.tileChunkInRegion(canonicalTileZ));
            if (chunk != null && (shown == null || (textureOf(shown) == NO_TEXTURE && textureOf(chunk) != NO_TEXTURE))) {
                shown = chunk;
                this.foldedRegion = canonicalRegion;
            }
        }

        if (areas.size() > 1) {
            this.areas = areas;
        }

        return shown;
    }

    public void drawPieces(SupportXaeroWorldmap support, Matrix4f matrix, XaeroPieceRect tile, MapTileChunk chunk,
            MultiTextureRenderTypeRenderer noLightRenderer, MultiTextureRenderTypeRenderer withLightRenderer,
            MinimapRendererHelper helper, TileDraw original) {
        List<TileArea> areas = this.areas;
        MapProcessor processor = this.processor;
        if (areas == null || processor == null) {
            original.draw(tile, chunk);
            return;
        }

        for (TileArea area : areas) {
            TilePiece pieceX = area.x();
            TilePiece pieceZ = area.z();
            MapRegion canonicalRegion = canonicalRegion(processor, pieceX.canonicalTile(), pieceZ.canonicalTile());
            MapTileChunk piece = canonicalRegion == null ? null : canonicalRegion.getChunk(
                    XaeroWorldMapFold.tileChunkInRegion(pieceX.canonicalTile()),
                    XaeroWorldMapFold.tileChunkInRegion(pieceZ.canonicalTile()));
            int texture = piece == null ? NO_TEXTURE : textureOf(piece);
            if (canonicalRegion == null || piece == null || texture == NO_TEXTURE) {
                continue;
            }

            if (canonicalRegion != this.foldedRegion) {
                support.bumpLoadedRegion(processor, canonicalRegion);
            }

            XaeroPieceRect rect = XaeroPieceRect.of(tile, pieceX, pieceZ);
            if (XaeroPieceRect.startsOnTileRow(pieceZ)) {
                original.draw(rect, piece);
                continue;
            }

            // prepareMapTexturedRect starts every piece at the tile's first row, so a piece inside it goes direct.
            helper.prepareMyTexturedModalRect(matrix, rect.x(), rect.y(), rect.textureX(), rect.textureY(),
                    rect.width(), rect.height(), -rect.height(), XaeroWorldMapFold.SLOT_BLOCKS, texture,
                    piece.getLeafTexture().getTextureHasLight() ? withLightRenderer : noLightRenderer);
        }
    }

    private @Nullable MapRegion canonicalRegion(MapProcessor processor, int canonicalTileX, int canonicalTileZ) {
        int regionX = XaeroWorldMapFold.regionOfTileChunk(canonicalTileX);
        int regionZ = XaeroWorldMapFold.regionOfTileChunk(canonicalTileZ);
        return this.fetchIsLeaf
                ? processor.getLeafMapRegion(this.fetchLeafLayer, regionX, regionZ, false)
                : processor.getMinimapMapRegion(regionX, regionZ);
    }

    private static int textureOf(MapTileChunk chunk) {
        return chunk.getLeafTexture().getGlColorTexture();
    }

    public int placedTileX(int raw) {
        return this.shape != null ? this.mirrorTileX : raw;
    }

    public int placedTileZ(int raw) {
        return this.shape != null ? this.mirrorTileZ : raw;
    }

    public void bump(@Nullable MapRegion region, Consumer<MapRegion> original) {
        MapRegion bumped = this.foldedRegion != null ? this.foldedRegion : region;
        if (bumped != null) {
            original.accept(bumped);
        }
    }
}
