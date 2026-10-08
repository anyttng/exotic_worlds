package com.exoticworlds.compat.xaero;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;

import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;
import xaero.map.file.MapRegionInfo;
import xaero.map.file.OldFormatSupport;
import xaero.map.region.ExportMapRegion;
import xaero.map.region.ExportMapTileChunk;
import xaero.map.region.MapLayer;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.texture.ExportLeafRegionTexture;
import xaero.map.region.texture.LeafRegionTexture;
import xaero.map.world.MapDimension;

public final class XaeroExportAssembly {
    private static final String CACHE_REASON = "png";
    private static final byte TILE_CHUNK_LOADED = 2;
    private static final int CACHE_LOAD_ATTEMPTS = 1;
    private static final int TILE_PIXELS = 64;
    private static final int CHUNK_PIXELS = 16;
    private static final int BYTES_PER_PIXEL = 4;
    private static final byte[] EMPTY_TILE = new byte[TILE_PIXELS * TILE_PIXELS * BYTES_PER_PIXEL];

    private record SlotPiece(ExportMapTileChunk chunk, TilePiece x, TilePiece z) {
    }

    private static final class Loaded {
        private final int regionX;
        private final int regionZ;
        private final ExportMapRegion region;
        private final boolean[][] handedOut =
                new boolean[XaeroWorldMapFold.REGION_TILE_CHUNKS][XaeroWorldMapFold.REGION_TILE_CHUNKS];

        private Loaded(int regionX, int regionZ, ExportMapRegion region) {
            this.regionX = regionX;
            this.regionZ = regionZ;
            this.region = region;
        }
    }

    private MapProcessor processor;
    private Registry<Biome> biomes;
    private OldFormatSupport oldFormatSupport;
    private MapUpdateFastConfig updateConfig;
    private boolean includingHighlights;

    private final List<Loaded> loaded = new ArrayList<>();
    private final List<SlotPiece> slotPieces = new ArrayList<>();
    private final ByteBuffer composite = ByteBuffer.allocateDirect(EMPTY_TILE.length);
    private boolean assembling;
    private int rawRegionX;
    private int rawRegionZ;
    private int regionX;
    private int regionZ;
    private int tileChunkX;
    private int tileChunkZ;

    public void begin(MapProcessor processor, Registry<Biome> biomes, OldFormatSupport oldFormatSupport) {
        this.processor = processor;
        this.biomes = biomes;
        this.oldFormatSupport = oldFormatSupport;
        this.updateConfig = null;
        this.assembling = false;
        this.loaded.clear();
    }

    public void enterRegion(int rawRegionX, int rawRegionZ) {
        release();
        this.rawRegionX = rawRegionX;
        this.rawRegionZ = rawRegionZ;
        this.regionX = rawRegionX;
        this.regionZ = rawRegionZ;
        this.assembling = XaeroWorldMapFold.regionCrossesSeam(Direction.Axis.X, rawRegionX)
                || XaeroWorldMapFold.regionCrossesSeam(Direction.Axis.Z, rawRegionZ);
        if (!this.assembling) {
            return;
        }

        int[] candidatesX = XaeroWorldMapFold.canonicalRegions(Direction.Axis.X,
                XaeroWorldMapFold.firstTileChunkOfRegion(rawRegionX),
                XaeroWorldMapFold.firstTileChunkOfRegion(rawRegionX + 1) - 1);
        int[] candidatesZ = XaeroWorldMapFold.canonicalRegions(Direction.Axis.Z,
                XaeroWorldMapFold.firstTileChunkOfRegion(rawRegionZ),
                XaeroWorldMapFold.firstTileChunkOfRegion(rawRegionZ + 1) - 1);
        for (int candidateX : candidatesX) {
            for (int candidateZ : candidatesZ) {
                if (hasSource(candidateX, candidateZ)) {
                    this.regionX = candidateX;
                    this.regionZ = candidateZ;
                    return;
                }
            }
        }
    }

    public boolean assembling() {
        return this.assembling;
    }

    public int regionX() {
        return this.regionX;
    }

    public int regionZ() {
        return this.regionZ;
    }

    public void adopt(ExportMapRegion region) {
        if (this.assembling) {
            this.loaded.add(new Loaded(this.regionX, this.regionZ, region));
        }
    }

    public @Nullable ExportMapTileChunk chunk(int slotX, int slotZ) {
        this.tileChunkX = XaeroWorldMapFold.firstTileChunkOfRegion(this.rawRegionX) + slotX;
        this.tileChunkZ = XaeroWorldMapFold.firstTileChunkOfRegion(this.rawRegionZ) + slotZ;
        this.slotPieces.clear();
        List<TilePiece> piecesX = XaeroWorldMapFold.tilePieces(XaeroWorldMapFold.chunkCopies(Direction.Axis.X), this.tileChunkX);
        List<TilePiece> piecesZ = XaeroWorldMapFold.tilePieces(XaeroWorldMapFold.chunkCopies(Direction.Axis.Z), this.tileChunkZ);
        ExportMapTileChunk carrier = null;
        for (TilePiece pieceX : piecesX) {
            for (TilePiece pieceZ : piecesZ) {
                ExportMapTileChunk chunk = canonicalChunk(pieceX.canonicalTile(), pieceZ.canonicalTile());
                if (chunk == null) {
                    continue;
                }

                this.slotPieces.add(new SlotPiece(chunk, pieceX, pieceZ));
                if (carrier == null) {
                    carrier = chunk;
                }
            }
        }

        if (piecesX.size() == 1 && piecesZ.size() == 1) {
            this.slotPieces.clear();
        }

        return carrier;
    }

    public ByteBuffer colorBuffer(ByteBuffer carrierBuffer) {
        if (this.slotPieces.isEmpty()) {
            return carrierBuffer;
        }

        ByteBuffer composite = this.composite;
        composite.put(0, EMPTY_TILE, 0, EMPTY_TILE.length);
        ExportMapTileChunk carrier = this.slotPieces.get(0).chunk();
        for (SlotPiece piece : this.slotPieces) {
            ExportLeafRegionTexture texture = piece.chunk().getLeafTexture();
            if (texture.getColorBuffer() == null) {
                texture.prepareBuffer();
            }

            copyPiece(texture.getDirectColorBuffer(), composite, piece.x(), piece.z());
            if (piece.chunk() != carrier) {
                texture.deleteColorBuffer();
            }
        }

        return composite;
    }

    private static void copyPiece(ByteBuffer source, ByteBuffer target, TilePiece pieceX, TilePiece pieceZ) {
        int rowBytes = pieceX.count() * CHUNK_PIXELS * BYTES_PER_PIXEL;
        for (int row = 0; row < pieceZ.count() * CHUNK_PIXELS; row++) {
            int sourceRow = pieceZ.firstInside() * CHUNK_PIXELS + row;
            int targetRow = pieceZ.rawOffset() * CHUNK_PIXELS + row;
            int sourcePos = (sourceRow * TILE_PIXELS + pieceX.firstInside() * CHUNK_PIXELS) * BYTES_PER_PIXEL;
            int targetPos = (targetRow * TILE_PIXELS + pieceX.rawOffset() * CHUNK_PIXELS) * BYTES_PER_PIXEL;
            target.put(targetPos, source, sourcePos, rowBytes);
        }
    }

    private @Nullable ExportMapTileChunk canonicalChunk(int canonicalX, int canonicalZ) {
        int canonicalRegionX = XaeroWorldMapFold.regionOfTileChunk(canonicalX);
        int canonicalRegionZ = XaeroWorldMapFold.regionOfTileChunk(canonicalZ);
        int canonicalSlotX = XaeroWorldMapFold.tileChunkInRegion(canonicalX);
        int canonicalSlotZ = XaeroWorldMapFold.tileChunkInRegion(canonicalZ);
        for (Loaded entry : this.loaded) {
            if (entry.regionX != canonicalRegionX || entry.regionZ != canonicalRegionZ) {
                continue;
            }

            if (entry.region == null) {
                return null;
            }

            if (!entry.handedOut[canonicalSlotX][canonicalSlotZ]) {
                return handOut(entry, canonicalSlotX, canonicalSlotZ);
            }
        }

        Loaded fresh = new Loaded(canonicalRegionX, canonicalRegionZ, load(canonicalRegionX, canonicalRegionZ));
        this.loaded.add(fresh);
        return fresh.region == null ? null : handOut(fresh, canonicalSlotX, canonicalSlotZ);
    }

    public int tileChunkX() {
        return this.tileChunkX;
    }

    public int tileChunkZ() {
        return this.tileChunkZ;
    }

    public void end() {
        release();
        this.assembling = false;
    }

    private static ExportMapTileChunk handOut(Loaded entry, int slotX, int slotZ) {
        entry.handedOut[slotX][slotZ] = true;
        return entry.region.getChunk(slotX, slotZ);
    }

    private boolean hasSource(int regionX, int regionZ) {
        int caveLayer = this.processor.getCurrentCaveLayer();
        return this.processor.getLeafMapRegion(caveLayer, regionX, regionZ, false) != null
                || mapLayer(caveLayer).regionDetectionExists(regionX, regionZ);
    }

    private MapDimension dimension() {
        return this.processor.getMapWorld().getCurrentDimension();
    }

    private MapLayer mapLayer(int caveLayer) {
        return dimension().getLayeredMapRegions().getLayer(caveLayer);
    }

    private @Nullable ExportMapRegion load(int regionX, int regionZ) {
        if (this.updateConfig == null) {
            this.updateConfig = new MapUpdateFastConfig(this.processor);
            this.includingHighlights = (Boolean) WorldMap.INSTANCE.getConfigs().getClientConfigManager()
                    .getPrimaryConfigManager().getEffective(WorldMapPrimaryClientConfigOptions.EXPORT_HIGHLIGHTS);
        }

        int caveLayer = this.processor.getCurrentCaveLayer();
        MapDimension dimension = dimension();
        MapLayer mapLayer = mapLayer(caveLayer);
        MapRegion live = this.processor.getLeafMapRegion(caveLayer, regionX, regionZ, false);
        MapRegionInfo info = live;
        if (live == null && mapLayer.regionDetectionExists(regionX, regionZ)) {
            info = mapLayer.getRegionDetection(regionX, regionZ);
        }

        boolean highlightsIfUndiscovered = this.includingHighlights
                && dimension.getHighlightHandler().shouldApplyRegionHighlights(regionX, regionZ, false);
        if (info == null && !highlightsIfUndiscovered) {
            return null;
        }

        File cacheFile = null;
        boolean fromCache = info != null && (live == null || !live.isBeingWritten() || live.getLoadState() != TILE_CHUNK_LOADED);
        if (fromCache) {
            cacheFile = info.getCacheFile();
            if (cacheFile == null && !info.hasLookedForCache()) {
                try {
                    cacheFile = this.processor.getMapSaveLoad().getCacheFile(info, caveLayer, true, false);
                } catch (IOException ignored) {
                    cacheFile = null;
                }
            }

            if (cacheFile == null) {
                if (!highlightsIfUndiscovered) {
                    return null;
                }

                fromCache = false;
            }
        }

        ExportMapRegion region = new ExportMapRegion(dimension, regionX, regionZ, caveLayer, this.biomes);
        if (fromCache) {
            region.setShouldCache(true, CACHE_REASON);
            region.setHasHadTerrain();
            region.setCacheFile(cacheFile);
            region.loadCacheTextures(this.processor, this.biomes, false, null, 0, null, new boolean[1],
                    CACHE_LOAD_ATTEMPTS, this.oldFormatSupport);
        } else if (live != null) {
            copyTiles(live, region);
        }

        if (this.includingHighlights) {
            this.processor.getMapRegionHighlightsPreparer().prepare(region, true);
        }

        return region;
    }

    private void copyTiles(MapRegion live, ExportMapRegion region) {
        for (int slotX = 0; slotX < XaeroWorldMapFold.REGION_TILE_CHUNKS; slotX++) {
            for (int slotZ = 0; slotZ < XaeroWorldMapFold.REGION_TILE_CHUNKS; slotZ++) {
                MapTileChunk source = live.getChunk(slotX, slotZ);
                if (source == null || !source.hasHadTerrain()) {
                    continue;
                }

                MapTileChunk target = region.createTexture(slotX, slotZ).getTileChunk();
                for (int tileX = 0; tileX < MapTileChunk.SIDE_LENGTH; tileX++) {
                    for (int tileZ = 0; tileZ < MapTileChunk.SIDE_LENGTH; tileZ++) {
                        target.setTile(tileX, tileZ, source.getTile(tileX, tileZ),
                                this.processor.getBlockStateShortShapeCache(), this.processor);
                    }
                }

                target.setLoadState(TILE_CHUNK_LOADED);
                target.updateBuffers(this.processor, this.processor.getWorldBlockTintProvider(),
                        this.processor.getOverlayManager(), WorldMap.detailed_debug,
                        this.processor.getBlockStateShortShapeCache(), this.updateConfig);
            }
        }
    }

    private void release() {
        for (Loaded entry : this.loaded) {
            if (entry.region == null) {
                continue;
            }

            for (int slotX = 0; slotX < XaeroWorldMapFold.REGION_TILE_CHUNKS; slotX++) {
                for (int slotZ = 0; slotZ < XaeroWorldMapFold.REGION_TILE_CHUNKS; slotZ++) {
                    ExportMapTileChunk chunk = entry.region.getChunk(slotX, slotZ);
                    LeafRegionTexture texture = chunk == null ? null : chunk.getLeafTexture();
                    if (texture != null) {
                        texture.deleteColorBuffer();
                    }
                }
            }
        }

        this.loaded.clear();
        this.slotPieces.clear();
    }
}
