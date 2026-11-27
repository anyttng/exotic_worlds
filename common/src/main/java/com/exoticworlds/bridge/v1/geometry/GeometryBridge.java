package com.exoticworlds.bridge.v1.geometry;

import java.util.Map;
import java.util.Optional;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.google.common.collect.MapMaker;

import net.minecraft.world.level.Level;

public final class GeometryBridge {
    private static final Map<WorldFold, ToroidalShape> VIEWS = new MapMaker().weakKeys().makeMap();

    public static ToroidalShape of(WorldFold fold) {
        return VIEWS.computeIfAbsent(fold, ToroidalShapeView::new);
    }

    public static Optional<ToroidalShape> of(Level level) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(level);
        return fold == null ? Optional.empty() : Optional.of(of(fold));
    }

    private GeometryBridge() {
    }
}
