package com.exoticworlds.accessors;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

public interface TransformerHolder {
    default WorldFold exotic_worlds$transformer() {
        return WorldFolds.NOOP;
    }

    default void exotic_worlds$setTransformer(WorldFold transformer) {
    }
}
