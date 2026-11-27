package com.exoticworlds.api.v2.feature;

import com.exoticworlds.api.v2.ExperimentalV2;

/**
 * The moment a chunk's light is done, when its neighbors' terrain is known. It runs on a worldgen thread.
 */
@ExperimentalV2
public interface LitChunkMomentV2 extends LevelMomentV2 {
    /** Called once for each chunk that reaches this step. */
    void atLit(LitChunkV2 chunk);
}
