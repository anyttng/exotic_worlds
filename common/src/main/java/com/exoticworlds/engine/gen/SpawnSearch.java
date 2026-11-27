package com.exoticworlds.engine.gen;

import java.util.function.Supplier;

import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.feature.SpawnFeature;
import com.exoticworlds.core.feature.SpawnMoment;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.engine.ThreadScope;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

public final class SpawnSearch {
    private static final ThreadScope<SpawnPoint> BOUND = new ThreadScope<>();

    private static final ThreadScope<SpawnSurface> SURFACE = new ThreadScope<>();

    public static ChunkPos from(ChunkGenerator generator, Supplier<ChunkPos> search) {
        SpawnPoint point = chosenSpawnOf(generator);
        return BOUND.with(point.isCustom() ? point : null, search);
    }

    private static SpawnPoint chosenSpawnOf(ChunkGenerator generator) {
        CarriedShape carried = GeneratorShapes.getCarriedShape(generator);
        if (carried == null) {
            return SpawnPoint.DEFAULT;
        }

        return ShapeFeatures.activeAt(carried, LevelStem.OVERWORLD, SpawnFeature.class).stream()
                .findFirst()
                .map(feature -> feature.spawnPointIn(new SpawnMoment(carried.getFold(), carried.getOptions())))
                .orElse(SpawnPoint.DEFAULT);
    }

    public static BlockPos overSurface(ChunkGenerator generator, Supplier<BlockPos> search) {
        return SURFACE.with(SpawnSurface.of(GeneratorShapes.getFold(generator)), search);
    }

    public static SpawnPoint getBound() {
        SpawnPoint point = BOUND.getCurrent();
        return point == null ? SpawnPoint.DEFAULT : point;
    }

    public static BlockPos walked(BlockPos searchOrigin, int blockX, int blockZ) {
        SpawnSurface surface = SURFACE.getCurrent();
        return surface == null
                ? new BlockPos(blockX, searchOrigin.getY(), blockZ)
                : surface.walked(searchOrigin, blockX, blockZ);
    }

    public static long distanceX(int blockX, int blockZ) {
        SpawnSurface surface = SURFACE.getCurrent();
        SpawnPoint point = getBound();
        return (long) (surface == null ? blockX : surface.nearestTo(point, blockX, blockZ).getX()) - point.getBlockX();
    }

    public static long distanceZ(int blockX, int blockZ) {
        SpawnSurface surface = SURFACE.getCurrent();
        SpawnPoint point = getBound();
        return (long) (surface == null ? blockZ : surface.nearestTo(point, blockX, blockZ).getZ()) - point.getBlockZ();
    }

    private SpawnSearch() {
    }
}
