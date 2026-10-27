package com.exoticworlds.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.dimension.LevelStem;

public final class CreationShape {
    private static final String FINGERPRINT_PREFIX = "|exotic_worlds=";

    private static final String UNSHAPED = FINGERPRINT_PREFIX + "none";

    private static final Map<CarriedShape, String> FINGERPRINTS = new ConcurrentHashMap<>();

    private static boolean sessionOpen;

    private static @Nullable Chosen kept;

    private record Chosen(WorldCreationContext settings, WorldShape shape, @Nullable CarriedShape carried,
            String fingerprint) {
    }

    public static void openSession() {
        sessionOpen = true;
        kept = null;
    }

    public static void closeSession() {
        sessionOpen = false;
        kept = null;
    }

    public static void forgetFingerprints() {
        FINGERPRINTS.clear();
    }

    public static @Nullable WorldFold fold(WorldCreationContext settings) {
        CarriedShape carried = chosen(settings).carried();
        return carried != null ? carried.fold() : null;
    }

    public static String fingerprint(WorldCreationContext settings) {
        return chosen(settings).fingerprint();
    }

    public static <T> Supplier<T> underRouterBuild(WorldCreationContext settings, Supplier<T> build) {
        WorldFold fold = fold(settings);
        return () -> GenerationFoldContext.withRouterBuild(fold, build);
    }

    public static Runnable underRouterBuild(@Nullable WorldFold fold, Runnable task) {
        return () -> GenerationFoldContext.withRouterBuild(fold, () -> {
            task.run();
            return null;
        });
    }

    public static WorldFold routerBuildFold() {
        WorldFold fold = GenerationFoldContext.context().routerBuildFold();
        return fold != null ? fold : WorldFolds.NOOP;
    }

    private static Chosen chosen(WorldCreationContext settings) {
        WorldShape shape = WorldShapes.selected();
        Chosen last = kept;
        if (last != null && last.settings() == settings && last.shape() == shape) {
            return last;
        }

        CarriedShape carried = carried(settings);
        Chosen chosen = new Chosen(settings, shape, carried, carried == null
                ? UNSHAPED
                : FINGERPRINTS.computeIfAbsent(carried, key -> FINGERPRINT_PREFIX + FINGERPRINTS.size()));
        if (sessionOpen) {
            kept = chosen;
        }
        return chosen;
    }

    public static @Nullable CarriedShape carried(WorldCreationContext settings) {
        return WorldShapes.applyAtCreation(settings.worldgenLoadContext(), settings.selectedDimensions())
                .get(LevelStem.OVERWORLD)
                .map(stem -> ShapedChunkGenerator.carriedShapeOf(stem.generator()))
                .orElse(null);
    }

    private CreationShape() {
    }
}
