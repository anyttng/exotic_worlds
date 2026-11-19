package com.exoticworlds.api.v1;

import com.exoticworlds.core.WorldFold;

public final class TestShapes {
    public static ToroidalShape of(WorldFold fold) {
        return fold.shapeView();
    }

    private TestShapes() {
    }
}
