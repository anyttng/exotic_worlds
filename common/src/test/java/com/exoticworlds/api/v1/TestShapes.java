package com.exoticworlds.api.v1;

import com.exoticworlds.bridge.v1.geometry.GeometryBridge;
import com.exoticworlds.core.WorldFold;

public final class TestShapes {
    public static ToroidalShape of(WorldFold fold) {
        return GeometryBridge.of(fold);
    }

    private TestShapes() {
    }
}
