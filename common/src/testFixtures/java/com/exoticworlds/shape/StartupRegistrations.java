package com.exoticworlds.shape;

import java.util.List;

import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.layout.LayoutShape;

public final class StartupRegistrations {
    public static synchronized void options() {
        if (!ShapeOptionRegistry.getAll().contains(SpawnPoint.OPTION)) {
            WorldOptionSetup.registerAll();
        }
    }

    public static synchronized void layoutShape() {
        if (WorldShapes.getShapes().stream().noneMatch(shape -> shape.id().equals(LayoutShape.ID))) {
            LayoutShape.register(false);
        }
    }

    public static List<NoiseFeature> noiseFeaturesFor(ShapeOptions options) {
        options();
        return ShapeFeatures.registeredAt(NoiseFeature.class).stream()
                .filter(feature -> feature.isOn(options))
                .toList();
    }

    private StartupRegistrations() {
    }
}
