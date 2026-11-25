package com.exoticworlds.engine.gen;

import java.util.function.Supplier;

import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.engine.ThreadScope;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class SpawnSearch {
    private static final ThreadScope<SpawnPoint> BOUND = new ThreadScope<>();

    public static ChunkPos from(ChunkGenerator generator, Supplier<ChunkPos> search) {
        SpawnPoint point = SpawnPoint.of(GeneratorShapes.getFold(generator));
        return BOUND.with(point.isCustom() ? point : null, search);
    }

    public static SpawnPoint getBound() {
        SpawnPoint point = BOUND.getCurrent();
        return point == null ? SpawnPoint.DEFAULT : point;
    }

    private SpawnSearch() {
    }
}
