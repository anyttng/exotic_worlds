package com.exoticworlds.accessors;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

public interface ClientBoundsHolder {
    default WorldFold exotic_worlds$clientBounds() {
        return WorldFolds.NOOP;
    }

    default void exotic_worlds$setClientBounds(WorldFold transformer) {
    }
}
