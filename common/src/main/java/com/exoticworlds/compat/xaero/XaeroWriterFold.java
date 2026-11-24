package com.exoticworlds.compat.xaero;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.map.AxisCopies;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

public final class XaeroWriterFold {
    private @Nullable WorldFold fold;
    private AxisCopies chunkCopiesX = AxisCopies.UNBOUNDED;
    private AxisCopies chunkCopiesZ = AxisCopies.UNBOUNDED;
    private @Nullable ChunkPos foldedTile;
    private int foldedTileChunkX;
    private int foldedTileChunkZ;
    private long @Nullable [] visitRegions;
    private boolean isFoldedPosition;
    private int insideX;
    private int insideZ;
    private int lastInsideX;
    private int lastInsideZ;

    public void beginRender() {
        beginRender(XaeroWorldMapFold.getBrowsedFold());
    }

    void beginRender(@Nullable WorldFold fold) {
        this.fold = fold;
        this.chunkCopiesX = AxisCopies.ofChunks(fold, Direction.Axis.X);
        this.chunkCopiesZ = AxisCopies.ofChunks(fold, Direction.Axis.Z);
        this.foldedTile = null;
    }

    public int tileX(int tileChunkX, int chunkX, int chunkZ) {
        return this.fold == null ? tileChunkX : foldedTile(chunkX, chunkZ).x();
    }

    public int tileZ(int tileChunkZ, int chunkX, int chunkZ) {
        return this.fold == null ? tileChunkZ : foldedTile(chunkX, chunkZ).z();
    }

    public int tileInRegionX(int tileChunkLocalX, int chunkX, int chunkZ) {
        return this.fold == null
                ? tileChunkLocalX : XaeroWorldMapFold.tileChunkInRegion(foldedTile(chunkX, chunkZ).x());
    }

    public int tileInRegionZ(int tileChunkLocalZ, int chunkX, int chunkZ) {
        return this.fold == null
                ? tileChunkLocalZ : XaeroWorldMapFold.tileChunkInRegion(foldedTile(chunkX, chunkZ).z());
    }

    private ChunkPos foldedTile(int chunkX, int chunkZ) {
        ChunkPos folded = this.foldedTile;
        if (folded == null || chunkX != this.foldedTileChunkX || chunkZ != this.foldedTileChunkZ) {
            folded = XaeroWorldMapFold.tileOfChunk(this.fold, chunkX, chunkZ);
            this.foldedTile = folded;
            this.foldedTileChunkX = chunkX;
            this.foldedTileChunkZ = chunkZ;
        }

        return folded;
    }

    public void enterChunk(int chunkX, int chunkZ) {
        this.isFoldedPosition = this.fold != null;
        if (!this.isFoldedPosition) {
            return;
        }

        ChunkPos canonical = canonicalChunk(chunkX, chunkZ);
        this.insideX = XaeroWorldMapFold.insideTile(canonical.x());
        this.insideZ = XaeroWorldMapFold.insideTile(canonical.z());
        this.lastInsideX = XaeroWorldMapFold.lastInsideTile(this.chunkCopiesX, canonical.x());
        this.lastInsideZ = XaeroWorldMapFold.lastInsideTile(this.chunkCopiesZ, canonical.z());
    }

    public int getInsideX(int raw) {
        return this.isFoldedPosition ? this.insideX : raw;
    }

    public int getInsideZ(int raw) {
        return this.isFoldedPosition ? this.insideZ : raw;
    }

    public int getLastInsideX(int raw) {
        return this.isFoldedPosition ? this.lastInsideX : raw;
    }

    public int getLastInsideZ(int raw) {
        return this.isFoldedPosition ? this.lastInsideZ : raw;
    }

    public ChunkPos canonicalChunk(int chunkX, int chunkZ) {
        return XaeroWorldMapFold.canonicalChunk(this.fold, chunkX, chunkZ);
    }

    public int visitFromRegionX(int startRegionX, int startTileChunkX, int startTileChunkZ, int endTileChunkX,
            int endTileChunkZ) {
        this.visitRegions = this.fold == null ? null : XaeroWorldMapFold.canonicalRegions(this.fold, startTileChunkX,
                startTileChunkZ, endTileChunkX, endTileChunkZ);
        return this.visitRegions == null ? startRegionX : 0;
    }

    public int visitFromRegionZ(int startRegionZ) {
        return this.visitRegions == null ? startRegionZ : 0;
    }

    public int visitToRegionX(int endRegionX) {
        return this.visitRegions == null ? endRegionX : 0;
    }

    public int visitToRegionZ(int endRegionZ) {
        long[] regions = this.visitRegions;
        return regions == null ? endRegionZ : regions.length - 1;
    }

    public ChunkPos visitedRegion(int regionX, int regionZ) {
        long[] regions = this.visitRegions;
        if (regions == null) {
            return new ChunkPos(regionX, regionZ);
        }

        long region = regions[regionZ];
        return new ChunkPos(ChunkPos.getX(region), ChunkPos.getZ(region));
    }

    public ChunkPos comparison(int comparisonX, int comparisonZ) {
        return this.fold == null
                ? new ChunkPos(comparisonX, comparisonZ)
                : XaeroWorldMapFold.foldComparison(this.fold, comparisonX, comparisonZ);
    }

    public ChunkPos comparisonLeaf(int comparisonX, int comparisonZ, int leafX, int leafZ) {
        return this.fold == null ? new ChunkPos(leafX, leafZ) : comparison(comparisonX, comparisonZ);
    }
}
