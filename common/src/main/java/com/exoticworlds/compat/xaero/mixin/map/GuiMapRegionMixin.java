package com.exoticworlds.compat.xaero.mixin.map;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix4f;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import xaero.lib.client.graphics.GpuTextureAndView;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.misc.Misc;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.region.texture.RegionTexture;

@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapRegionMixin {
    @Shadow
    private int mouseBlockPosX;
    @Shadow
    private int mouseBlockPosZ;
    @Shadow
    private double scale;
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;
    @Shadow
    private ArrayList<MapRegion> regionBuffer;
    @Shadow
    private ArrayList<BranchLeveledRegion> branchRegionBuffer;
    @Shadow
    private boolean prevWaitingForBranchCache;
    @Shadow
    private boolean[] waitingForBranchCache;

    @Unique
    private static final int REQUEST_BUFFER_SIZE = 10;

    @Unique
    private static final String LEVELED_REGION_GET_TEXTURE =
            "Lxaero/map/region/LeveledRegion;getTexture(II)Lxaero/map/region/texture/RegionTexture;";

    @Unique
    private MapCopies toroidal$mapCopies = MapCopies.REPEATED;
    @Unique
    private MapProcessor toroidal$processor;
    @Unique
    private int toroidal$viewLeveledRegX;
    @Unique
    private int toroidal$viewLeveledRegZ;
    @Unique
    private int toroidal$viewLevel;
    @Unique
    private int toroidal$viewCaveLayer;
    @Unique
    private LeveledRegion<?> toroidal$leveledCandidate;
    @Unique
    private boolean toroidal$slotFolded;
    @Unique
    private int toroidal$slotViewBlockX;
    @Unique
    private int toroidal$slotViewBlockZ;
    @Unique
    private final LongOpenHashSet toroidal$drawnCanonicalSlots = new LongOpenHashSet();
    @Unique
    private final LongOpenHashSet toroidal$fannedRegions = new LongOpenHashSet();
    @Unique
    private final LongOpenHashSet toroidal$loopRegions = new LongOpenHashSet();

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void toroidal$beginRegionFrame(CallbackInfo ci) {
        this.toroidal$drawnCanonicalSlots.clear();
        this.toroidal$fannedRegions.clear();
        this.toroidal$loopRegions.clear();
        this.toroidal$mapCopies = MapCopies.current();
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getLeveledRegion(IIII)Lxaero/map/region/LeveledRegion;"))
    private @Nullable LeveledRegion<?> toroidal$fetchLeveledRegion(MapProcessor processor, int caveLayer, int regX, int regZ,
            int level, Operation<@Nullable LeveledRegion<?>> original) {
        this.toroidal$processor = processor;
        this.toroidal$viewLeveledRegX = regX;
        this.toroidal$viewLeveledRegZ = regZ;
        this.toroidal$viewLevel = level;
        this.toroidal$viewCaveLayer = caveLayer;
        this.toroidal$leveledCandidate = null;
        LeveledRegion<?> existing = original.call(processor, caveLayer, regX, regZ, level);
        this.toroidal$loopRegions.add(ChunkPos.pack(regX, regZ));
        if (!XaeroWorldMapFold.active()) {
            return existing;
        }

        int side = XaeroWorldMapFold.REGION_BLOCKS << level;
        toroidal$collectFannedRegions(regX, regZ, side);
        if (existing != null) {
            return existing;
        }

        // A candidate value only, so the draw block runs at all; the texture redirect re-resolves each slot precisely.
        BlockPos foldedOrigin = XaeroWorldMapFold.foldBlock(regX * side, regZ * side);
        int candidateX = Math.floorDiv(foldedOrigin.getX(), side);
        int candidateZ = Math.floorDiv(foldedOrigin.getZ(), side);
        LeveledRegion<?> candidate = original.call(processor, caveLayer, candidateX, candidateZ, level);
        if (candidate != null) {
            this.toroidal$loopRegions.add(ChunkPos.pack(candidateX, candidateZ));
        }

        this.toroidal$leveledCandidate = candidate;
        return candidate;
    }

    @Unique
    private void toroidal$collectFannedRegions(int regX, int regZ, int side) {
        AxisCopies copiesX = XaeroWorldMapFold.copies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.copies(Direction.Axis.Z);
        if (!XaeroWorldMapFold.spanLeavesWorld(copiesX, regX * side, side)
                && !XaeroWorldMapFold.spanLeavesWorld(copiesZ, regZ * side, side)) {
            return;
        }

        for (long origin : XaeroWorldMapFold.canonicalSlotOrigins(regX * side, regZ * side, side)) {
            this.toroidal$fannedRegions.add(ChunkPos.pack(
                    Math.floorDiv(ChunkPos.getX(origin), side), Math.floorDiv(ChunkPos.getZ(origin), side)));
        }
    }

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/file/MapSaveLoad;getNextToLoadByViewing()Lxaero/map/region/LeveledRegion;",
                    ordinal = 1))
    private void toroidal$maintainFannedRegions(CallbackInfo ci) {
        if (this.toroidal$fannedRegions.isEmpty()) {
            return;
        }

        LongIterator regions = this.toroidal$fannedRegions.iterator();
        while (regions.hasNext()) {
            long region = regions.nextLong();
            toroidal$maintainRegion(ChunkPos.getX(region), ChunkPos.getZ(region),
                    !this.toroidal$loopRegions.contains(region));
        }
    }

    @Unique
    private void toroidal$maintainRegion(int regX, int regZ, boolean outsideLoop) {
        MapProcessor processor = this.toroidal$processor;
        int caveLayer = this.toroidal$viewCaveLayer;
        int level = this.toroidal$viewLevel;
        int leaves = 1 << level;
        int minLeafX = regX * leaves;
        int minLeafZ = regZ * leaves;
        for (int leafX = minLeafX; leafX < minLeafX + leaves; leafX++) {
            for (int leafZ = minLeafZ; leafZ < minLeafZ + leaves; leafZ++) {
                MapRegion leaf = processor.getLeafMapRegion(caveLayer, leafX, leafZ, false);
                if (leaf == null) {
                    leaf = processor.getLeafMapRegion(caveLayer, leafX, leafZ, processor.regionExists(caveLayer, leafX, leafZ));
                }

                if (leaf != null && !this.prevWaitingForBranchCache) {
                    toroidal$queueLeafLoad(leaf, level);
                }
            }
        }

        LeveledRegion<?> region = processor.getLeveledRegion(caveLayer, regX, regZ, level);
        if (region == null || !outsideLoop || processor.isUploadingPaused() || WorldMap.pauseRequests) {
            return;
        }

        if (region instanceof BranchLeveledRegion branch) {
            branch.checkForUpdates(processor, this.prevWaitingForBranchCache, this.waitingForBranchCache,
                    this.branchRegionBuffer, level, minLeafX, minLeafZ, minLeafX + leaves - 1, minLeafZ + leaves - 1);
        }

        processor.getMapWorld().getCurrentDimension().getLayeredMapRegions().bumpLoadedRegion(region);
    }

    @Unique
    private void toroidal$queueLeafLoad(MapRegion leaf, int level) {
        synchronized (leaf) {
            if (leaf.canRequestReload_unsynced() && leaf.getLoadState() == 0
                    && (!leaf.isMetaLoaded() || level == 0 || leaf.loadingNeededForBranchLevel == level)
                    && !this.regionBuffer.contains(leaf)) {
                leaf.calculateSortingDistance();
                Misc.addToListOfSmallest(REQUEST_BUFFER_SIZE, this.regionBuffer, leaf);
            }
        }
    }

    // An origin-fold substitute, so the block runs even where the cell has no LEAF region of its own.
    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private xaero.map.region.@Nullable MapRegion toroidal$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX,
            int regZ, boolean create, Operation<xaero.map.region.@Nullable MapRegion> original) {
        xaero.map.region.MapRegion existing = original.call(processor, caveLayer, regX, regZ, create);
        if (existing != null || !XaeroWorldMapFold.active()) {
            return existing;
        }

        BlockPos foldedOrigin = XaeroWorldMapFold.foldBlock(regX * XaeroWorldMapFold.REGION_BLOCKS,
                regZ * XaeroWorldMapFold.REGION_BLOCKS);
        int foldedRegX = Math.floorDiv(foldedOrigin.getX(), XaeroWorldMapFold.REGION_BLOCKS);
        int foldedRegZ = Math.floorDiv(foldedOrigin.getZ(), XaeroWorldMapFold.REGION_BLOCKS);
        return original.call(processor, caveLayer, foldedRegX, foldedRegZ,
                processor.regionExists(caveLayer, foldedRegX, foldedRegZ));
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/LeveledRegion;hasTextures()Z"))
    private boolean toroidal$candidateHasTextures(LeveledRegion<?> region, Operation<Boolean> original) {
        if (XaeroWorldMapFold.active() && region != null && region == this.toroidal$leveledCandidate) {
            return true;
        }

        return original.call(region);
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = LEVELED_REGION_GET_TEXTURE,
                    ordinal = 0))
    private @Nullable RegionTexture<?> toroidal$foldHoverTexture(LeveledRegion<?> region, int textureX, int textureZ,
            Operation<RegionTexture<?>> original) {
        if (!XaeroWorldMapFold.active()) {
            return original.call(region, textureX, textureZ);
        }

        return toroidal$canonicalRegionTexture(this.mouseBlockPosX, this.mouseBlockPosZ);
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = LEVELED_REGION_GET_TEXTURE,
                    ordinal = 1))
    private @Nullable RegionTexture<?> toroidal$foldLeafTexture(LeveledRegion<?> region, int slotX, int slotZ,
            Operation<RegionTexture<?>> original) {
        this.toroidal$slotFolded = false;
        boolean isCandidate = region == this.toroidal$leveledCandidate;
        int level = this.toroidal$viewLevel;
        int slotSize = XaeroWorldMapFold.SLOT_BLOCKS << level;
        int side = XaeroWorldMapFold.REGION_BLOCKS << level;
        int viewBlockX = this.toroidal$viewLeveledRegX * side + slotX * slotSize;
        int viewBlockZ = this.toroidal$viewLeveledRegZ * side + slotZ * slotSize;
        this.toroidal$slotViewBlockX = viewBlockX;
        this.toroidal$slotViewBlockZ = viewBlockZ;
        if (!XaeroWorldMapFold.active()) {
            return isCandidate ? null : original.call(region, slotX, slotZ);
        }

        if (!XaeroWorldMapFold.glueableAt(slotSize)) {
            if (!isCandidate
                    && !XaeroWorldMapFold.spanLeavesWorld(XaeroWorldMapFold.copies(Direction.Axis.X), viewBlockX, slotSize)
                    && !XaeroWorldMapFold.spanLeavesWorld(XaeroWorldMapFold.copies(Direction.Axis.Z), viewBlockZ, slotSize)) {
                return original.call(region, slotX, slotZ);
            }

            this.toroidal$slotFolded = true;
            return toroidal$anyCanonicalTexture(viewBlockX, viewBlockZ, slotSize);
        }

        BlockPos foldedBlock = XaeroWorldMapFold.foldBlock(viewBlockX, viewBlockZ);
        if (foldedBlock.getX() == viewBlockX && foldedBlock.getZ() == viewBlockZ) {
            return isCandidate ? null : original.call(region, slotX, slotZ);
        }

        this.toroidal$slotFolded = true;
        return this.toroidal$mapCopies == MapCopies.SINGLE
                ? null : toroidal$canonicalRegionTexture(foldedBlock.getX(), foldedBlock.getZ());
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = LEVELED_REGION_GET_TEXTURE,
                    ordinal = 2))
    private @Nullable RegionTexture<?> toroidal$suppressFoldedRootTexture(LeveledRegion<?> region, int textureX, int textureZ,
            Operation<RegionTexture<?>> original) {
        if (XaeroWorldMapFold.active() && this.toroidal$slotFolded) {
            return null;
        }

        return original.call(region, textureX, textureZ);
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/gui/GuiMap;renderTexturedModalRectWithLighting3(Lorg/joml/Matrix4f;FFFFLcom/mojang/blaze3d/textures/GpuTextureView;ZLxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V"))
    private void toroidal$drawClippedPeriodCopies(
            Matrix4f matrix, float x, float y, float width, float height,
            GpuTextureView texture, boolean hasLight, MultiTextureRenderTypeRenderer renderer, Operation<Void> original) {
        int slotSize = XaeroWorldMapFold.SLOT_BLOCKS << this.toroidal$viewLevel;
        if (!XaeroWorldMapFold.active() || XaeroWorldMapFold.glueableAt(slotSize)) {
            original.call(matrix, x, y, width, height, texture, hasLight, renderer);
            return;
        }

        AxisCopies copiesX = XaeroWorldMapFold.copies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.copies(Direction.Axis.Z);
        Window window = Minecraft.getInstance().getWindow();
        int[] spanX = XaeroWorldMapFold.viewSpan(this.cameraX, window.getWidth(), this.scale, slotSize);
        int[] spanZ = XaeroWorldMapFold.viewSpan(this.cameraZ, window.getHeight(), this.scale, slotSize);
        List<WorldCopies.Copy> copies = XaeroWorldMapFold.drawnCopies(spanX, spanZ, this.toroidal$mapCopies);
        int viewBlockX = this.toroidal$slotViewBlockX;
        int viewBlockZ = this.toroidal$slotViewBlockZ;
        for (long origin : XaeroWorldMapFold.canonicalSlotOrigins(viewBlockX, viewBlockZ, slotSize)) {
            if (!this.toroidal$drawnCanonicalSlots.add(origin)) {
                continue;
            }

            int originX = ChunkPos.getX(origin);
            int originZ = ChunkPos.getZ(origin);
            RegionTexture<?> regionTexture = toroidal$canonicalRegionTexture(originX, originZ);
            GpuTextureAndView textureAndView = regionTexture == null ? null : regionTexture.getGlColorTexture();
            if (textureAndView == null) {
                continue;
            }

            int clippedMinX = copiesX.clipMin(originX);
            int clippedMaxX = copiesX.clipMax(originX + slotSize);
            int clippedMinZ = copiesZ.clipMin(originZ);
            int clippedMaxZ = copiesZ.clipMax(originZ + slotSize);
            if (clippedMinX >= clippedMaxX || clippedMinZ >= clippedMaxZ) {
                continue;
            }

            float clippedX = x + (clippedMinX - viewBlockX);
            float clippedY = y + (clippedMinZ - viewBlockZ);
            float clippedWidth = clippedMaxX - clippedMinX;
            float clippedHeight = clippedMaxZ - clippedMinZ;
            float u1 = (float) (clippedMinX - originX) / slotSize;
            float u2 = (float) (clippedMaxX - originX) / slotSize;
            float v1 = (float) (clippedMinZ - originZ) / slotSize;
            float v2 = (float) (clippedMaxZ - originZ) / slotSize;
            for (WorldCopies.Copy copy : copies) {
                float copyX = clippedX + copy.dx();
                float copyY = clippedY + copy.dz();
                BufferBuilder quad = renderer.begin(textureAndView.view);
                quad.addVertex(matrix, copyX, copyY + clippedHeight, 0.0F).setUv(u1, v2);
                quad.addVertex(matrix, copyX + clippedWidth, copyY + clippedHeight, 0.0F).setUv(u2, v2);
                quad.addVertex(matrix, copyX + clippedWidth, copyY, 0.0F).setUv(u2, v1);
                quad.addVertex(matrix, copyX, copyY, 0.0F).setUv(u1, v1);
            }
        }
    }

    @Unique
    private @Nullable RegionTexture<?> toroidal$anyCanonicalTexture(int viewBlockX, int viewBlockZ, int slotSize) {
        for (long origin : XaeroWorldMapFold.canonicalSlotOrigins(viewBlockX, viewBlockZ, slotSize)) {
            RegionTexture<?> regionTexture = toroidal$canonicalRegionTexture(ChunkPos.getX(origin), ChunkPos.getZ(origin));
            if (regionTexture != null) {
                return regionTexture;
            }
        }

        return null;
    }

    @Unique
    private @Nullable RegionTexture<?> toroidal$canonicalRegionTexture(int canonicalBlockX, int canonicalBlockZ) {
        int level = this.toroidal$viewLevel;
        int slotSize = XaeroWorldMapFold.SLOT_BLOCKS << level;
        int side = XaeroWorldMapFold.REGION_BLOCKS << level;
        int canonicalRegX = Math.floorDiv(canonicalBlockX, side);
        int canonicalRegZ = Math.floorDiv(canonicalBlockZ, side);
        LeveledRegion<?> canonical = this.toroidal$processor
                .getLeveledRegion(this.toroidal$viewCaveLayer, canonicalRegX, canonicalRegZ, level);
        if (canonical == null || !canonical.hasTextures()) {
            return null;
        }

        return canonical.getTexture((canonicalBlockX - canonicalRegX * side) / slotSize,
                (canonicalBlockZ - canonicalRegZ * side) / slotSize);
    }
}
