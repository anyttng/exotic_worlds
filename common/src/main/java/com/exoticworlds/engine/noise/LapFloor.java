package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;

enum LapFloor {
    FOUR_CELLS(4L),
    TWO_CELLS(2L),
    HELD(PeriodicNoiseSampler.HELD_PERIOD);

    final long period;

    LapFloor(long period) {
        this.period = period;
    }

    static LapFloor of(WorldFold transformer) {
        return transformer.blockLattice().bothLoop() ? TWO_CELLS : HELD;
    }
}
