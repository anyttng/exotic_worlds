package com.exoticworlds.bridge.v2.feature;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.api.v2.feature.FeatureV2;
import com.exoticworlds.api.v2.feature.OptionFeatureV2;
import com.exoticworlds.api.v2.feature.OptionPartV2;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.registry.StartupRegistry;
import com.exoticworlds.shape.feature.ShapeFeature;
import com.exoticworlds.shape.option.OptionPart;

import net.minecraft.resources.Identifier;

public final class FeatureBridge {
    private static final StartupRegistry<Identifier, ShapeFeature> FEATURES = new StartupRegistry<>("Features");

    public static void register(FeatureV2 feature) {
        ShapeFeature internal = feature instanceof OptionFeatureV2<?> offered
                ? new BridgedOptionFeature<>(offered)
                : new BridgedFeature(feature);
        FEATURES.register(feature.getId(), internal);
        if (internal instanceof BridgedOptionFeature<?> offered) {
            ShapeOptionRegistry.register(offered.getOption());
        }

        ShapeFeatures.register(internal);
    }

    public static ShapeFeature internalOf(FeatureV2 feature) {
        ShapeFeature internal = FEATURES.getEntries().get(feature.getId());
        if (internal == null) {
            throw new IllegalStateException("The feature " + feature.getId() + " was never registered:"
                    + " call FeaturesV2.register for it while the mod is constructed.");
        }

        return internal;
    }

    @SuppressWarnings("unchecked")
    public static <T> ShapeOption<T> optionOf(OptionFeatureV2<T> feature) {
        return ((BridgedOptionFeature<T>) internalOf(feature)).getOption();
    }

    static String keyOf(FeatureV2 feature) {
        return feature.getId().toString();
    }

    public static <T> List<OptionPart<T>> toInternal(List<OptionPartV2<T>> parts) {
        List<OptionPart<T>> converted = new ArrayList<>(parts.size());
        for (OptionPartV2<T> part : parts) {
            converted.add(toInternal(part));
        }

        return converted;
    }

    private static <T> OptionPart<T> toInternal(OptionPartV2<T> part) {
        return switch (part) {
            case OptionPartV2.Toggle<T> toggle ->
                    new OptionPart.Toggle<>(toggle.key(), toggle.read(), toggle.write(), toggle.shownWhen());
            case OptionPartV2.Choice<T, ?> choice -> toInternal(choice);
            case OptionPartV2.WholeNumber<T> number -> new OptionPart.WholeNumber<>(number.key(), number.min(),
                    number.max(), number.read(), number.write(), number.shownWhen());
            case OptionPartV2.Row<T> row -> new OptionPart.Row<>(toInternal(row.parts()));
        };
    }

    private static <T, V> OptionPart<T> toInternal(OptionPartV2.Choice<T, V> choice) {
        return new OptionPart.Choice<>(choice.key(), choice.values(), choice.valueKey(), value -> null, choice.read(),
                choice.write(), choice.shownWhen());
    }

    private FeatureBridge() {
    }
}
