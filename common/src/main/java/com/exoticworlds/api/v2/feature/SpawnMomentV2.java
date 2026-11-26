package com.exoticworlds.api.v2.feature;

import java.util.Optional;

import net.minecraft.world.level.ChunkPos;

/**
 * The moment a new world looks for its spawn. Where several features of one shape take this moment, the first
 * registered one that is on decides.
 */
public interface SpawnMomentV2 {
    /**
     * The chunk the search starts from, or empty to search as the game does without Exotic Worlds.
     */
    Optional<ChunkPos> spawnChunkIn(SpawnV2 spawn);
}
