package com.exoticworlds.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;

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
            int caveLayer, int regX, int regZ, int level, double cameraX, double cameraZ, double scale) {
        XaeroRegionFrame frame = this.frame;
        frame.beginView(caveLayer, regX, regZ, level, cameraX, cameraZ, scale);
        this.sweep.seen(regX, regZ);
        ToroidalShape shape = frame.shape();
        if (shape == null) {
            return existing;
        }

        this.sweep.fanned(frame.fannedRegions(regX, regZ));
        if (existing != null) {
            return existing;
        }

        LeveledRegion<?> candidate = null;
        for (long corner : XaeroWorldMapFold.canonicalRegionCorners(shape, regX, regZ, level)) {
            candidate = texturedRegion(processor, caveLayer, ChunkPos.getX(corner), ChunkPos.getZ(corner), level);
            if (candidate != null) {
                this.sweep.seen(ChunkPos.getX(corner), ChunkPos.getZ(corner));
                break;
            }
        }

        frame.candidate(candidate);
        return candidate;
    }

    private static @Nullable LeveledRegion<?> texturedRegion(MapProcessor processor, int caveLayer, int regX, int regZ,
            int level) {
        LeveledRegion<?> region = processor.getLeveledRegion(caveLayer, regX, regZ, level);
        return region == null || !region.hasTextures() ? null : region;
    }

    @SuppressWarnings("NonApiType")
    public void keepFannedRegions(MapProcessor processor, boolean prevWaitingForBranchCache,
            boolean[] waitingForBranchCache, ArrayList<BranchLeveledRegion> branchRegionBuffer) {
        this.sweep.forEachUnseen(region -> keep(processor, ChunkPos.getX(region), ChunkPos.getZ(region),
                prevWaitingForBranchCache, waitingForBranchCache, branchRegionBuffer));
    }

    @SuppressWarnings("NonApiType")
    private void keep(MapProcessor processor, int regX, int regZ, boolean prevWaitingForBranchCache,
            boolean[] waitingForBranchCache, ArrayList<BranchLeveledRegion> branchRegionBuffer) {
        int level = this.frame.viewLevel();
        LeveledRegion<?> region = processor.getLeveledRegion(this.frame.viewCaveLayer(), regX, regZ, level);
        if (region == null || processor.isUploadingPaused() || WorldMap.pauseRequests) {
            return;
        }

        if (region instanceof BranchLeveledRegion branch) {
            int leaves = 1 << level;
            branch.checkForUpdates(processor, prevWaitingForBranchCache, waitingForBranchCache, branchRegionBuffer,
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

    public boolean fannedLeafExists(boolean exists, int regX, int regZ) {
        return this.fan.exists(exists, regX, regZ);
    }

    public @Nullable MapRegion loopLeaf(MapProcessor processor, @Nullable MapRegion existing, int caveLayer, int regX,
            int regZ) {
        long leaf = this.fan.leaf(regX, regZ);
        return leaf == XaeroLeafFan.NO_LEAF
                ? leafOrFold(processor, existing, caveLayer, regX, regZ)
                : leafRegion(processor, caveLayer, ChunkPos.getX(leaf), ChunkPos.getZ(leaf));
    }

    public @Nullable MapRegion hoverLeaf(MapProcessor processor, @Nullable MapRegion existing, int caveLayer,
            int mouseBlockX, int mouseBlockZ) {
        return leafOrFold(processor, existing, caveLayer, Math.floorDiv(mouseBlockX, XaeroWorldMapFold.REGION_BLOCKS),
                Math.floorDiv(mouseBlockZ, XaeroWorldMapFold.REGION_BLOCKS));
    }

    public @Nullable MapRegion leafOrFold(MapProcessor processor, @Nullable MapRegion existing, int caveLayer, int regX,
            int regZ) {
        ToroidalShape shape = this.frame.shape();
        if (existing != null || shape == null) {
            return existing;
        }

        ChunkPos folded = XaeroWorldMapFold.foldRegion(shape, regX, regZ);
        return leafRegion(processor, caveLayer, folded.x, folded.z);
    }

    private static @Nullable MapRegion leafRegion(MapProcessor processor, int caveLayer, int regX, int regZ) {
        return processor.getLeafMapRegion(caveLayer, regX, regZ, processor.regionExists(caveLayer, regX, regZ));
    }

    public static <T> void queueOnce(List<T> list, T element, Runnable queue) {
        if (!list.contains(element)) {
            queue.run();
        }
    }
}
