package com.exoticworlds.api.v2.feature;

import com.exoticworlds.api.v2.ExperimentalV2;

/**
 * The moment a chunk has its terrain filled and carved, before features are placed. It runs on a worldgen thread, and
 * only the chunk itself may be touched.
 */
@ExperimentalV2
public interface CarvedChunkMomentV2 extends LevelMomentV2 {
    /** Called once for each chunk that reaches this step. */
    void atCarved(ChunkV2 chunk);
}
