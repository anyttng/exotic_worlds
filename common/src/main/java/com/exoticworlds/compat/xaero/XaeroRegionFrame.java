package com.exoticworlds.compat.xaero;

import java.util.List;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapViewSpan;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePlacement;
import com.mojang.blaze3d.platform.Window;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import xaero.map.MapProcessor;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

public final class XaeroRegionFrame {
    private static final int NO_SLOT_SIZE = 0;
    private static final int NO_LEVEL = -1;
    private static final int NO_TEXTURE = -1;

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
    private XaeroRegionKey viewRegion = new XaeroRegionKey(0, 0, 0, 0);
    private XaeroCamera viewCamera = new XaeroCamera(0.0, 0.0, 0.0);
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

    private boolean glueableAt(int slotSize) {
        if (slotSize != this.glueSlotSize) {
            this.glueSlotSize = slotSize;
            this.glueable = XaeroWorldMapFold.glueableAt(this.shape, slotSize);
        }

        return this.glueable;
    }

    private List<WorldCopies.Copy> drawnCopies() {
        if (this.viewRegion.level() != this.drawnCopiesLevel) {
            Window screen = Minecraft.getInstance().getWindow();
            int[] spanX = drawnSpan(this.viewCamera.x(), screen.getWidth());
            int[] spanZ = drawnSpan(this.viewCamera.z(), screen.getHeight());
            this.drawnCopiesLevel = this.viewRegion.level();
            this.drawnCopies = XaeroWorldMapFold.drawnCopies(
                    window().within(spanX[0], spanZ[0], spanX[1], spanZ[1]).meeting(), this.mapCopies);
        }

        return this.drawnCopies;
    }

    private WorldCopies.Window window() {
        WorldCopies.Window resolved = this.window;
        if (resolved == null || this.viewRegion.level() != this.windowLevel) {
            Window screen = Minecraft.getInstance().getWindow();
            int[] drawnX = drawnSpan(this.viewCamera.x(), screen.getWidth());
            int[] drawnZ = drawnSpan(this.viewCamera.z(), screen.getHeight());
            int[] regionsX = regionSpan(this.viewCamera.x(), screen.getWidth());
            int[] regionsZ = regionSpan(this.viewCamera.z(), screen.getHeight());
            resolved = WorldCopies.Window.of(this.shape, Math.min(drawnX[0], regionsX[0]),
                    Math.min(drawnZ[0], regionsZ[0]), Math.max(drawnX[1], regionsX[1]),
                    Math.max(drawnZ[1], regionsZ[1]));
            this.window = resolved;
            this.windowLevel = this.viewRegion.level();
        }

        return resolved;
    }

    private int[] drawnSpan(double camera, int windowPixels) {
        return MapViewSpan.of(camera, windowPixels, this.viewCamera.scale(), slotSize());
    }

    private int[] regionSpan(double camera, int windowPixels) {
        int side = XaeroWorldMapFold.REGION_BLOCKS << this.viewRegion.level();
        double low = camera - Math.floorDiv(windowPixels, 2) / this.viewCamera.scale();
        double high = low + windowPixels / this.viewCamera.scale();
        return new int[] {Math.floorDiv((int) Math.floor(low), side) * side,
                (Math.floorDiv((int) Math.floor(high), side) + 1) * side};
    }

    public long[] fannedRegions(int leveledRegX, int leveledRegZ) {
        return XaeroWorldMapFold.fannedRegions(window(), leveledRegX, leveledRegZ, this.viewRegion.level());
    }

    public long[] fannedLeaves(int leveledRegX, int leveledRegZ) {
        return XaeroWorldMapFold.fannedLeaves(window(), leveledRegX, leveledRegZ, this.viewRegion.level());
    }

    private boolean drawnOnce(long canonicalSlotOrigin) {
        return this.drawnCanonicalSlots.add(canonicalSlotOrigin);
    }

    public void beginView(XaeroRegionKey region, XaeroCamera camera) {
        this.viewRegion = region;
        this.viewCamera = camera;
        this.leveledCandidate = null;
    }

    public int viewCaveLayer() {
        return this.viewRegion.caveLayer();
    }

    public int viewLevel() {
        return this.viewRegion.level();
    }

    private int slotSize() {
        return XaeroWorldMapFold.SLOT_BLOCKS << this.viewRegion.level();
    }

    public void candidate(@Nullable LeveledRegion<?> candidate) {
        this.leveledCandidate = candidate;
    }

    private boolean isCandidate(@Nullable LeveledRegion<?> region) {
        return this.shape != null && region != null && region == this.leveledCandidate;
    }

    private void beginSlot(int slotX, int slotZ) {
        int side = XaeroWorldMapFold.REGION_BLOCKS << this.viewRegion.level();
        int slotSize = slotSize();
        this.slotViewBlockX = this.viewRegion.regX() * side + slotX * slotSize;
        this.slotViewBlockZ = this.viewRegion.regZ() * side + slotZ * slotSize;
        this.slotOrigins = null;
        this.slotFolded = false;
    }

    private boolean slotInsideWorld(int slotSize) {
        return !XaeroWorldMapFold.spanLeavesWorld(this.copiesX, this.slotViewBlockX, slotSize)
                && !XaeroWorldMapFold.spanLeavesWorld(this.copiesZ, this.slotViewBlockZ, slotSize);
    }

    private long[] slotOrigins(int slotSize) {
        long[] origins = this.slotOrigins;
        if (origins == null) {
            origins = XaeroWorldMapFold.canonicalSlotOrigins(window(), this.slotViewBlockX, this.slotViewBlockZ,
                    slotSize);
            this.slotOrigins = origins;
        }

        return origins;
    }

    public @Nullable RegionTexture<?> hoverTexture(MapProcessor processor, @Nullable RegionTexture<?> texture,
            int mouseBlockX, int mouseBlockZ) {
        return this.shape == null ? texture : canonicalRegionTexture(processor, mouseBlockX, mouseBlockZ);
    }

    public @Nullable RegionTexture<?> slotTexture(MapProcessor processor, @Nullable RegionTexture<?> texture,
            @Nullable LeveledRegion<?> region, int slotX, int slotZ) {
        boolean isCandidate = isCandidate(region);
        beginSlot(slotX, slotZ);
        int slotSize = slotSize();
        boolean shaped = this.shape != null;
        XaeroSlotSource source = XaeroSlotSource.of(this.shape, this.mapCopies, isCandidate,
                shaped && glueableAt(slotSize), shaped && slotInsideWorld(slotSize), this.slotViewBlockX,
                this.slotViewBlockZ);
        this.slotFolded = source.folded();
        return switch (source.kind()) {
            case RAW -> texture;
            case HIDDEN -> null;
            case ANY_CANONICAL -> anyCanonicalTexture(processor);
            case CANONICAL -> canonicalRegionTexture(processor, source.blockX(), source.blockZ());
        };
    }

    public @Nullable RegionTexture<?> rootTexture(@Nullable RegionTexture<?> texture) {
        return this.shape != null && this.slotFolded ? null : texture;
    }

    public int drawPeriodCopies(MapProcessor processor, int texture,
            Matrix4f matrix, int textureX, int textureZ, int flooredCameraX, int flooredCameraZ,
            RegionTexture<?> regionTexture, MultiTextureRenderTypeRenderer withLightRenderer,
            MultiTextureRenderTypeRenderer noLightRenderer) {
        ToroidalShape shape = this.shape;
        int slotSize = slotSize();
        if (texture == NO_TEXTURE || shape == null || glueableAt(slotSize)) {
            return texture;
        }

        float x = textureX - flooredCameraX;
        float y = textureZ - flooredCameraZ;
        boolean hasLight = regionTexture.getTextureHasLight();
        MultiTextureRenderTypeRenderer renderer = hasLight ? withLightRenderer : noLightRenderer;
        List<WorldCopies.Copy> copies = drawnCopies();
        for (long origin : slotOrigins(slotSize)) {
            if (!drawnOnce(origin)) {
                continue;
            }

            int originX = ChunkPos.getX(origin);
            int originZ = ChunkPos.getZ(origin);
            RegionTexture<?> canonical = canonicalRegionTexture(processor, originX, originZ);
            int canonicalTexture = canonical == null ? NO_TEXTURE : canonical.getGlColorTexture();
            if (canonicalTexture == NO_TEXTURE) {
                continue;
            }

            for (TilePlacement placement : XaeroWorldMapFold.placements(shape, originX, originZ, slotSize, copies)) {
                GuiMap.renderTexturedModalSubRectWithLighting(matrix, x + (placement.minX() - this.slotViewBlockX),
                        y + (placement.minZ() - this.slotViewBlockZ), placement.u1(), placement.v1(), placement.u2(),
                        placement.v2(), placement.width(), placement.height(), canonicalTexture, hasLight,
                        renderer);
            }
        }

        return NO_TEXTURE;
    }

    private @Nullable RegionTexture<?> anyCanonicalTexture(MapProcessor processor) {
        for (long origin : slotOrigins(slotSize())) {
            RegionTexture<?> texture = canonicalRegionTexture(processor, ChunkPos.getX(origin), ChunkPos.getZ(origin));
            if (texture != null) {
                return texture;
            }
        }

        return null;
    }

    private @Nullable RegionTexture<?> canonicalRegionTexture(MapProcessor processor, int canonicalBlockX,
            int canonicalBlockZ) {
        int side = XaeroWorldMapFold.REGION_BLOCKS << this.viewRegion.level();
        int canonicalRegX = Math.floorDiv(canonicalBlockX, side);
        int canonicalRegZ = Math.floorDiv(canonicalBlockZ, side);
        LeveledRegion<?> canonical = processor.getLeveledRegion(this.viewRegion.caveLayer(), canonicalRegX,
                canonicalRegZ, this.viewRegion.level());
        if (canonical == null || !canonical.hasTextures()) {
            return null;
        }

        int slotSize = slotSize();
        return canonical.getTexture((canonicalBlockX - canonicalRegX * side) / slotSize,
                (canonicalBlockZ - canonicalRegZ * side) / slotSize);
    }
}
