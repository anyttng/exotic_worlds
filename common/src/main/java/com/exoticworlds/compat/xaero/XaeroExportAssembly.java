package com.exoticworlds.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePlacement;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.world.level.ChunkPos;

import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.region.ExportMapTileChunk;

public final class XaeroExportAssembly {
    private long @Nullable [] regions;
    private List<WorldCopies.Copy> copies = List.of();
    private int frameMinTileX;
    private int frameMinTileZ;
    private int frameMinX;
    private int frameMinZ;
    private int frameMaxX;
    private int frameMaxZ;
    private int regionX;
    private int regionZ;
    private @Nullable ExportMapTileChunk placedChunk;
    private List<TilePlacement> placements = List.of();

    public void beginFrame(int minTileChunkX, int minTileChunkZ, int maxTileChunkX, int maxTileChunkZ) {
        this.regions = XaeroWorldMapFold.frameRegions(minTileChunkX, minTileChunkZ, maxTileChunkX, maxTileChunkZ);
        this.frameMinTileX = minTileChunkX;
        this.frameMinTileZ = minTileChunkZ;
        this.frameMinX = minTileChunkX * XaeroWorldMapFold.SLOT_BLOCKS;
        this.frameMinZ = minTileChunkZ * XaeroWorldMapFold.SLOT_BLOCKS;
        this.frameMaxX = (maxTileChunkX + 1) * XaeroWorldMapFold.SLOT_BLOCKS;
        this.frameMaxZ = (maxTileChunkZ + 1) * XaeroWorldMapFold.SLOT_BLOCKS;
        this.copies = this.regions == null ? List.of() : XaeroWorldMapFold.drawnCopies(
                new int[] {this.frameMinX, this.frameMaxX}, new int[] {this.frameMinZ, this.frameMaxZ},
                MapCopies.REPEATED);
        this.placedChunk = null;
    }

    public boolean walking() {
        return this.regions != null;
    }

    public int lastWalkedRegion() {
        return this.regions.length - 1;
    }

    public void enterRegion(int regionX, int regionZ) {
        if (this.regions == null) {
            this.regionX = regionX;
            this.regionZ = regionZ;
            return;
        }

        long region = this.regions[regionZ];
        this.regionX = ChunkPos.getX(region);
        this.regionZ = ChunkPos.getZ(region);
    }

    public int regionX() {
        return this.regionX;
    }

    public int regionZ() {
        return this.regionZ;
    }

    public int tileChunkX(ExportMapTileChunk chunk) {
        return placed(chunk) ? this.frameMinTileX : this.frameMinTileX - 1;
    }

    public int tileChunkZ(ExportMapTileChunk chunk) {
        return placed(chunk) ? this.frameMinTileZ : this.frameMinTileZ - 1;
    }

    private boolean placed(ExportMapTileChunk chunk) {
        if (chunk != this.placedChunk) {
            this.placedChunk = chunk;
            List<TilePlacement> placements = new ArrayList<>();
            for (TilePlacement placement : XaeroWorldMapFold.placements(chunk.getX() * XaeroWorldMapFold.SLOT_BLOCKS,
                    chunk.getZ() * XaeroWorldMapFold.SLOT_BLOCKS, XaeroWorldMapFold.SLOT_BLOCKS, this.copies)) {
                if (placement.meets(this.frameMinX, this.frameMinZ, this.frameMaxX, this.frameMaxZ)) {
                    placements.add(placement);
                }
            }

            this.placements = placements;
        }

        return !this.placements.isEmpty();
    }

    public void draw(Matrix4f matrix, float x, float y, GpuTextureView texture, boolean hasLight,
            MultiTextureRenderTypeRenderer renderer) {
        for (TilePlacement placement : this.placements) {
            GuiMap.renderTexturedModalSubRectWithLighting(matrix, x + (placement.minX() - this.frameMinX),
                    y + (placement.minZ() - this.frameMinZ), placement.u1(), placement.v1(), placement.u2(),
                    placement.v2(), placement.width(), placement.height(), texture, hasLight, renderer);
        }
    }
}
