package com.exoticworlds.engine.noise.check;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.FixedPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

final class FeatureCauseSearch {
    private static final String VANILLA_PACKAGE = "net.minecraft.";
    private static final String PLACER_CLASS = PlacedFeature.class.getName();
    private static final String FEATURE_CLASS = Feature.class.getName();

    record Lookups(Registry<PlacedFeature> placed, Registry<ConfiguredFeature<?, ?>> features) {
    }

    static @Nullable SeamCause causeOf(FeatureRecording here, FeatureRecording there, Lookups lookups) {
        List<FeatureRecording.Entry> hereEntries = here.getEntries();
        List<FeatureRecording.Entry> thereEntries = there.getEntries();
        int shared = Math.min(hereEntries.size(), thereEntries.size());
        for (int index = 0; index < shared; index++) {
            FeatureRecording.Entry hereEntry = hereEntries.get(index);
            FeatureRecording.Entry thereEntry = thereEntries.get(index);
            if (hereEntry.getPlaced() != thereEntry.getPlaced()) {
                return new SeamCause.NotFound();
            }

            if (hereEntry.getSteps().equals(thereEntry.getSteps())) {
                continue;
            }

            if (!hereEntry.isPastTheReach() && !thereEntry.isPastTheReach()) {
                return causeIn(hereEntry, thereEntry.getSteps(), lookups);
            }

            if (!writtenBy(hereEntry).equals(writtenBy(thereEntry))) {
                return null;
            }
        }

        return hereEntries.size() == thereEntries.size() ? null : new SeamCause.NotFound();
    }

    private static Map<Long, BlockState> writtenBy(FeatureRecording.Entry entry) {
        Map<Long, BlockState> written = new HashMap<>();
        for (FeatureRecording.Step step : entry.getSteps()) {
            if (step instanceof FeatureRecording.Place place) {
                written.putAll(place.written());
            }
        }

        return written;
    }

    private static @Nullable SeamCause causeIn(FeatureRecording.Entry here, List<FeatureRecording.Step> there,
            Lookups lookups) {
        List<FeatureRecording.Step> steps = here.getSteps();
        int shared = Math.min(steps.size(), there.size());
        for (int index = 0; index < shared; index++) {
            FeatureRecording.Step hereStep = steps.get(index);
            FeatureRecording.Step thereStep = there.get(index);
            if (hereStep.equals(thereStep)) {
                continue;
            }

            if (hereStep instanceof FeatureRecording.Modify(var modifier, long input, var _)
                    && thereStep instanceof FeatureRecording.Modify(var thereModifier, long thereInput, var _)
                    && modifier == thereModifier && input == thereInput) {
                if (modifier instanceof FixedPlacement) {
                    return null;
                }

                Identifier type = BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.getKey(modifier.type());
                Identifier holder = lookups.placed().getKey(here.getPlaced());
                return nodeOf(type, holder, modifier.getClass().getName(), PLACER_CLASS);
            }

            if (hereStep instanceof FeatureRecording.Place(var feature, long pos, boolean isPlaced, var _)
                    && thereStep instanceof FeatureRecording.Place(var thereFeature, long therePos,
                            boolean isPlacedThere, var _)
                    && feature == thereFeature && pos == therePos) {
                if (isPlaced == isPlacedThere) {
                    return null;
                }

                Identifier type = BuiltInRegistries.FEATURE.getKey(feature.feature());
                Identifier holder = lookups.features().getKey(feature);
                return nodeOf(type, holder != null ? holder : lookups.placed().getKey(here.getPlaced()),
                        feature.feature().getClass().getName(), FEATURE_CLASS);
            }

            return new SeamCause.NotFound();
        }

        return new SeamCause.NotFound();
    }

    private static SeamCause.Node nodeOf(@Nullable Identifier type, @Nullable Identifier holderKey, String className,
            String mixedClass) {
        String holder = holderKey != null ? holderKey.toString() : null;
        boolean isVanilla = type != null
                ? type.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
                : className.startsWith(VANILLA_PACKAGE);
        List<String> candidates = isVanilla
                ? AppliedMixins.foreignOn(List.of(className, mixedClass))
                : List.of();
        return new SeamCause.Node(type != null ? type.toString() : className, holder,
                type != null ? type.getNamespace() : null, isVanilla, candidates);
    }

    private FeatureCauseSearch() {
    }
}
