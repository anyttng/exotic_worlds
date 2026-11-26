package com.exoticworlds.api.v2.feature;

/**
 * The moment a dimension's noise state is built. A feature measures the terrain here, before any chunk exists, and
 * settles what its other moments will use.
 */
public interface RandomStateMomentV2 {
    /** Called once for each noise state built. */
    void atRandomState(RandomStateV2 state);
}
