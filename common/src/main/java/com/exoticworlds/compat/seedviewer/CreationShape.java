package com.exoticworlds.compat.seedviewer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.dimension.LevelStem;

public final class CreationShape {
    private static final String FINGERPRINT_PREFIX = "|exotic_worlds=";

    private static final String UNSHAPED = FINGERPRINT_PREFIX + "none";

    private static final Map<CarriedShape, String> FINGERPRINTS = new ConcurrentHashMap<>();

    public static @Nullable WorldFold fold(WorldCreationContext settings) {
        CarriedShape carried = overworld(settings);
        return carried != null ? carried.fold() : null;
    }

    public static String fingerprint(WorldCreationContext settings) {
        CarriedShape carried = overworld(settings);
        return carried == null
                ? UNSHAPED
                : FINGERPRINTS.computeIfAbsent(carried, shape -> FINGERPRINT_PREFIX + FINGERPRINTS.size());
    }

    public static WorldFold routerBuildFold() {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        return fold != null ? fold : WorldFolds.NOOP;
    }

    private static @Nullable CarriedShape overworld(WorldCreationContext settings) {
        return WorldShapes.applyAtCreation(settings.worldgenLoadContext(), settings.selectedDimensions())
                .get(LevelStem.OVERWORLD)
                .map(stem -> ShapedChunkGenerator.carriedShapeOf(stem.generator()))
                .orElse(null);
    }

    private CreationShape() {
    }
}
