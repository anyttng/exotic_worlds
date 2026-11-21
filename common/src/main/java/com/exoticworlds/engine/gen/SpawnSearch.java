package com.exoticworlds.engine.gen;

import java.util.function.Supplier;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.engine.ThreadScope;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class SpawnSearch {
    private static final ThreadScope<SpawnPoint> BOUND = new ThreadScope<>();

    private static final ThreadScope<SpawnSurface> SURFACE = new ThreadScope<>();

    public static ChunkPos from(ChunkGenerator generator, Supplier<ChunkPos> search) {
        SpawnPoint point = SpawnPoint.of(ShapedChunkGenerator.foldOf(generator));
        return BOUND.with(point.isCustom() ? point : null, search);
    }

    public static BlockPos overSurface(ChunkGenerator generator, Supplier<BlockPos> search) {
        SpawnSurface surface = SpawnSurface.of(ShapedChunkGenerator.foldOf(generator));
        BlockPos found = SURFACE.with(surface, search);
        if (surface != null) {
            ExoticWorlds.LOGGER.info(
                    "[ew/spawn] surface_search block_x={} block_z={} on_face={} walked_points={} off_net_points={}",
                    found.getX(), found.getZ(), surface.isOnFace(found.getX(), found.getZ()),
                    surface.walkedPoints(), surface.offNetPoints());
        }

        return found;
    }

    public static SpawnPoint bound() {
        SpawnPoint point = BOUND.current();
        return point == null ? SpawnPoint.DEFAULT : point;
    }

    public static BlockPos walked(BlockPos searchOrigin, int blockX, int blockZ) {
        SpawnSurface surface = SURFACE.current();
        return surface == null
                ? new BlockPos(blockX, searchOrigin.getY(), blockZ)
                : surface.walked(searchOrigin, blockX, blockZ);
    }

    public static long distanceX(int blockX, int blockZ) {
        SpawnSurface surface = SURFACE.current();
        SpawnPoint point = bound();
        return (long) (surface == null ? blockX : surface.nearestTo(point, blockX, blockZ).getX()) - point.blockX();
    }

    public static long distanceZ(int blockX, int blockZ) {
        SpawnSurface surface = SURFACE.current();
        SpawnPoint point = bound();
        return (long) (surface == null ? blockZ : surface.nearestTo(point, blockX, blockZ).getZ()) - point.blockZ();
    }

    private SpawnSearch() {
    }
}
