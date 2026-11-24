package com.exoticworlds.compat.xaero;

import java.util.List;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.map.AxisCopies;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.map.MapViewSpan;
import com.exoticworlds.compat.map.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePlacement;
import com.exoticworlds.core.fold.WorldFold;
import com.mojang.blaze3d.platform.Window;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import xaero.lib.client.graphics.GpuTextureAndView;
import xaero.map.MapProcessor;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

public final class XaeroRegionFrame {
    private static final int NO_SLOT_SIZE = 0;
    private static final int NO_LEVEL = -1;

    private @Nullable WorldFold fold;
    private MapCopies mapCopies = MapCopies.REPEATED;
    private AxisCopies copiesX = AxisCopies.UNBOUNDED;
    private AxisCopies copiesZ = AxisCopies.UNBOUNDED;
    private int glueSlotSize = NO_SLOT_SIZE;
    private boolean isGlueable;
    private int windowLevel = NO_LEVEL;
    private WorldCopies.@Nullable Window window;
    private int drawnCopiesLevel = NO_LEVEL;
    private List<WorldCopies.Copy> drawnCopies = List.of();
    private final LongOpenHashSet drawnCanonicalSlots = new LongOpenHashSet();
    private XaeroRegionKey viewRegion = new XaeroRegionKey(0, 0, 0, 0);
    private XaeroCamera viewCamera = new XaeroCamera(0.0, 0.0, 0.0);
    private @Nullable LeveledRegion<?> leveledCandidate;
    private boolean isSlotFolded;
    private int slotViewBlockX;
    private int slotViewBlockZ;
    private long @Nullable [] slotOrigins;

    public void begin() {
        this.fold = XaeroWorldMapFold.getBrowsedFold();
        this.mapCopies = MapCopies.getCurrent();
        this.copiesX = AxisCopies.of(this.fold, Direction.Axis.X);
        this.copiesZ = AxisCopies.of(this.fold, Direction.Axis.Z);
        this.glueSlotSize = NO_SLOT_SIZE;
        this.windowLevel = NO_LEVEL;
        this.drawnCopiesLevel = NO_LEVEL;
        this.drawnCanonicalSlots.clear();
    }

    public @Nullable WorldFold getFold() {
        return this.fold;
    }

    private boolean isGlueableAt(int slotSizeBlocks) {
        if (slotSizeBlocks != this.glueSlotSize) {
            this.glueSlotSize = slotSizeBlocks;
            this.isGlueable = XaeroWorldMapFold.isGlueableAt(this.fold, slotSizeBlocks);
        }

        return this.isGlueable;
    }

    private List<WorldCopies.Copy> getDrawnCopies() {
        if (this.viewRegion.level() != this.drawnCopiesLevel) {
            Window screen = Minecraft.getInstance().getWindow();
            int[] spanX = drawnSpanBlocks(this.viewCamera.x(), screen.getWidth());
            int[] spanZ = drawnSpanBlocks(this.viewCamera.z(), screen.getHeight());
            this.drawnCopiesLevel = this.viewRegion.level();
            this.drawnCopies = XaeroWorldMapFold.drawnCopies(
                    getWindow().within(spanX[0], spanZ[0], spanX[1], spanZ[1]).getMeetingCopies(), this.mapCopies);
        }

        return this.drawnCopies;
    }

    private WorldCopies.Window getWindow() {
        WorldCopies.Window resolved = this.window;
        if (resolved == null || this.viewRegion.level() != this.windowLevel) {
            Window screen = Minecraft.getInstance().getWindow();
            int[] drawnX = drawnSpanBlocks(this.viewCamera.x(), screen.getWidth());
            int[] drawnZ = drawnSpanBlocks(this.viewCamera.z(), screen.getHeight());
            int[] regionsX = regionSpanBlocks(this.viewCamera.x(), screen.getWidth());
            int[] regionsZ = regionSpanBlocks(this.viewCamera.z(), screen.getHeight());
            resolved = WorldCopies.Window.of(this.fold, Math.min(drawnX[0], regionsX[0]),
                    Math.min(drawnZ[0], regionsZ[0]), Math.max(drawnX[1], regionsX[1]),
                    Math.max(drawnZ[1], regionsZ[1]));
            this.window = resolved;
            this.windowLevel = this.viewRegion.level();
        }

        return resolved;
    }

    private int[] drawnSpanBlocks(double camera, int windowPixels) {
        return MapViewSpan.of(camera, windowPixels, this.viewCamera.scale(), getSlotSizeBlocks());
    }

    private int[] regionSpanBlocks(double camera, int windowPixels) {
        int sideBlocks = XaeroWorldMapFold.REGION_BLOCKS << this.viewRegion.level();
        double low = camera - Math.floorDiv(windowPixels, 2) / this.viewCamera.scale();
        double high = low + windowPixels / this.viewCamera.scale();
        return new int[] {Math.floorDiv((int) Math.floor(low), sideBlocks) * sideBlocks,
                (Math.floorDiv((int) Math.floor(high), sideBlocks) + 1) * sideBlocks};
    }

    public long[] fannedRegions(int leveledRegX, int leveledRegZ) {
        return XaeroWorldMapFold.fannedRegions(getWindow(), leveledRegX, leveledRegZ, this.viewRegion.level());
    }

    public long[] fannedLeaves(int leveledRegX, int leveledRegZ) {
        return XaeroWorldMapFold.fannedLeaves(getWindow(), leveledRegX, leveledRegZ, this.viewRegion.level());
    }

    private boolean tryMarkDrawn(long canonicalSlotOrigin) {
        return this.drawnCanonicalSlots.add(canonicalSlotOrigin);
    }

    public void beginView(XaeroRegionKey region, XaeroCamera camera) {
        this.viewRegion = region;
        this.viewCamera = camera;
        this.leveledCandidate = null;
    }

    public int getViewCaveLayer() {
        return this.viewRegion.caveLayer();
    }

    public int getViewLevel() {
        return this.viewRegion.level();
    }

    private int getSlotSizeBlocks() {
        return XaeroWorldMapFold.SLOT_BLOCKS << this.viewRegion.level();
    }

    public void candidate(@Nullable LeveledRegion<?> candidate) {
        this.leveledCandidate = candidate;
    }

    private boolean isCandidate(@Nullable LeveledRegion<?> region) {
        return this.fold != null && region != null && region == this.leveledCandidate;
    }

    private void beginSlot(int slotX, int slotZ) {
        int sideBlocks = XaeroWorldMapFold.REGION_BLOCKS << this.viewRegion.level();
        int slotSizeBlocks = getSlotSizeBlocks();
        this.slotViewBlockX = this.viewRegion.regX() * sideBlocks + slotX * slotSizeBlocks;
        this.slotViewBlockZ = this.viewRegion.regZ() * sideBlocks + slotZ * slotSizeBlocks;
        this.slotOrigins = null;
        this.isSlotFolded = false;
    }

    private boolean isSlotInsideWorld(int slotSizeBlocks) {
        return !XaeroWorldMapFold.isSpanLeavingWorld(this.copiesX, this.slotViewBlockX, slotSizeBlocks)
                && !XaeroWorldMapFold.isSpanLeavingWorld(this.copiesZ, this.slotViewBlockZ, slotSizeBlocks);
    }

    private long[] getSlotOrigins(int slotSizeBlocks) {
        long[] origins = this.slotOrigins;
        if (origins == null) {
            origins = XaeroWorldMapFold.canonicalSlotOrigins(getWindow(), this.slotViewBlockX, this.slotViewBlockZ,
                    slotSizeBlocks);
            this.slotOrigins = origins;
        }

        return origins;
    }

    public @Nullable RegionTexture<?> getHoverTexture(MapProcessor processor, @Nullable RegionTexture<?> texture,
            int mouseBlockX, int mouseBlockZ) {
        return this.fold == null ? texture : getCanonicalRegionTexture(processor, mouseBlockX, mouseBlockZ);
    }

    public @Nullable RegionTexture<?> slotTexture(MapProcessor processor, @Nullable RegionTexture<?> texture,
            @Nullable LeveledRegion<?> region, int slotX, int slotZ) {
        boolean isCandidate = isCandidate(region);
        beginSlot(slotX, slotZ);
        int slotSizeBlocks = getSlotSizeBlocks();
        boolean isShaped = this.fold != null;
        XaeroSlotSource source = XaeroSlotSource.of(this.fold, this.mapCopies, isCandidate,
                isShaped && isGlueableAt(slotSizeBlocks), isShaped && isSlotInsideWorld(slotSizeBlocks), this.slotViewBlockX,
                this.slotViewBlockZ);
        this.isSlotFolded = source.isFolded();
        return switch (source.kind()) {
            case RAW -> texture;
            case HIDDEN -> null;
            case ANY_CANONICAL -> anyCanonicalTexture(processor);
            case CANONICAL -> getCanonicalRegionTexture(processor, source.blockX(), source.blockZ());
        };
    }

    public @Nullable RegionTexture<?> rootTexture(@Nullable RegionTexture<?> texture) {
        return this.fold != null && this.isSlotFolded ? null : texture;
    }

    public @Nullable GpuTextureAndView drawPeriodCopies(MapProcessor processor, @Nullable GpuTextureAndView texture,
            Matrix4f matrix, int textureX, int textureZ, int flooredCameraX, int flooredCameraZ,
            RegionTexture<?> regionTexture, MultiTextureRenderTypeRenderer withLightRenderer,
            MultiTextureRenderTypeRenderer noLightRenderer) {
        WorldFold fold = this.fold;
        int slotSizeBlocks = getSlotSizeBlocks();
        if (texture == null || fold == null || isGlueableAt(slotSizeBlocks)) {
            return texture;
        }

        float x = textureX - flooredCameraX;
        float y = textureZ - flooredCameraZ;
        boolean hasLight = regionTexture.getTextureHasLight();
        MultiTextureRenderTypeRenderer renderer = hasLight ? withLightRenderer : noLightRenderer;
        List<WorldCopies.Copy> copies = getDrawnCopies();
        for (long origin : getSlotOrigins(slotSizeBlocks)) {
            if (!tryMarkDrawn(origin)) {
                continue;
            }

            int originX = ChunkPos.getX(origin);
            int originZ = ChunkPos.getZ(origin);
            RegionTexture<?> canonical = getCanonicalRegionTexture(processor, originX, originZ);
            GpuTextureAndView textureAndView = canonical == null ? null : canonical.getGlColorTexture();
            if (textureAndView == null) {
                continue;
            }

            for (TilePlacement placement : XaeroWorldMapFold.placements(fold, originX, originZ, slotSizeBlocks, copies)) {
                GuiMap.renderTexturedModalSubRectWithLighting(matrix, x + (placement.minX() - this.slotViewBlockX),
                        y + (placement.minZ() - this.slotViewBlockZ), placement.u1(), placement.v1(), placement.u2(),
                        placement.v2(), placement.widthBlocks(), placement.heightBlocks(), textureAndView.view, hasLight,
                        renderer);
            }
        }

        return null;
    }

    private @Nullable RegionTexture<?> anyCanonicalTexture(MapProcessor processor) {
        for (long origin : getSlotOrigins(getSlotSizeBlocks())) {
            RegionTexture<?> texture = getCanonicalRegionTexture(processor, ChunkPos.getX(origin), ChunkPos.getZ(origin));
            if (texture != null) {
                return texture;
            }
        }

        return null;
    }

    private @Nullable RegionTexture<?> getCanonicalRegionTexture(MapProcessor processor, int canonicalBlockX,
            int canonicalBlockZ) {
        int sideBlocks = XaeroWorldMapFold.REGION_BLOCKS << this.viewRegion.level();
        int canonicalRegX = Math.floorDiv(canonicalBlockX, sideBlocks);
        int canonicalRegZ = Math.floorDiv(canonicalBlockZ, sideBlocks);
        LeveledRegion<?> canonical = processor.getLeveledRegion(this.viewRegion.caveLayer(), canonicalRegX,
                canonicalRegZ, this.viewRegion.level());
        if (canonical == null || !canonical.hasTextures()) {
            return null;
        }

        int slotSizeBlocks = getSlotSizeBlocks();
        return canonical.getTexture((canonicalBlockX - canonicalRegX * sideBlocks) / slotSizeBlocks,
                (canonicalBlockZ - canonicalRegZ * sideBlocks) / slotSizeBlocks);
    }
}
