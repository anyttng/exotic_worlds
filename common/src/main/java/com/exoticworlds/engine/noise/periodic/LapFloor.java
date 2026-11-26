package com.exoticworlds.engine.noise.periodic;

import com.exoticworlds.core.fold.WorldFold;

public enum LapFloor {
    FOUR_CELLS(4L),
    TWO_CELLS(2L),
    HELD(PeriodicNoiseSampler.HELD_PERIOD);

    public final long period;

    LapFloor(long period) {
        this.period = period;
    }

    public static LapFloor of(WorldFold fold) {
        return fold.getBlockLattice().areBothLooped() ? TWO_CELLS : HELD;
    }
}
