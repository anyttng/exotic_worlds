package com.exoticworlds.compat.betterend;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

public final class EndTerrainLap {
    private static final int ISLAND_LAYER_SEEDS = 3;

    private static volatile @Nullable EndTerrainLap current;

    private final WorldFold fold;

    private final TranslationLattice lattice;

    private final LapFrame frame;

    private final OpenSimplexStandIn first;

    private final OpenSimplexStandIn second;

    EndTerrainLap(WorldFold fold, long seed) {
        this.fold = fold;
        this.lattice = fold.blockLattice();
        this.frame = new LapFrame(this.lattice);
        RandomSource random = new LegacyRandomSource(seed);
        for (int layer = 0; layer < ISLAND_LAYER_SEEDS; layer++) {
            random.nextInt();
        }

        this.first = new OpenSimplexStandIn(random.nextInt());
        this.second = new OpenSimplexStandIn(random.nextInt());
    }

    public static void capture(ServerLevel level, long seed) {
        WorldFold fold = ShapedChunkGenerator.wrappedTransformerOf(level.getChunkSource().getGenerator());
        current = fold == null ? null : new EndTerrainLap(fold, seed);
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
