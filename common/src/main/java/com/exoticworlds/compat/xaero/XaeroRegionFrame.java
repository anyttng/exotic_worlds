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
    private static final int NO_LEVEL = -1;

    private @Nullable ToroidalShape shape;
    private MapCopies mapCopies = MapCopies.REPEATED;
    private AxisCopies copiesX = AxisCopies.UNBOUNDED;
    private AxisCopies copiesZ = AxisCopies.UNBOUNDED;
    private int glueSlotSize = NO_SLOT_SIZE;
    private boolean glueable;
    private int windowLevel = NO_LEVEL;
    private WorldCopies.@Nullable Window window;
    private int drawnCopiesLevel = NO_LEVEL;
    private List<WorldCopies.Copy> drawnCopies = List.of();
    private final LongOpenHashSet drawnCanonicalSlots = new LongOpenHashSet();
    private int viewCaveLayer;
    private int viewLeveledRegX;
    private int viewLeveledRegZ;
    private int viewLevel;
    private double viewCameraX;
    private double viewCameraZ;
    private double viewScale;
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
        this.windowLevel = NO_LEVEL;
        this.drawnCopiesLevel = NO_LEVEL;
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

    public List<WorldCopies.Copy> drawnCopies() {
        if (this.viewLevel != this.drawnCopiesLevel) {
            Window screen = Minecraft.getInstance().getWindow();
            int[] spanX = drawnSpan(this.viewCameraX, screen.getWidth());
            int[] spanZ = drawnSpan(this.viewCameraZ, screen.getHeight());
            this.drawnCopiesLevel = this.viewLevel;
            this.drawnCopies = XaeroWorldMapFold.drawnCopies(window().meeting(spanX[0], spanZ[0], spanX[1], spanZ[1]),
                    this.mapCopies);
        }

        return this.drawnCopies;
    }

    public WorldCopies.Window window() {
        WorldCopies.Window resolved = this.window;
        if (resolved == null || this.viewLevel != this.windowLevel) {
            Window screen = Minecraft.getInstance().getWindow();
            int[] drawnX = drawnSpan(this.viewCameraX, screen.getWidth());
            int[] drawnZ = drawnSpan(this.viewCameraZ, screen.getHeight());
            int[] regionsX = regionSpan(this.viewCameraX, screen.getWidth());
            int[] regionsZ = regionSpan(this.viewCameraZ, screen.getHeight());
            resolved = WorldCopies.Window.of(this.shape, Math.min(drawnX[0], regionsX[0]),
                    Math.min(drawnZ[0], regionsZ[0]), Math.max(drawnX[1], regionsX[1]),
                    Math.max(drawnZ[1], regionsZ[1]));
            this.window = resolved;
            this.windowLevel = this.viewLevel;
        }

        return resolved;
    }

    private int[] drawnSpan(double camera, int windowPixels) {
        return MapViewSpan.of(camera, windowPixels, this.viewScale, slotSize());
    }

    private int[] regionSpan(double camera, int windowPixels) {
        int side = XaeroWorldMapFold.REGION_BLOCKS << this.viewLevel;
        double low = camera - Math.floorDiv(windowPixels, 2) / this.viewScale;
        double high = low + windowPixels / this.viewScale;
        return new int[] {Math.floorDiv((int) Math.floor(low), side) * side,
                (Math.floorDiv((int) Math.floor(high), side) + 1) * side};
    }

    public long[] fannedRegions(int leveledRegX, int leveledRegZ) {
        return XaeroWorldMapFold.fannedRegions(window(), leveledRegX, leveledRegZ, this.viewLevel);
    }

    public long[] fannedLeaves(int leveledRegX, int leveledRegZ) {
        return XaeroWorldMapFold.fannedLeaves(window(), leveledRegX, leveledRegZ, this.viewLevel);
    }

    public boolean drawnOnce(long canonicalSlotOrigin) {
        return this.drawnCanonicalSlots.add(canonicalSlotOrigin);
    }

    public void beginView(int caveLayer, int leveledRegX, int leveledRegZ, int level, double cameraX, double cameraZ,
            double scale) {
        this.viewCaveLayer = caveLayer;
        this.viewLeveledRegX = leveledRegX;
        this.viewLeveledRegZ = leveledRegZ;
        this.viewLevel = level;
        this.viewCameraX = cameraX;
        this.viewCameraZ = cameraZ;
        this.viewScale = scale;
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
            origins = XaeroWorldMapFold.canonicalSlotOrigins(window(), this.slotViewBlockX, this.slotViewBlockZ,
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
