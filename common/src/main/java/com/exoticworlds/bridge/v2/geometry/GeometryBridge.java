package com.exoticworlds.bridge.v2.geometry;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.geometry.WorldGeometry;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.google.common.collect.MapMaker;

import net.minecraft.world.level.Level;

public final class GeometryBridge {
    private static final Map<WorldFold, WorldGeometry> VIEWS = new MapMaker().weakKeys().makeMap();

    public static WorldGeometry of(WorldFold fold) {
        return VIEWS.computeIfAbsent(fold, WorldGeometryView::new);
    }

    public static Optional<WorldGeometry> of(Level level) {
        return ofNullable(WorldLoopAttachments.wrappedFoldOf(level));
    }

    public static Optional<WorldGeometry> ofNullable(@Nullable WorldFold fold) {
        return fold == null ? Optional.empty() : Optional.of(of(fold));
    }

    private GeometryBridge() {
    }
}
