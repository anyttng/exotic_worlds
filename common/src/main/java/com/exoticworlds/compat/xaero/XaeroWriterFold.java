package com.exoticworlds.compat.xaero;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

public final class XaeroWriterFold {
    private @Nullable ToroidalShape shape;
    private AxisCopies chunkCopiesX = AxisCopies.UNBOUNDED;
    private AxisCopies chunkCopiesZ = AxisCopies.UNBOUNDED;
    private @Nullable ChunkPos foldedTile;
    private int foldedTileChunkX;
    private int foldedTileChunkZ;
    private long @Nullable [] visitRegions;
    private boolean foldedPosition;
    private int insideX;
    private int insideZ;
    private int lastInsideX;
    private int lastInsideZ;

    public void beginRender() {
        beginRender(XaeroWorldMapFold.browsedShape());
    }

    void beginRender(@Nullable ToroidalShape shape) {
        this.shape = shape;
        this.chunkCopiesX = AxisCopies.ofChunks(shape, Direction.Axis.X);
        this.chunkCopiesZ = AxisCopies.ofChunks(shape, Direction.Axis.Z);
        this.foldedTile = null;
    }

    public int tileX(int tileChunkX, int chunkX, int chunkZ) {
        return this.shape == null ? tileChunkX : foldedTile(chunkX, chunkZ).x;
    }

    public int tileZ(int tileChunkZ, int chunkX, int chunkZ) {
        return this.shape == null ? tileChunkZ : foldedTile(chunkX, chunkZ).z;
    }

    public int tileInRegionX(int tileChunkLocalX, int chunkX, int chunkZ) {
        return this.shape == null
                ? tileChunkLocalX : XaeroWorldMapFold.tileChunkInRegion(foldedTile(chunkX, chunkZ).x);
    }

    public int tileInRegionZ(int tileChunkLocalZ, int chunkX, int chunkZ) {
        return this.shape == null
                ? tileChunkLocalZ : XaeroWorldMapFold.tileChunkInRegion(foldedTile(chunkX, chunkZ).z);
    }

    private ChunkPos foldedTile(int chunkX, int chunkZ) {
        ChunkPos folded = this.foldedTile;
        if (folded == null || chunkX != this.foldedTileChunkX || chunkZ != this.foldedTileChunkZ) {
            folded = XaeroWorldMapFold.tileOfChunk(this.shape, chunkX, chunkZ);
            this.foldedTile = folded;
            this.foldedTileChunkX = chunkX;
            this.foldedTileChunkZ = chunkZ;
        }

        return folded;
    }

    public void enterChunk(int chunkX, int chunkZ) {
        this.foldedPosition = this.shape != null;
        if (!this.foldedPosition) {
            return;
        }

        ChunkPos canonical = canonicalChunk(chunkX, chunkZ);
        this.insideX = XaeroWorldMapFold.insideTile(canonical.x);
        this.insideZ = XaeroWorldMapFold.insideTile(canonical.z);
        this.lastInsideX = XaeroWorldMapFold.lastInsideTile(this.chunkCopiesX, canonical.x);
        this.lastInsideZ = XaeroWorldMapFold.lastInsideTile(this.chunkCopiesZ, canonical.z);
    }

    public int insideX(int raw) {
        return this.foldedPosition ? this.insideX : raw;
    }

    public int insideZ(int raw) {
        return this.foldedPosition ? this.insideZ : raw;
    }

    public int lastInsideX(int raw) {
        return this.foldedPosition ? this.lastInsideX : raw;
    }

    public int lastInsideZ(int raw) {
        return this.foldedPosition ? this.lastInsideZ : raw;
    }

    public ChunkPos canonicalChunk(int chunkX, int chunkZ) {
        return XaeroWorldMapFold.canonicalChunk(this.shape, chunkX, chunkZ);
    }

    public int visitFromRegionX(int startRegionX, int startTileChunkX, int startTileChunkZ, int endTileChunkX,
            int endTileChunkZ) {
        this.visitRegions = this.shape == null ? null : XaeroWorldMapFold.canonicalRegions(this.shape, startTileChunkX,
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
        return this.shape == null
                ? new ChunkPos(comparisonX, comparisonZ)
                : XaeroWorldMapFold.foldComparison(this.shape, comparisonX, comparisonZ);
    }

    public ChunkPos comparisonLeaf(int comparisonX, int comparisonZ, int leafX, int leafZ) {
        return this.shape == null ? new ChunkPos(leafX, leafZ) : comparison(comparisonX, comparisonZ);
    }
}
