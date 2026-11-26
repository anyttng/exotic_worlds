package com.exoticworlds.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.ChunkPos;

import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;

public final class XaeroRegionLoads {
    private final XaeroRegionFrame frame;
    private final XaeroFannedSweep sweep = new XaeroFannedSweep();
    private final XaeroLeafFan fan = new XaeroLeafFan();

    public XaeroRegionLoads(XaeroRegionFrame frame) {
        this.frame = frame;
    }

    public void begin() {
        this.sweep.begin();
        this.fan.begin();
        this.frame.begin();
    }

    public @Nullable LeveledRegion<?> leveledOrFold(MapProcessor processor, @Nullable LeveledRegion<?> existing,
            XaeroRegionKey region, XaeroCamera camera) {
        XaeroRegionFrame frame = this.frame;
        frame.beginView(region, camera);
        int regX = region.regX();
        int regZ = region.regZ();
        this.sweep.seen(regX, regZ);
        WorldFold fold = frame.getFold();
        if (fold == null) {
            return existing;
        }

        this.sweep.fanned(frame.fannedRegions(regX, regZ));
        if (existing != null) {
            return existing;
        }

        LeveledRegion<?> candidate = this.canonicalCandidate(processor, fold, region);
        frame.candidate(candidate);
        return candidate;
    }

    private @Nullable LeveledRegion<?> canonicalCandidate(MapProcessor processor, WorldFold fold,
            XaeroRegionKey region) {
        int level = region.level();
        for (long corner : XaeroWorldMapFold.canonicalRegionCorners(fold, region.regX(), region.regZ(), level)) {
            LeveledRegion<?> candidate = getTexturedRegion(processor, region.caveLayer(), ChunkPos.getX(corner),
                    ChunkPos.getZ(corner), level);
            if (candidate != null) {
                this.sweep.seen(ChunkPos.getX(corner), ChunkPos.getZ(corner));
                return candidate;
            }
        }

        return null;
    }

    private static @Nullable LeveledRegion<?> getTexturedRegion(MapProcessor processor, int caveLayer, int regX, int regZ,
            int level) {
        LeveledRegion<?> region = processor.getLeveledRegion(caveLayer, regX, regZ, level);
        return region == null || !region.hasTextures() ? null : region;
    }

    @SuppressWarnings("NonApiType")
    public void keepFannedRegions(MapProcessor processor, boolean isPrevWaitingForBranchCache,
            boolean[] waitingForBranchCache, ArrayList<BranchLeveledRegion> branchRegionBuffer) {
        this.sweep.forEachUnseen(region -> keep(processor, ChunkPos.getX(region), ChunkPos.getZ(region),
                isPrevWaitingForBranchCache, waitingForBranchCache, branchRegionBuffer));
    }

    @SuppressWarnings("NonApiType")
    private void keep(MapProcessor processor, int regX, int regZ, boolean isPrevWaitingForBranchCache,
            boolean[] waitingForBranchCache, ArrayList<BranchLeveledRegion> branchRegionBuffer) {
        int level = this.frame.getViewLevel();
        LeveledRegion<?> region = processor.getLeveledRegion(this.frame.getViewCaveLayer(), regX, regZ, level);
        if (region == null || processor.isUploadingPaused() || WorldMap.pauseRequests) {
            return;
        }

        if (region instanceof BranchLeveledRegion branch) {
            int leaves = 1 << level;
            branch.checkForUpdates(processor, isPrevWaitingForBranchCache, waitingForBranchCache, branchRegionBuffer,
                    level, regX * leaves, regZ * leaves, regX * leaves + leaves - 1, regZ * leaves + leaves - 1);
        }

        processor.getMapWorld().getCurrentDimension().getLayeredMapRegions().bumpLoadedRegion(region);
    }

    public int extendLeafColumn(int leveledSideInRegions, int leveledRegX, int leveledRegZ, int leafRegionMinX,
            int leafRegionMinZ, int leafX, int minLeafRegX, int maxLeafRegX) {
        return this.fan.extendColumn(leveledSideInRegions, leveledRegX, leveledRegZ, leafRegionMinX, leafRegionMinZ,
                leafX, minLeafRegX, maxLeafRegX, this.frame::fannedLeaves);
    }

    public int admitFannedLeafRow(int maxLeafRegZ, int regX, int regZ) {
        return this.fan.admitRow(maxLeafRegZ, regX, regZ);
    }

    public boolean isFannedLeafExisting(boolean isExisting, int regX, int regZ) {
        return this.fan.isExisting(isExisting, regX, regZ);
    }

    public @Nullable MapRegion getLoopLeaf(MapProcessor processor, @Nullable MapRegion existing, int caveLayer, int regX,
            int regZ) {
        long leaf = this.fan.getLeaf(regX, regZ);
        return leaf == XaeroLeafFan.NO_LEAF
                ? getLeafOrFold(processor, existing, caveLayer, regX, regZ)
                : getLeafRegion(processor, caveLayer, ChunkPos.getX(leaf), ChunkPos.getZ(leaf));
    }

    public @Nullable MapRegion getHoverLeaf(MapProcessor processor, @Nullable MapRegion existing, int caveLayer,
            int mouseBlockX, int mouseBlockZ) {
        return getLeafOrFold(processor, existing, caveLayer, Math.floorDiv(mouseBlockX, XaeroWorldMapFold.REGION_BLOCKS),
                Math.floorDiv(mouseBlockZ, XaeroWorldMapFold.REGION_BLOCKS));
    }

    public @Nullable MapRegion getLeafOrFold(MapProcessor processor, @Nullable MapRegion existing, int caveLayer, int regX,
            int regZ) {
        WorldFold fold = this.frame.getFold();
        if (existing != null || fold == null) {
            return existing;
        }

        ChunkPos folded = XaeroWorldMapFold.foldRegion(fold, regX, regZ);
        return getLeafRegion(processor, caveLayer, folded.x(), folded.z());
    }

    private static @Nullable MapRegion getLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ) {
        return processor.getLeafMapRegion(caveLayer, regX, regZ, processor.regionExists(caveLayer, regX, regZ));
    }

    public static <T> void queueOnce(List<T> list, T element, Runnable queue) {
        if (!list.contains(element)) {
            queue.run();
        }
    }
}
