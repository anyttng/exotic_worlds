package com.exoticworlds.compat.betterend.mixin;

import java.util.ArrayList;
import java.util.List;

import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFRadialNoiseMap;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.generator.GeneratorOptions;
import org.betterx.betterend.world.generator.IslandLayer;
import org.betterx.betterend.world.generator.LayerOptions;
import org.betterx.betterend.world.generator.TerrainGenerator;
import org.betterx.wover.generator.api.biomesource.end.WoverEndConfig;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.compat.betterend.EndTerrainLap;
import com.exoticworlds.compat.betterend.LapIslandPlacement;
import com.exoticworlds.compat.betterend.LapIslands;

import net.minecraft.core.BlockPos;

@Mixin(IslandLayer.class)
public abstract class IslandLayerMixin implements LapIslands {
    @Unique
    private static final float FAR = 10.0F;

    @Unique
    private static final float NOISE_RADIUS = 0.5F;

    @Shadow
    @Final
    private SDFRadialNoiseMap noise;

    @Shadow
    @Final
    private OpenSimplexNoise density;

    @Shadow
    @Final
    private LayerOptions options;

    @Unique
    private final List<LapIslandPlacement.Island> exotic_worlds$islands = new ArrayList<>();

    @Unique
    private @Nullable EndTerrainLap exotic_worlds$lap;

    @Unique
    private LapIslandPlacement.@Nullable Grid exotic_worlds$grid;

    @Unique
    private int exotic_worlds$lastX = Integer.MIN_VALUE;

    @Unique
    private int exotic_worlds$lastZ = Integer.MIN_VALUE;

    @Unique
    private int exotic_worlds$lastHeight;

    @Shadow
    private int getSeed(int x, int z) {
        throw new AssertionError();
    }

    @Shadow
    private SDF getIsland(BlockPos pos) {
        throw new AssertionError();
    }

    @Shadow
    private float getRelativeDistance(SDF sdf, BlockPos center, double px, double py, double pz) {
        throw new AssertionError();
    }

    @Override
    public void exotic_worlds$updatePositionsOnLap(EndTerrainLap lap, double x, double z, int maxHeight) {
        LapIslandPlacement.Grid grid = this.exotic_worlds$grid;
        if (grid == null || this.exotic_worlds$lap != lap) {
            grid = lap.grid(this.options.distance);
            this.exotic_worlds$grid = grid;
            this.exotic_worlds$lap = lap;
            this.exotic_worlds$lastX = Integer.MIN_VALUE;
        }

        int cellX = grid.cellX(x, z);
        int cellZ = grid.z().cell(z);
        if (cellX == this.exotic_worlds$lastX && cellZ == this.exotic_worlds$lastZ && maxHeight == this.exotic_worlds$lastHeight) {
            return;
        }

        this.exotic_worlds$lastX = cellX;
        this.exotic_worlds$lastZ = cellZ;
        this.exotic_worlds$lastHeight = maxHeight;
        LapIslandPlacement.place(this.exotic_worlds$islands, grid, cellX, cellZ, maxHeight, this.options, this::getSeed,
                this.density, exotic_worlds$center());
    }

    @Override
    public float exotic_worlds$densityOnLap(double x, double y, double z) {
        float distance = FAR;
        for (LapIslandPlacement.Island island : this.exotic_worlds$islands) {
            distance = Math.min(distance,
                    getRelativeDistance(getIsland(island.canonical()), island.seated(), x, y, z));
        }

        return -distance;
    }

    @Override
    public float exotic_worlds$densityOnLap(double x, double y, double z, float height) {
        this.noise.setIntensity(height);
        this.noise.setRadius(NOISE_RADIUS / (1.0F + height));
        return exotic_worlds$densityOnLap(x, y, z);
    }

    @Unique
    private static LapIslandPlacement.Center exotic_worlds$center() {
        WoverEndConfig config = TerrainGenerator.config;
        return config == null
                ? new LapIslandPlacement.Center(false, 0, 0L)
                : new LapIslandPlacement.Center(GeneratorOptions.hasCentralIsland(), config.centerBiomesSize,
                        config.innerVoidRadiusSquared);
    }
}
