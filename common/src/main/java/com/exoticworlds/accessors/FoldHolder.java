package com.exoticworlds.accessors;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;

public interface FoldHolder {
    default WorldFold exotic_worlds$getFold() {
        return WorldFolds.NOOP;
    }

    default void exotic_worlds$setFold(WorldFold fold) {
    }
}
