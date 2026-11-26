package com.exoticworlds.core.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ActiveFeaturesHolder;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.registry.NamespaceOrder;
import com.exoticworlds.core.registry.StartupRegistry;
import com.exoticworlds.core.world.CarriedShape;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.LevelStem;

public final class ShapeFeatures {
    private record Registered(String namespace, GenerationFeature feature) {
    }

    private record RunOrder(Map<String, Registered> source, List<GenerationFeature> features) {
    }

    private static final StartupRegistry<String, Registered> FEATURES = new StartupRegistry<>("Shape features");

    private static final StartupRegistry<Identifier, Map<String, DimensionScope>> SCOPES =
            new StartupRegistry<>("Shape feature scopes");

    private static volatile RunOrder runOrder = new RunOrder(Map.of(), List.of());

    public static void register(String namespace, GenerationFeature feature) {
        FEATURES.register(feature.getKey(), new Registered(namespace, feature));
    }

    private static List<GenerationFeature> inRunOrder() {
        Map<String, Registered> registered = FEATURES.getEntries();
        RunOrder held = runOrder;
        if (held.source() != registered) {
            held = new RunOrder(registered, NamespaceOrder.sorted(registered.values(), Registered::namespace).stream()
                    .map(Registered::feature)
                    .toList());
            runOrder = held;
        }

        return held.features();
    }

    public static void attach(Identifier shape, GenerationFeature feature, DimensionScope scope) {
        Registered registered = FEATURES.getEntries().get(feature.getKey());
        if (registered == null || registered.feature() != feature) {
            throw new IllegalStateException("The feature " + feature.getKey() + " is attached to the shape " + shape
                    + " before it is registered, so it has no place in the feature order.");
        }

        SCOPES.merge(shape, Map.of(feature.getKey(), scope), (held, added) -> merged(shape, held, added));
    }

    public static Set<Identifier> getShapes() {
        return SCOPES.getEntries().keySet();
    }

    public static List<GenerationFeature> activeFeatures(CarriedShape carried, ResourceKey<LevelStem> dimension) {
        Map<String, DimensionScope> scopes = carried.findShapeId()
                .map(shape -> SCOPES.getEntries().get(shape))
                .orElse(Map.of());
        return inRunOrder().stream()
                .filter(feature -> isInScope(scopes.get(feature.getKey()), dimension))
                .filter(feature -> feature.isOn(carried.getOptions()))
                .toList();
    }

    public static boolean isAttached(Identifier shape, GenerationFeature feature, ResourceKey<LevelStem> dimension) {
        Map<String, DimensionScope> scopes = SCOPES.getEntries().get(shape);
        return scopes != null && isInScope(scopes.get(feature.getKey()), dimension);
    }

    public static <F extends GenerationFeature> List<F> activeAt(CarriedShape carried, ResourceKey<LevelStem> dimension,
            Class<F> moment) {
        return atMoment(activeFeatures(carried, dimension), moment);
    }

    public static <F extends GenerationFeature> List<F> registeredAt(Class<F> moment) {
        return atMoment(inRunOrder(), moment);
    }

    public static <F extends GenerationFeature> List<F> activeOn(ServerLevel level, Class<F> moment) {
        return atMoment(((ActiveFeaturesHolder) level).exotic_worlds$getActiveFeatures(), moment);
    }

    public static List<GenerationFeature> installedOn(ServerLevel level) {
        return GeneratorShapes.activeFeatures(level.getChunkSource().getGenerator(), level.dimension()).stream()
                .filter(feature -> isInstalledOn(feature, level))
                .toList();
    }

    public static <F extends GenerationFeature> List<F> atMoment(List<GenerationFeature> features, Class<F> moment) {
        return features.stream()
                .map(feature -> feature.asMoment(moment))
                .filter(Objects::nonNull)
                .toList();
    }

    private static boolean isInstalledOn(GenerationFeature feature, ServerLevel level) {
        LevelFeature installed = feature.asMoment(LevelFeature.class);
        return installed == null || installed.isInstalledOn(level);
    }

    private static boolean isInScope(@Nullable DimensionScope scope, ResourceKey<LevelStem> dimension) {
        return scope != null && scope.contains(dimension);
    }

    private static Map<String, DimensionScope> merged(Identifier shape, Map<String, DimensionScope> held,
            Map<String, DimensionScope> added) {
        Map<String, DimensionScope> merged = new HashMap<>(held);
        added.forEach((key, scope) -> {
            if (merged.putIfAbsent(key, scope) != null) {
                throw new IllegalStateException("The shape " + shape + " already carries the feature " + key
                        + ": attach a feature to a shape once.");
            }
        });
        return Map.copyOf(merged);
    }

    private ShapeFeatures() {
    }
}
