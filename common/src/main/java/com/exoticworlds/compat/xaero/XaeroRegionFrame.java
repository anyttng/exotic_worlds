package com.exoticworlds.compat.xaero;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapViewSpan;
import com.exoticworlds.compat.WorldCopies;
import com.mojang.blaze3d.platform.Window;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import xaero.map.MapProcessor;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

public final class XaeroRegionFrame {
    private static final int NO_SLOT_SIZE = 0;

    private @Nullable ToroidalShape shape;
    private MapCopies mapCopies = MapCopies.REPEATED;
    private AxisCopies copiesX = AxisCopies.UNBOUNDED;
    private AxisCopies copiesZ = AxisCopies.UNBOUNDED;
    private int glueSlotSize = NO_SLOT_SIZE;
    private boolean glueable;
    private int drawnCopiesSlotSize = NO_SLOT_SIZE;
    private List<WorldCopies.Copy> drawnCopies = List.of();
    private final LongOpenHashSet drawnCanonicalSlots = new LongOpenHashSet();
    private int viewCaveLayer;
    private int viewLeveledRegX;
    private int viewLeveledRegZ;
    private int viewLevel;
    private @Nullable LeveledRegion<?> leveledCandidate;
    private boolean slotFolded;
    private int slotViewBlockX;
    private int slotViewBlockZ;
    private long @Nullable [] slotOrigins;

    public void begin() {
        this.shape = XaeroWorldMapFold.browsedShape();
        this.mapCopies = MapCopies.current();
        this.copiesX = AxisCopies.of(this.shape, Direction.Axis.X);
        this.copiesZ = AxisCopies.of(this.shape, Direction.Axis.Z);
        this.glueSlotSize = NO_SLOT_SIZE;
        this.drawnCopiesSlotSize = NO_SLOT_SIZE;
        this.drawnCanonicalSlots.clear();
    }

    public @Nullable ToroidalShape shape() {
        return this.shape;
    }

    public MapCopies mapCopies() {
        return this.mapCopies;
    }

    public boolean glueableAt(int slotSize) {
        if (slotSize != this.glueSlotSize) {
            this.glueSlotSize = slotSize;
            this.glueable = XaeroWorldMapFold.glueableAt(this.shape, slotSize);
        }

        return this.glueable;
    }

    public List<WorldCopies.Copy> drawnCopies(int slotSize, double cameraX, double cameraZ, double scale) {
        if (slotSize != this.drawnCopiesSlotSize) {
            Window window = Minecraft.getInstance().getWindow();
            int[] spanX = MapViewSpan.of(cameraX, window.getWidth(), scale, slotSize);
            int[] spanZ = MapViewSpan.of(cameraZ, window.getHeight(), scale, slotSize);
            this.drawnCopiesSlotSize = slotSize;
            this.drawnCopies = XaeroWorldMapFold.drawnCopies(this.shape, spanX, spanZ, this.mapCopies);
        }

        return this.drawnCopies;
    }

    public boolean drawnOnce(long canonicalSlotOrigin) {
        return this.drawnCanonicalSlots.add(canonicalSlotOrigin);
    }

    public void beginView(int caveLayer, int leveledRegX, int leveledRegZ, int level) {
        this.viewCaveLayer = caveLayer;
        this.viewLeveledRegX = leveledRegX;
        this.viewLeveledRegZ = leveledRegZ;
        this.viewLevel = level;
        this.leveledCandidate = null;
    }

    public int viewCaveLayer() {
        return this.viewCaveLayer;
    }

    public int viewLevel() {
        return this.viewLevel;
    }

    public int slotSize() {
        return XaeroWorldMapFold.SLOT_BLOCKS << this.viewLevel;
    }

    public void candidate(@Nullable LeveledRegion<?> candidate) {
        this.leveledCandidate = candidate;
    }

    public boolean isCandidate(@Nullable LeveledRegion<?> region) {
        return this.shape != null && region != null && region == this.leveledCandidate;
    }

    public boolean slotFolded() {
        return this.slotFolded;
    }

    public void slotFolded(boolean folded) {
        this.slotFolded = folded;
    }

    public void beginSlot(int slotX, int slotZ) {
        int side = XaeroWorldMapFold.REGION_BLOCKS << this.viewLevel;
        int slotSize = slotSize();
        this.slotViewBlockX = this.viewLeveledRegX * side + slotX * slotSize;
        this.slotViewBlockZ = this.viewLeveledRegZ * side + slotZ * slotSize;
        this.slotOrigins = null;
        this.slotFolded = false;
    }

    public int slotViewBlockX() {
        return this.slotViewBlockX;
    }

    public int slotViewBlockZ() {
        return this.slotViewBlockZ;
    }

    public boolean slotInsideWorld(int slotSize) {
        return !XaeroWorldMapFold.spanLeavesWorld(this.copiesX, this.slotViewBlockX, slotSize)
                && !XaeroWorldMapFold.spanLeavesWorld(this.copiesZ, this.slotViewBlockZ, slotSize);
    }

    public long[] slotOrigins(int slotSize) {
        long[] origins = this.slotOrigins;
        if (origins == null) {
            origins = XaeroWorldMapFold.canonicalSlotOrigins(this.shape, this.slotViewBlockX, this.slotViewBlockZ,
                    slotSize);
            this.slotOrigins = origins;
        }

        return origins;
    }

    public @Nullable RegionTexture<?> anyCanonicalTexture(MapProcessor processor) {
        for (long origin : slotOrigins(slotSize())) {
            RegionTexture<?> texture = canonicalRegionTexture(processor, ChunkPos.getX(origin), ChunkPos.getZ(origin));
            if (texture != null) {
                return texture;
            }
        }

        return null;
    }

    public @Nullable RegionTexture<?> canonicalRegionTexture(MapProcessor processor, int canonicalBlockX,
            int canonicalBlockZ) {
        int side = XaeroWorldMapFold.REGION_BLOCKS << this.viewLevel;
        int canonicalRegX = Math.floorDiv(canonicalBlockX, side);
        int canonicalRegZ = Math.floorDiv(canonicalBlockZ, side);
        LeveledRegion<?> canonical = processor.getLeveledRegion(this.viewCaveLayer, canonicalRegX, canonicalRegZ,
                this.viewLevel);
        if (canonical == null || !canonical.hasTextures()) {
            return null;
        }

        int slotSize = slotSize();
        return canonical.getTexture((canonicalBlockX - canonicalRegX * side) / slotSize,
                (canonicalBlockZ - canonicalRegZ * side) / slotSize);
    }
}
