package com.exoticworlds.accessors;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

public interface ClientBoundsHolder {
    default WorldFold toroidal$clientBounds() {
        return WorldFolds.NOOP;
    }

    default void toroidal$setClientBounds(WorldFold transformer) {
    }
}
