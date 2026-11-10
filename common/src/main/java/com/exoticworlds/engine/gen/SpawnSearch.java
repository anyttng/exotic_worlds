package com.exoticworlds.engine.gen;

import java.util.function.Supplier;

import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.engine.ThreadScope;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class SpawnSearch {
    private static final ThreadScope<SpawnPoint> BOUND = new ThreadScope<>();

    public static ChunkPos from(ChunkGenerator generator, Supplier<ChunkPos> search) {
        SpawnPoint point = SpawnPoint.of(ShapedChunkGenerator.foldOf(generator));
        return BOUND.with(point.equals(SpawnPoint.ZERO) ? null : point, search);
    }

    public static SpawnPoint bound() {
        SpawnPoint point = BOUND.current();
        return point == null ? SpawnPoint.ZERO : point;
    }

    private SpawnSearch() {
    }
}
