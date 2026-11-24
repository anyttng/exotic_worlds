package com.exoticworlds.accessors;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;

public interface ClientBoundsHolder {
    default WorldFold exotic_worlds$getClientBounds() {
        return WorldFolds.NOOP;
    }

    default void exotic_worlds$setClientBounds(WorldFold fold) {
    }
}
