package com.exoticworlds.accessors;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

public interface FoldHolder {
    default WorldFold exotic_worlds$fold() {
        return WorldFolds.NOOP;
    }

    default void exotic_worlds$setFold(WorldFold fold) {
    }
}
