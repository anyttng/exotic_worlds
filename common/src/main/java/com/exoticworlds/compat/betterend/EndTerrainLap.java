package com.exoticworlds.compat.betterend;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.LapFrame;
import com.exoticworlds.compat.OpenSimplexStandIn;
import com.exoticworlds.compat.StandInHolder;
import com.exoticworlds.compat.betterend.mixin.TerrainGeneratorAccessor;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public final class EndTerrainLap {
    private static volatile @Nullable EndTerrainLap current;

    private final WorldFold fold;

    private final TranslationLattice lattice;

    private final LapFrame frame;

    private final OpenSimplexStandIn first;

    private final OpenSimplexStandIn second;

    EndTerrainLap(WorldFold fold, StandInHolder first, StandInHolder second) {
        this.fold = fold;
        this.lattice = fold.blockLattice();
        this.frame = new LapFrame(this.lattice);
        this.first = first.exotic_worlds$standIn();
        this.second = second.exotic_worlds$standIn();
    }

    public static void capture(ServerLevel level) {
        WorldFold fold = ShapedChunkGenerator.wrappedFoldOf(level.getChunkSource().getGenerator());
        OpenSimplexNoise first = TerrainGeneratorAccessor.exotic_worlds$noise1();
        OpenSimplexNoise second = TerrainGeneratorAccessor.exotic_worlds$noise2();
        current = fold == null || first == null || second == null
                ? null
                : new EndTerrainLap(fold, (StandInHolder) (Object) first, (StandInHolder) (Object) second);
    }

    public static @Nullable EndTerrainLap current() {
        return current;
    }

    public WorldFold fold() {
        return this.fold;
    }

    public OpenSimplexStandIn first() {
        return this.first;
    }

    public OpenSimplexStandIn second() {
        return this.second;
    }

    public int foldX(int blockX, int blockZ) {
        return this.lattice.foldX(blockX, blockZ);
    }

    public int foldZ(int blockZ) {
        return this.lattice.foldZ(blockZ);
    }

    public double frameX(double x, double z) {
        return this.frame.frameX(x, z);
    }

    public double period(Direction.Axis axis, double unitBlocks) {
        WrapDomain domain = axis == Direction.Axis.X ? this.lattice.x() : this.lattice.z();
        return domain.loops() ? domain.domainLength / unitBlocks : OpenSimplexStandIn.UNBOUNDED;
    }

    public LapIslandPlacement.Grid grid(double distance) {
        return new LapIslandPlacement.Grid(IslandLapAxis.of(this.lattice.x(), distance),
                IslandLapAxis.of(this.lattice.z(), distance), this.frame);
    }
}
