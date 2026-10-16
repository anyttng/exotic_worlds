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
import com.mojang.blaze3d.textures.GpuTextureView;
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
    private final XaeroRegionFrame toroidal$frame = new XaeroRegionFrame();
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
    private final LongOpenHashSet toroidal$drawnCanonicalSlots = new LongOpenHashSet();
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
        this.toroidal$drawnCanonicalSlots.clear();
        this.toroidal$fannedRegions.clear();
        this.toroidal$loopRegions.clear();
        this.toroidal$fannedLeavesTaken.clear();
        this.toroidal$fannedLeavesOwner = NO_OWNER;
        this.toroidal$frame.begin();
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION))
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
        ToroidalShape shape = this.toroidal$frame.shape();
        if (shape == null) {
            return existing;
        }

        for (long fanned : XaeroWorldMapFold.fannedRegions(shape, regX, regZ, level)) {
            this.toroidal$fannedRegions.add(fanned);
        }

        if (existing != null) {
            return existing;
        }

        // A candidate value only, so the draw block runs at all; the texture redirect re-resolves each slot precisely.
        int side = XaeroWorldMapFold.REGION_BLOCKS << level;
        BlockPos foldedOrigin = XaeroWorldMapFold.foldBlock(shape, regX * side, regZ * side);
        int candidateX = Math.floorDiv(foldedOrigin.getX(), side);
        int candidateZ = Math.floorDiv(foldedOrigin.getZ(), side);
        LeveledRegion<?> candidate = original.call(processor, caveLayer, candidateX, candidateZ, level);
        if (candidate != null) {
            this.toroidal$loopRegions.add(ChunkPos.pack(candidateX, candidateZ));
        }

        this.toroidal$leveledCandidate = candidate;
        return candidate;
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
        MapProcessor processor = this.toroidal$processor;
        int level = this.toroidal$viewLevel;
        LeveledRegion<?> region = processor.getLeveledRegion(this.toroidal$viewCaveLayer, regX, regZ, level);
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

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lxaero/map/MapProcessor;regionExists(III)Z"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private boolean toroidal$fannedLeafExists(MapProcessor processor, int caveLayer, int regX, int regZ,
            Operation<Boolean> original) {
        long leaf = toroidal$fannedLeaf(regX, regZ);
        return leaf == NO_LEAF
                ? original.call(processor, caveLayer, regX, regZ)
                : original.call(processor, caveLayer, ChunkPos.getX(leaf), ChunkPos.getZ(leaf));
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 1),
                    to = @At(value = "INVOKE", target = MAP_PROCESSOR_GET_LEVELED_REGION, ordinal = 2)))
    private @Nullable MapRegion toroidal$fetchLoopLeaf(MapProcessor processor, int caveLayer, int regX, int regZ,
            boolean create, Operation<@Nullable MapRegion> original) {
        long leaf = toroidal$fannedLeaf(regX, regZ);
        return leaf == NO_LEAF
                ? toroidal$leafOrFold(processor, caveLayer, regX, regZ, create, original)
                : original.call(processor, caveLayer, ChunkPos.getX(leaf), ChunkPos.getZ(leaf), create);
    }

    @WrapOperation(
            method = "extractRenderState",
            at = {
                    @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION, ordinal = 0),
                    @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION, ordinal = 3)})
    private @Nullable MapRegion toroidal$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ,
            boolean create, Operation<@Nullable MapRegion> original) {
        return toroidal$leafOrFold(processor, caveLayer, regX, regZ, create, original);
    }

    @Unique
    private @Nullable MapRegion toroidal$leafOrFold(MapProcessor processor, int caveLayer, int regX, int regZ,
            boolean create, Operation<@Nullable MapRegion> original) {
        MapRegion existing = original.call(processor, caveLayer, regX, regZ, create);
        ToroidalShape shape = this.toroidal$frame.shape();
        if (existing != null || shape == null) {
            return existing;
        }

        BlockPos foldedOrigin = XaeroWorldMapFold.foldBlock(shape, regX * XaeroWorldMapFold.REGION_BLOCKS,
                regZ * XaeroWorldMapFold.REGION_BLOCKS);
        int foldedRegX = Math.floorDiv(foldedOrigin.getX(), XaeroWorldMapFold.REGION_BLOCKS);
        int foldedRegZ = Math.floorDiv(foldedOrigin.getZ(), XaeroWorldMapFold.REGION_BLOCKS);
        return original.call(processor, caveLayer, foldedRegX, foldedRegZ,
                processor.regionExists(caveLayer, foldedRegX, foldedRegZ));
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

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/LeveledRegion;hasTextures()Z"))
    private boolean toroidal$candidateHasTextures(LeveledRegion<?> region, Operation<Boolean> original) {
        if (this.toroidal$frame.shape() != null && region != null && region == this.toroidal$leveledCandidate) {
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
        if (this.toroidal$frame.shape() == null) {
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
        XaeroRegionFrame frame = this.toroidal$frame;
        frame.beginSlot(viewBlockX, viewBlockZ);
        ToroidalShape shape = frame.shape();
        if (shape == null) {
            return isCandidate ? null : original.call(region, slotX, slotZ);
        }

        if (!frame.glueableAt(slotSize)) {
            if (!isCandidate && frame.slotInsideWorld(slotSize)) {
                return original.call(region, slotX, slotZ);
            }

            this.toroidal$slotFolded = true;
            return toroidal$anyCanonicalTexture(slotSize);
        }

        BlockPos foldedBlock = XaeroWorldMapFold.foldBlock(shape, viewBlockX, viewBlockZ);
        if (foldedBlock.getX() == viewBlockX && foldedBlock.getZ() == viewBlockZ) {
            return isCandidate ? null : original.call(region, slotX, slotZ);
        }

        this.toroidal$slotFolded = true;
        return frame.mapCopies() == MapCopies.SINGLE
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
        if (this.toroidal$frame.shape() != null && this.toroidal$slotFolded) {
            return null;
        }

        return original.call(region, textureX, textureZ);
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.GUI_MAP_RENDER_TEXTURED_MODAL_RECT_WITH_LIGHTING_3))
    private void toroidal$drawClippedPeriodCopies(
            Matrix4f matrix, float x, float y, float width, float height,
            GpuTextureView texture, boolean hasLight, MultiTextureRenderTypeRenderer renderer, Operation<Void> original) {
        int slotSize = XaeroWorldMapFold.SLOT_BLOCKS << this.toroidal$viewLevel;
        XaeroRegionFrame frame = this.toroidal$frame;
        ToroidalShape shape = frame.shape();
        if (shape == null || frame.glueableAt(slotSize)) {
            original.call(matrix, x, y, width, height, texture, hasLight, renderer);
            return;
        }

        List<WorldCopies.Copy> copies = frame.drawnCopies(slotSize, this.cameraX, this.cameraZ, this.scale);
        int viewBlockX = frame.slotViewBlockX();
        int viewBlockZ = frame.slotViewBlockZ();
        for (long origin : frame.slotOrigins(slotSize)) {
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

            for (TilePlacement placement : XaeroWorldMapFold.placements(shape, originX, originZ, slotSize, copies)) {
                GuiMap.renderTexturedModalSubRectWithLighting(matrix, x + (placement.minX() - viewBlockX),
                        y + (placement.minZ() - viewBlockZ), placement.u1(), placement.v1(), placement.u2(),
                        placement.v2(), placement.width(), placement.height(), textureAndView.view, hasLight,
                        renderer);
            }
        }
    }

    @Unique
    private @Nullable RegionTexture<?> toroidal$anyCanonicalTexture(int slotSize) {
        for (long origin : this.toroidal$frame.slotOrigins(slotSize)) {
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
