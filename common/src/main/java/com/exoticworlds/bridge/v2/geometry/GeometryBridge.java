package com.exoticworlds.bridge.v2.geometry;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.google.common.collect.MapMaker;

import net.minecraft.world.level.Level;

public final class GeometryBridge {
    private static final Map<WorldFold, WorldGeometryV2> VIEWS = new MapMaker().weakKeys().makeMap();

    public static WorldGeometryV2 of(WorldFold fold) {
        return VIEWS.computeIfAbsent(fold, WorldGeometryView::new);
    }

    public static Optional<WorldGeometryV2> of(Level level) {
        return ofNullable(WorldLoopAttachments.getWrappedFold(level));
    }

    public static Optional<WorldGeometryV2> ofNullable(@Nullable WorldFold fold) {
        return fold == null ? Optional.empty() : Optional.of(of(fold));
    }

    private GeometryBridge() {
    }
}
