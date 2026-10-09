package com.exoticworlds.compat.betterend;

import java.awt.Point;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.betterx.betterend.world.generator.IslandLayer;
import org.betterx.betterend.world.generator.TerrainBoolCache;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.betterend.mixin.TerrainGeneratorAccessor;
import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class LapTerrain {
    private static final float FADE_OUT_BELOW_TOP = 28.0F;

    private static final double FADE_OUT_DISTANCE = 27.0;

    private static final double AIR = -1.0;

    private static final float SOLID = 1.0F;

    private static final float DETAIL_ABOVE = -0.5F;

    private static final double LAND_ABOVE = -0.01;

    private static final float DEPTH_SHARE = 0.5F;

    private static final double LAND_CELL_BLOCKS = 8.0;

    private static final double LAND_CELL_HEIGHT = 4.0;

    private static final double CELL_MIDDLE = 0.5;

    private static final int BOOL_CACHE_LIMIT = 64;

    private static final byte LAND_AIR = 1;

    private static final byte LAND_SOLID = 2;

    private static final double[] WARP_FREQUENCIES = {0.1, 0.2, 0.4};

    private static final double[] WARP_AMPLITUDES = {20.0, 10.0, 5.0};

    private static final double[] DETAIL_FREQUENCIES = {0.01, 0.05, 0.1};

    private static final double[] DETAIL_AMPLITUDES = {0.02, 0.01, 0.005};

    private record Warp(EndTerrainLap lap, double xPeriod, double zPeriod) {
        static Warp of(EndTerrainLap lap, double unitBlocks) {
            return new Warp(lap, lap.period(Direction.Axis.X, unitBlocks), lap.period(Direction.Axis.Z, unitBlocks));
        }

        double x(double x, double z) {
            return sum(this.lap.first(), this.lap.second(), x, z);
        }

        double z(double x, double z) {
            return sum(this.lap.second(), this.lap.first(), x, z);
        }

        private double sum(OpenSimplexStandIn odd, OpenSimplexStandIn even, double x, double z) {
            double frameX = this.lap.frameX(x, z);
            double sum = 0.0;
            for (int octave = 0; octave < WARP_FREQUENCIES.length; octave++) {
                double frequency = WARP_FREQUENCIES[octave];
                OpenSimplexStandIn noise = (octave & 1) == 0 ? odd : even;
                sum += noise.eval(frameX * frequency, z * frequency, this.xPeriod * frequency,
                        this.zPeriod * frequency) * WARP_AMPLITUDES[octave];
            }

            return sum;
        }
    }

    public static boolean fill(double[] buffer, int posX, int posZ, int scaleXZ, int scaleY, int maxHeight) {
        EndTerrainLap lap = EndTerrainLap.current();
        LapIslands[] layers = layers();
        if (lap == null || layers == null) {
            return false;
        }

        ReentrantLock locker = TerrainGeneratorAccessor.toroidal$locker();
        locker.lock();
        try {
            GenerationTransformerContext.runWithTransformer(lap.fold(), () -> fillFolded(lap, layers, buffer,
                    lap.foldX(posX, posZ), lap.foldZ(posZ), scaleXZ, scaleY, maxHeight));
        } finally {
            locker.unlock();
        }

        return true;
    }

    public static @Nullable Boolean isLand(int quartX, int quartZ, int maxHeight) {
        EndTerrainLap lap = EndTerrainLap.current();
        LapIslands[] layers = layers();
        if (lap == null || layers == null) {
            return null;
        }

        long folded = FoldedQuart.fold(lap.fold(), quartX, 0, quartZ);
        int x = FoldedQuart.x(folded);
        int z = FoldedQuart.z(folded);
        ReentrantLock locker = TerrainGeneratorAccessor.toroidal$locker();
        locker.lock();
        try {
            TerrainBoolCache section = section(x, z);
            byte known = section.getData(x, z);
            if (known > 0) {
                return known > LAND_AIR;
            }

            boolean land = landFolded(lap, layers, x, z, maxHeight);
            section.setData(x, z, land ? LAND_SOLID : LAND_AIR);
            return land;
        } finally {
            locker.unlock();
        }
    }

    private static void fillFolded(EndTerrainLap lap, LapIslands[] layers, double[] buffer, int posX, int posZ,
            int scaleXZ, int scaleY, int maxHeight) {
        float fadeOutStart = maxHeight - FADE_OUT_BELOW_TOP;
        clearCaches();
        int x = posX / scaleXZ;
        int z = posZ / scaleXZ;
        Warp warp = Warp.of(lap, scaleXZ);
        double px = (double) x * scaleXZ + warp.x(x, z);
        double pz = (double) z * scaleXZ + warp.z(x, z);
        float height = TerrainGeneratorAccessor.toroidal$averageDepth(x << 1, z << 1) * DEPTH_SHARE;
        for (LapIslands layer : layers) {
            layer.toroidal$updatePositionsOnLap(lap, px, pz, maxHeight);
        }

        double xPeriod = lap.period(Direction.Axis.X, 1.0);
        double zPeriod = lap.period(Direction.Axis.Z, 1.0);
        for (int y = 0; y < buffer.length; y++) {
            double py = (double) y * scaleY;
            float dist = layers[0].toroidal$densityOnLap(px, py, pz, height);
            for (int layer = 1; layer < layers.length; layer++) {
                dist = dist > SOLID ? dist : Math.max(dist, layers[layer].toroidal$densityOnLap(px, py, pz, height));
            }

            if (dist > DETAIL_ABOVE) {
                dist = withDetail(lap, dist, px, py, pz, xPeriod, zPeriod);
            }

            if (py >= maxHeight) {
                dist = (float) AIR;
            } else if (py > fadeOutStart) {
                dist = (float) Mth.lerp((py - fadeOutStart) / FADE_OUT_DISTANCE, dist, AIR);
            }

            buffer[y] = dist;
        }
    }

    private static boolean landFolded(EndTerrainLap lap, LapIslands[] layers, int x, int z, int maxHeight) {
        int stepY = (int) Math.ceil(maxHeight / LAND_CELL_HEIGHT);
        double cellX = (x >> 1) + CELL_MIDDLE;
        double cellZ = (z >> 1) + CELL_MIDDLE;
        Warp warp = Warp.of(lap, LAND_CELL_BLOCKS);
        double px = cellX * LAND_CELL_BLOCKS + warp.x(cellX, cellZ);
        double pz = cellZ * LAND_CELL_BLOCKS + warp.z(cellX, cellZ);
        for (LapIslands layer : layers) {
            layer.toroidal$updatePositionsOnLap(lap, px, pz, maxHeight);
        }

        double xPeriod = lap.period(Direction.Axis.X, 1.0);
        double zPeriod = lap.period(Direction.Axis.Z, 1.0);
        for (int y = 0; y < stepY; y++) {
            double py = y * LAND_CELL_HEIGHT;
            float dist = layers[0].toroidal$densityOnLap(px, py, pz);
            for (int layer = 1; layer < layers.length; layer++) {
                dist = dist > SOLID ? dist : Math.max(dist, layers[layer].toroidal$densityOnLap(px, py, pz));
            }

            if (dist > DETAIL_ABOVE) {
                dist = withDetail(lap, dist, px, py, pz, xPeriod, zPeriod);
            }

            if (dist > LAND_ABOVE) {
                return true;
            }
        }

        return false;
    }

    private static float withDetail(EndTerrainLap lap, float dist, double x, double y, double z, double xPeriod,
            double zPeriod) {
        double frameX = lap.frameX(x, z);
        for (int octave = 0; octave < DETAIL_FREQUENCIES.length; octave++) {
            double frequency = DETAIL_FREQUENCIES[octave];
            OpenSimplexStandIn noise = (octave & 1) == 0 ? lap.first() : lap.second();
            double amplitude = DETAIL_AMPLITUDES[octave];
            dist += (float) (noise.eval(frameX * frequency, y * frequency, z * frequency, xPeriod * frequency,
                    zPeriod * frequency) * amplitude + amplitude);
        }

        return dist;
    }

    private static TerrainBoolCache section(int x, int z) {
        Map<Point, TerrainBoolCache> cache = TerrainGeneratorAccessor.toroidal$boolCache();
        Point pos = TerrainGeneratorAccessor.toroidal$pos();
        pos.setLocation(TerrainBoolCache.scaleCoordinate(x), TerrainBoolCache.scaleCoordinate(z));
        TerrainBoolCache section = cache.get(pos);
        if (section == null) {
            if (cache.size() > BOOL_CACHE_LIMIT) {
                cache.clear();
            }

            section = new TerrainBoolCache();
            cache.put(new Point(pos.x, pos.y), section);
        }

        return section;
    }

    private static void clearCaches() {
        TerrainGeneratorAccessor.toroidal$largeIslands().clearCache();
        TerrainGeneratorAccessor.toroidal$mediumIslands().clearCache();
        TerrainGeneratorAccessor.toroidal$smallIslands().clearCache();
    }

    private static LapIslands @Nullable [] layers() {
        IslandLayer large = TerrainGeneratorAccessor.toroidal$largeIslands();
        IslandLayer medium = TerrainGeneratorAccessor.toroidal$mediumIslands();
        IslandLayer small = TerrainGeneratorAccessor.toroidal$smallIslands();
        if (large == null || medium == null || small == null) {
            return null;
        }

        return new LapIslands[] {(LapIslands) (Object) large, (LapIslands) (Object) medium, (LapIslands) (Object) small};
    }

    private LapTerrain() {
    }
}
