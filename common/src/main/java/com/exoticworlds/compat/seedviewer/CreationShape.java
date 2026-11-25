package com.exoticworlds.compat.seedviewer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.dimension.LevelStem;

public final class CreationShape {
    private static final String FINGERPRINT_PREFIX = "|exotic_worlds=";

    private static final Map<CarriedShape, String> FINGERPRINTS = new ConcurrentHashMap<>();

    private static boolean sessionOpen;

    private static @Nullable Chosen kept;

    private record Chosen(WorldCreationContext settings, @Nullable CarriedShape carried,
            @Nullable String fingerprint) {
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
        return carried != null ? carried.getFold() : null;
    }

    public static List<GenerationFeature> activeFeatures(WorldCreationContext settings) {
        CarriedShape carried = chosen(settings).carried();
        return carried != null ? ShapeFeatures.activeFeatures(carried, LevelStem.OVERWORLD) : List.of();
    }

    public static String fingerprinted(String foreign, WorldCreationContext settings) {
        String fingerprint = chosen(settings).fingerprint();
        return fingerprint != null ? foreign + fingerprint : foreign;
    }

    public static WorldFold routerBuildFold() {
        WorldFold fold = GenerationFoldContext.getContext().getRouterBuildFold();
        return fold != null ? fold : WorldFolds.NOOP;
    }

    private static Chosen chosen(WorldCreationContext settings) {
        Chosen last = kept;
        if (last != null && last.settings() == settings) {
            return last;
        }

        CarriedShape carried = overworld(settings);
        Chosen chosen = new Chosen(settings, carried, carried == null
                ? null
                : FINGERPRINTS.computeIfAbsent(carried, key -> FINGERPRINT_PREFIX + FINGERPRINTS.size()));
        if (sessionOpen) {
            kept = chosen;
        }
        return chosen;
    }

    private static @Nullable CarriedShape overworld(WorldCreationContext settings) {
        return WorldShapes.applyAtCreation(settings.worldgenLoadContext(), settings.selectedDimensions())
                .get(LevelStem.OVERWORLD)
                .map(stem -> GeneratorShapes.getCarriedShape(stem.generator()))
                .orElse(null);
    }

    private CreationShape() {
    }
}
