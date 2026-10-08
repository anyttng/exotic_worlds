package com.exoticworlds.accessors;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

public interface TransformerHolder {
    default WorldFold toroidal$transformer() {
        return WorldFolds.NOOP;
    }

    default void toroidal$setTransformer(WorldFold transformer) {
    }
}
