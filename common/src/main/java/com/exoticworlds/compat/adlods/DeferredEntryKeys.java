package com.exoticworlds.compat.adlods;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class DeferredEntryKeys {
    public static void fold(Map<ChunkPos, Map<BlockPos, BlockPos>> keyed, WorldFold fold) {
        for (ChunkPos key : List.copyOf(keyed.keySet())) {
            ChunkPos folded = fold.fold(key);
            if (folded.equals(key)) {
                continue;
            }

            Map<BlockPos, BlockPos> points = keyed.remove(key);
            if (points != null) {
                keyed.computeIfAbsent(folded, chunk -> new ConcurrentHashMap<>()).putAll(points);
            }
        }
    }

    private DeferredEntryKeys() {
    }
}
