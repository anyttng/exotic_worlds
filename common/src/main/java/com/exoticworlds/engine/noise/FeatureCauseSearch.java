package com.exoticworlds.engine.noise;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import net.minecraft.world.level.levelgen.placement.FixedPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

final class FeatureCauseSearch {
    private static final String VANILLA_PACKAGE = "net.minecraft.";
    private static final String PLACER_CLASS = FeaturePlacer.class.getName();
    private static final String FEATURE_CLASS = Feature.class.getName();

    record Lookups(Registry<PlacedFeature> placed, Registry<Feature> features) {
    }

    static @Nullable SeamCause causeOf(FeatureRecording here, FeatureRecording there, Lookups lookups) {
        List<FeatureRecording.Entry> hereEntries = here.entries();
        List<FeatureRecording.Entry> thereEntries = there.entries();
        int shared = Math.min(hereEntries.size(), thereEntries.size());
        for (int index = 0; index < shared; index++) {
            FeatureRecording.Entry hereEntry = hereEntries.get(index);
            FeatureRecording.Entry thereEntry = thereEntries.get(index);
            if (hereEntry.placed() != thereEntry.placed()) {
                return new SeamCause.NotFound();
            }

            if (hereEntry.pastTheReach() || thereEntry.pastTheReach()
                    || hereEntry.steps().equals(thereEntry.steps())) {
                continue;
            }

            return causeIn(hereEntry, thereEntry.steps(), lookups);
        }

        return hereEntries.size() == thereEntries.size() ? null : new SeamCause.NotFound();
    }

    private static @Nullable SeamCause causeIn(FeatureRecording.Entry here, List<FeatureRecording.Step> there,
            Lookups lookups) {
        List<FeatureRecording.Step> steps = here.steps();
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

                Identifier type = BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.getKey(modifier.codec());
                Identifier holder = lookups.placed().getKey(here.placed());
                return nodeOf(type, holder, modifier.getClass().getName(), PLACER_CLASS);
            }

            if (hereStep instanceof FeatureRecording.Place(var feature, long pos, boolean answer, var _)
                    && thereStep instanceof FeatureRecording.Place(var thereFeature, long therePos,
                            boolean thereAnswer, var _)
                    && feature == thereFeature && pos == therePos) {
                if (answer == thereAnswer) {
                    return null;
                }

                Identifier type = BuiltInRegistries.FEATURE_TYPE.getKey(feature.codec());
                Identifier holder = lookups.features().getKey(feature);
                return nodeOf(type, holder != null ? holder : lookups.placed().getKey(here.placed()),
                        feature.getClass().getName(), FEATURE_CLASS);
            }

            return new SeamCause.NotFound();
        }

        return new SeamCause.NotFound();
    }

    private static SeamCause.Node nodeOf(@Nullable Identifier type, @Nullable Identifier holderKey, String className,
            String mixedClass) {
        String holder = holderKey != null ? holderKey.toString() : null;
        boolean vanilla = type != null
                ? type.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
                : className.startsWith(VANILLA_PACKAGE);
        List<String> candidates = vanilla
                ? AppliedMixins.foreignOn(List.of(className, mixedClass))
                : List.of();
        return new SeamCause.Node(type != null ? type.toString() : className, holder,
                type != null ? type.getNamespace() : null, vanilla, candidates);
    }

    private FeatureCauseSearch() {
    }
}
