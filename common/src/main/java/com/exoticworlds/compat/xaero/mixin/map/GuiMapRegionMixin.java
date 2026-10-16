package com.exoticworlds.compat.xaero.mixin.map;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix4f;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroRegionFrame;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePlacement;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import xaero.lib.client.graphics.GpuTextureAndView;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.gui.GuiMap;
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
    private MapProcessor mapProcessor;
    @Shadow
    private ArrayList<BranchLeveledRegion> branchRegionBuffer;
    @Shadow
    private boolean prevWaitingForBranchCache;
    @Shadow
    private boolean[] waitingForBranchCache;

    @Unique
    private static final String LEVELED_REGION_GET_TEXTURE =
            "Lxaero/map/region/LeveledRegion;getTexture(II)Lxaero/map/region/texture/RegionTexture;";
    @Unique
    private static final String MAP_PROCESSOR_GET_LEVELED_REGION =
            "Lxaero/map/MapProcessor;getLeveledRegion(IIII)Lxaero/map/region/LeveledRegion;";
    @Unique
    private static final String EXPRESSION = "MIXINEXTRAS:EXPRESSION";
    @Unique
    private static final long[] NO_LEAVES = new long[0];
    @Unique
    private static final long NO_OWNER = Long.MIN_VALUE;
    @Unique
    private static final long NO_LEAF = Long.MIN_VALUE;
    @Unique
    private static final int NO_COLUMN = Integer.MIN_VALUE;
    @Unique
    private static final int REGION_SHIFT = 9;

    @Unique
    private final XaeroRegionFrame toroidal$frame = new XaeroRegionFrame();
    @Unique
    private final LongOpenHashSet toroidal$fannedRegions = new LongOpenHashSet();
    @Unique
    private final LongOpenHashSet toroidal$loopRegions = new LongOpenHashSet();
    @Unique
    private final LongOpenHashSet toroidal$fannedLeavesTaken = new LongOpenHashSet();
    @Unique
    private long toroidal$fannedLeavesOwner = NO_OWNER;
    @Unique
    private long[] toroidal$fannedLeaves = NO_LEAVES;
    @Unique
    private int toroidal$fannedLeafColumn = NO_COLUMN;
    @Unique
    private int toroidal$firstFannedLeafRow;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void toroidal$beginRegionFrame(CallbackInfo ci) {
        this.toroidal$fannedRegions.clear();
        this.toroidal$loopRegions.clear();
        this.toroidal$fannedLeavesTaken.clear();
        this.toroidal$fannedLeavesOwner = NO_OWNER;
        this.toroidal$frame.begin();
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 0))
    private @Nullable LeveledRegion<?> toroidal$fetchHoverLeveledRegion(@Nullable LeveledRegion<?> existing,
            @Local(name = "renderedCaveLayer") int caveLayer, @Local(name = "mouseRegX") int regX,
            @Local(name = "mouseRegZ") int regZ, @Local(name = "textureLevel") int level) {
        return toroidal$leveledOrFold(existing, caveLayer, regX, regZ, level);
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = {
                    @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)})
    private @Nullable LeveledRegion<?> toroidal$fetchViewLeveledRegion(@Nullable LeveledRegion<?> existing,
            @Local(name = "renderedCaveLayer") int caveLayer, @Local(name = "leveledRegX") int regX,
            @Local(name = "leveledRegZ") int regZ, @Local(name = "textureLevel") int level) {
        return toroidal$leveledOrFold(existing, caveLayer, regX, regZ, level);
    }

    @Unique
    private @Nullable LeveledRegion<?> toroidal$leveledOrFold(@Nullable LeveledRegion<?> existing, int caveLayer,
            int regX, int regZ, int level) {
        XaeroRegionFrame frame = this.toroidal$frame;
        frame.beginView(caveLayer, regX, regZ, level);
        this.toroidal$loopRegions.add(ChunkPos.pack(regX, regZ));
        ToroidalShape shape = frame.shape();
        if (shape == null) {
            return existing;
        }

        for (long fanned : XaeroWorldMapFold.fannedRegions(shape, regX, regZ, level)) {
            this.toroidal$fannedRegions.add(fanned);
        }

        if (existing != null) {
            return existing;
        }

        int side = XaeroWorldMapFold.REGION_BLOCKS << level;
        int minX = regX * side;
        int minZ = regZ * side;
        int maxX = minX + side - 1;
        int maxZ = minZ + side - 1;
        LeveledRegion<?> candidate = toroidal$texturedCanonical(shape, caveLayer, level, minX, minZ);
        if (candidate == null) {
            candidate = toroidal$texturedCanonical(shape, caveLayer, level, maxX, minZ);
        }

        if (candidate == null) {
            candidate = toroidal$texturedCanonical(shape, caveLayer, level, minX, maxZ);
        }

        if (candidate == null) {
            candidate = toroidal$texturedCanonical(shape, caveLayer, level, maxX, maxZ);
        }

        frame.candidate(candidate);
        return candidate;
    }

    @Unique
    private @Nullable LeveledRegion<?> toroidal$texturedCanonical(ToroidalShape shape, int caveLayer, int level,
            int blockX, int blockZ) {
        int side = XaeroWorldMapFold.REGION_BLOCKS << level;
        BlockPos folded = XaeroWorldMapFold.foldBlock(shape, blockX, blockZ);
        int canonicalX = Math.floorDiv(folded.getX(), side);
        int canonicalZ = Math.floorDiv(folded.getZ(), side);
        LeveledRegion<?> region = this.mapProcessor.getLeveledRegion(caveLayer, canonicalX, canonicalZ, level);
        if (region == null || !region.hasTextures()) {
            return null;
        }

        this.toroidal$loopRegions.add(ChunkPos.pack(canonicalX, canonicalZ));
        return region;
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
            if (!this.toroidal$loopRegions.contains(region)) {
                toroidal$keepRegion(ChunkPos.getX(region), ChunkPos.getZ(region));
            }
        }
    }

    @Unique
    private void toroidal$keepRegion(int regX, int regZ) {
        MapProcessor processor = this.mapProcessor;
        int level = this.toroidal$frame.viewLevel();
        LeveledRegion<?> region = processor.getLeveledRegion(this.toroidal$frame.viewCaveLayer(), regX, regZ, level);
        if (region == null || processor.isUploadingPaused() || WorldMap.pauseRequests) {
            return;
        }

        if (region instanceof BranchLeveledRegion branch) {
            int leaves = 1 << level;
            branch.checkForUpdates(processor, this.prevWaitingForBranchCache, this.waitingForBranchCache,
                    this.branchRegionBuffer, level, regX * leaves, regZ * leaves, regX * leaves + leaves - 1,
                    regZ * leaves + leaves - 1);
        }

        processor.getMapWorld().getCurrentDimension().getLayeredMapRegions().bumpLoadedRegion(region);
    }

    @Definition(id = "leafZ", local = @Local(type = int.class, name = "leafZ"))
    @Definition(id = "leveledSideInRegions", local = @Local(type = int.class, name = "leveledSideInRegions"))
    @Expression("leafZ < @(leveledSideInRegions)")
    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = EXPRESSION),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private int toroidal$extendLeafColumn(int leveledSideInRegions, @Local(name = "textureLevel") int textureLevel,
            @Local(name = "leveledRegX") int leveledRegX, @Local(name = "leveledRegZ") int leveledRegZ,
            @Local(name = "leafRegionMinX") int leafRegionMinX, @Local(name = "leafRegionMinZ") int leafRegionMinZ,
            @Local(name = "leafX") int leafX, @Local(name = "minLeafRegX") int minLeafRegX,
            @Local(name = "maxLeafRegX") int maxLeafRegX) {
        long owner = ChunkPos.pack(leveledRegX, leveledRegZ);
        if (owner != this.toroidal$fannedLeavesOwner) {
            this.toroidal$fannedLeavesOwner = owner;
            this.toroidal$fannedLeaves = NO_LEAVES;
            this.toroidal$fannedLeafColumn = NO_COLUMN;
            this.toroidal$firstFannedLeafRow = leafRegionMinZ + leveledSideInRegions;
        }

        int regX = leafRegionMinX + leafX;
        if (this.toroidal$fannedLeafColumn == NO_COLUMN && regX >= minLeafRegX && regX <= maxLeafRegX) {
            this.toroidal$fannedLeafColumn = regX;
            this.toroidal$fannedLeaves = toroidal$untakenLeaves(
                    XaeroWorldMapFold.fannedLeaves(this.toroidal$frame.shape(), leveledRegX, leveledRegZ, textureLevel));
        }

        return regX == this.toroidal$fannedLeafColumn
                ? leveledSideInRegions + this.toroidal$fannedLeaves.length : leveledSideInRegions;
    }

    @Definition(id = "regZ", local = @Local(type = int.class, name = "regZ"))
    @Definition(id = "maxLeafRegZ", local = @Local(type = int.class, name = "maxLeafRegZ"))
    @Expression("regZ <= @(maxLeafRegZ)")
    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = EXPRESSION),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private int toroidal$admitFannedLeafRow(int maxLeafRegZ, @Local(name = "regX") int regX,
            @Local(name = "regZ") int regZ) {
        return toroidal$fannedLeaf(regX, regZ) == NO_LEAF ? maxLeafRegZ : Integer.MAX_VALUE;
    }

    @Unique
    private long[] toroidal$untakenLeaves(long[] fanned) {
        LongArrayList untaken = new LongArrayList(fanned.length);
        for (long leaf : fanned) {
            if (this.toroidal$fannedLeavesTaken.add(leaf)) {
                untaken.add(leaf);
            }
        }

        return untaken.toLongArray();
    }

    @Unique
    private long toroidal$fannedLeaf(int regX, int regZ) {
        int index = regZ - this.toroidal$firstFannedLeafRow;
        return regX == this.toroidal$fannedLeafColumn && index >= 0 && index < this.toroidal$fannedLeaves.length
                ? this.toroidal$fannedLeaves[index] : NO_LEAF;
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lxaero/map/MapProcessor;regionExists(III)Z"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private boolean toroidal$fannedLeafExists(boolean exists, @Local(name = "regX") int regX,
            @Local(name = "regZ") int regZ) {
        return exists && toroidal$fannedLeaf(regX, regZ) == NO_LEAF;
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private @Nullable MapRegion toroidal$fetchLoopLeaf(@Nullable MapRegion existing,
            @Local(name = "renderedCaveLayer") int caveLayer, @Local(name = "regX") int regX,
            @Local(name = "regZ") int regZ) {
        long leaf = toroidal$fannedLeaf(regX, regZ);
        return leaf == NO_LEAF
                ? toroidal$leafOrFold(existing, caveLayer, regX, regZ)
                : toroidal$leafRegion(caveLayer, ChunkPos.getX(leaf), ChunkPos.getZ(leaf));
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION, ordinal = 0))
    private @Nullable MapRegion toroidal$fetchHoverLeaf(@Nullable MapRegion existing,
            @Local(name = "renderedCaveLayer") int caveLayer) {
        return toroidal$leafOrFold(existing, caveLayer, this.mouseBlockPosX >> REGION_SHIFT,
                this.mouseBlockPosZ >> REGION_SHIFT);
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION, ordinal = 3))
    private @Nullable MapRegion toroidal$fetchDebugLeaf(@Nullable MapRegion existing,
            @Local(name = "renderedCaveLayer") int caveLayer, @Local(name = "regX") int regX,
            @Local(name = "regZ") int regZ) {
        return toroidal$leafOrFold(existing, caveLayer, regX, regZ);
    }

    @Unique
    private @Nullable MapRegion toroidal$leafOrFold(@Nullable MapRegion existing, int caveLayer, int regX, int regZ) {
        ToroidalShape shape = this.toroidal$frame.shape();
        if (existing != null || shape == null) {
            return existing;
        }

        BlockPos foldedOrigin = XaeroWorldMapFold.foldBlock(shape, regX * XaeroWorldMapFold.REGION_BLOCKS,
                regZ * XaeroWorldMapFold.REGION_BLOCKS);
        return toroidal$leafRegion(caveLayer, Math.floorDiv(foldedOrigin.getX(), XaeroWorldMapFold.REGION_BLOCKS),
                Math.floorDiv(foldedOrigin.getZ(), XaeroWorldMapFold.REGION_BLOCKS));
    }

    @Unique
    private @Nullable MapRegion toroidal$leafRegion(int caveLayer, int regX, int regZ) {
        MapProcessor processor = this.mapProcessor;
        return processor.getLeafMapRegion(caveLayer, regX, regZ, processor.regionExists(caveLayer, regX, regZ));
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/misc/Misc;addToListOfSmallest(ILjava/util/List;Ljava/lang/Comparable;)V",
                    ordinal = 0))
    private <T extends Comparable<? super T>> void toroidal$queueLeafOnce(int maxSize, List<T> list, T element,
            Operation<Void> original) {
        if (!list.contains(element)) {
            original.call(maxSize, list, element);
        }
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = LEVELED_REGION_GET_TEXTURE, ordinal = 0))
    private @Nullable RegionTexture<?> toroidal$foldHoverTexture(@Nullable RegionTexture<?> texture) {
        if (this.toroidal$frame.shape() == null) {
            return texture;
        }

        return this.toroidal$frame.canonicalRegionTexture(this.mapProcessor, this.mouseBlockPosX, this.mouseBlockPosZ);
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = LEVELED_REGION_GET_TEXTURE, ordinal = 1))
    private @Nullable RegionTexture<?> toroidal$foldLeafTexture(@Nullable RegionTexture<?> texture,
            @Local(name = "leveledRegion") LeveledRegion<?> region, @Local(name = "o") int slotX,
            @Local(name = "p") int slotZ) {
        XaeroRegionFrame frame = this.toroidal$frame;
        boolean isCandidate = frame.isCandidate(region);
        frame.beginSlot(slotX, slotZ);
        ToroidalShape shape = frame.shape();
        if (shape == null) {
            return isCandidate ? null : texture;
        }

        int slotSize = frame.slotSize();
        if (!frame.glueableAt(slotSize)) {
            if (!isCandidate && frame.slotInsideWorld(slotSize)) {
                return texture;
            }

            frame.slotFolded(true);
            return frame.anyCanonicalTexture(this.mapProcessor);
        }

        int viewBlockX = frame.slotViewBlockX();
        int viewBlockZ = frame.slotViewBlockZ();
        BlockPos foldedBlock = XaeroWorldMapFold.foldBlock(shape, viewBlockX, viewBlockZ);
        if (foldedBlock.getX() == viewBlockX && foldedBlock.getZ() == viewBlockZ) {
            return isCandidate ? null : texture;
        }

        frame.slotFolded(true);
        return frame.mapCopies() == MapCopies.SINGLE
                ? null : frame.canonicalRegionTexture(this.mapProcessor, foldedBlock.getX(), foldedBlock.getZ());
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = LEVELED_REGION_GET_TEXTURE, ordinal = 2))
    private @Nullable RegionTexture<?> toroidal$suppressFoldedRootTexture(@Nullable RegionTexture<?> texture) {
        return this.toroidal$frame.shape() != null && this.toroidal$frame.slotFolded() ? null : texture;
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/texture/RegionTexture;getGlColorTexture()"
                            + "Lxaero/lib/client/graphics/GpuTextureAndView;",
                    ordinal = 0),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lxaero/map/gui/GuiMap;renderTexturedModalSubRectWithLighting(Lorg/joml/Matrix4f;"
                                    + "FFFFFFFFLcom/mojang/blaze3d/textures/GpuTextureView;Z"
                                    + "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V")))
    private @Nullable GpuTextureAndView toroidal$drawClippedPeriodCopies(@Nullable GpuTextureAndView texture,
            @Local(name = "matrix") Matrix4f matrix, @Local(name = "textureX") int textureX,
            @Local(name = "textureZ") int textureZ, @Local(name = "flooredCameraX") int flooredCameraX,
            @Local(name = "flooredCameraZ") int flooredCameraZ,
            @Local(name = "regionTexture") RegionTexture<?> regionTexture,
            @Local(name = "withLightRenderer") MultiTextureRenderTypeRenderer withLightRenderer,
            @Local(name = "noLightRenderer") MultiTextureRenderTypeRenderer noLightRenderer) {
        XaeroRegionFrame frame = this.toroidal$frame;
        ToroidalShape shape = frame.shape();
        int slotSize = frame.slotSize();
        if (texture == null || shape == null || frame.glueableAt(slotSize)) {
            return texture;
        }

        float x = textureX - flooredCameraX;
        float y = textureZ - flooredCameraZ;
        boolean hasLight = regionTexture.getTextureHasLight();
        MultiTextureRenderTypeRenderer renderer = hasLight ? withLightRenderer : noLightRenderer;
        List<WorldCopies.Copy> copies = frame.drawnCopies(slotSize, this.cameraX, this.cameraZ, this.scale);
        int viewBlockX = frame.slotViewBlockX();
        int viewBlockZ = frame.slotViewBlockZ();
        for (long origin : frame.slotOrigins(slotSize)) {
            if (!frame.drawnOnce(origin)) {
                continue;
            }

            int originX = ChunkPos.getX(origin);
            int originZ = ChunkPos.getZ(origin);
            RegionTexture<?> canonical = frame.canonicalRegionTexture(this.mapProcessor, originX, originZ);
            GpuTextureAndView textureAndView = canonical == null ? null : canonical.getGlColorTexture();
            if (textureAndView == null) {
                continue;
            }

            for (TilePlacement placement : XaeroWorldMapFold.placements(shape, originX, originZ, slotSize, copies)) {
                GuiMap.renderTexturedModalSubRectWithLighting(matrix, x + (placement.minX() - viewBlockX),
                        y + (placement.minZ() - viewBlockZ), placement.u1(), placement.v1(), placement.u2(),
                        placement.v2(), placement.width(), placement.height(), textureAndView.view, hasLight,
                        renderer);
            }
        }

        return null;
    }
}
