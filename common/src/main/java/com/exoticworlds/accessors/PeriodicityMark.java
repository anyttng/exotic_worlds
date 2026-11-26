package com.exoticworlds.accessors;

import java.util.concurrent.atomic.AtomicReference;

import com.exoticworlds.engine.noise.check.SeamVerdict;

public interface PeriodicityMark {
    AtomicReference<SeamVerdict> exotic_worlds$getSeamVerdict();

    AtomicReference<SeamVerdict> exotic_worlds$getFullSeamVerdict();
}
