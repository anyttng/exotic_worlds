package com.exoticworlds.compat.betterend;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.core.ShapedChunkGenerator;
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

    private final WrapDomain x;

    private final WrapDomain z;

    private final OpenSimplexStandIn first;

    private final OpenSimplexStandIn second;

    private EndTerrainLap(WorldFold fold, long seed) {
        this.fold = fold;
        this.x = fold.blockDomain(Direction.Axis.X);
        this.z = fold.blockDomain(Direction.Axis.Z);
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

    public int foldX(int block) {
        return this.x.wrap(block);
    }

    public int foldZ(int block) {
        return this.z.wrap(block);
    }

    public double period(Direction.Axis axis, double unitBlocks) {
        WrapDomain domain = domain(axis);
        return domain.loops() ? domain.domainLength / unitBlocks : OpenSimplexStandIn.UNBOUNDED;
    }

    public LapIslandPlacement.Grid grid(double distance) {
        return new LapIslandPlacement.Grid(IslandLapAxis.of(this.x, distance), IslandLapAxis.of(this.z, distance));
    }

    private WrapDomain domain(Direction.Axis axis) {
        return axis == Direction.Axis.X ? this.x : this.z;
    }
}
