package com.exoticworlds.engine.gen;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

public final class BaseHeights {
    static final int CAP_PER_TYPE = 1 << 18;

    private final WorldFold fold;

    private final Map<Heightmap.Types, Map<Long, Integer>> heights = new EnumMap<>(Heightmap.Types.class);

    public BaseHeights(WorldFold fold) {
        this.fold = fold;
        for (Heightmap.Types type : Heightmap.Types.values()) {
            this.heights.put(type, new ConcurrentHashMap<>());
        }
    }

    public int heightAt(int x, int z, Heightmap.Types type, IntSupplier compute) {
        long folded = this.fold.foldBlockNode(BlockPos.asLong(x, 0, z));
        Map<Long, Integer> cache = Objects.requireNonNull(this.heights.get(type), "filled for every type");

        Integer cached = cache.get(folded);
        if (cached != null) {
            return cached;
        }

        int height = compute.getAsInt();
        if (cache.size() >= CAP_PER_TYPE) {
            cache.clear();
        }
        cache.put(folded, height);
        return height;
    }
}
