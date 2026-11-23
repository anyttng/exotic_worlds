package com.exoticworlds.bridge.v1.geometry;

import java.util.Map;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.WorldFold;
import com.google.common.collect.MapMaker;

public final class GeometryBridge {
    private static final Map<WorldFold, ToroidalShape> VIEWS = new MapMaker().weakKeys().makeMap();

    public static ToroidalShape of(WorldFold fold) {
        return VIEWS.computeIfAbsent(fold, ToroidalShapeView::new);
    }

    private GeometryBridge() {
    }
}
