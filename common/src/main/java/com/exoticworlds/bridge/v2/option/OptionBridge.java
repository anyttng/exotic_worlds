package com.exoticworlds.bridge.v2.option;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.api.v2.option.DimensionOptionV2;
import com.exoticworlds.api.v2.option.OptionPartV2;
import com.exoticworlds.api.v2.option.ShapeOptionV2;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.shape.option.DeclaredOptions;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.DimensionScope;
import com.exoticworlds.shape.option.OptionPart;

public final class OptionBridge {

    public static void register(DimensionOptionV2<?> declared) {
        DeclaredOptions.register(new DeclaredOptions.Entry(internal(declared), declared.getShapes(),
                declared.getDimensions().isEmpty() ? DimensionScope.ALL : DimensionScope.of(declared.getDimensions())));
    }

    public static void register(ShapeOptionV2<?> option) {
        ShapeOptionRegistry.register(keyOf(option));
    }

    private static <T> DimensionOption<T> internal(DimensionOptionV2<T> declared) {
        List<OptionPart<T>> parts = new ArrayList<>(declared.getParts().size());
        for (OptionPartV2<T> part : declared.getParts()) {
            parts.add(internal(part));
        }

        return DimensionOption.of(keyOf(declared.getOption()), declared.getCaptionKey(), parts);
    }

    private static <T> OptionPart<T> internal(OptionPartV2<T> part) {
        return switch (part) {
            case OptionPartV2.Toggle<T> toggle ->
                    new OptionPart.Toggle<>(toggle.key(), toggle.read(), toggle.write(), toggle.shownWhen());
            case OptionPartV2.Choice<T, ?> choice -> internal(choice);
            case OptionPartV2.WholeNumber<T> number -> new OptionPart.WholeNumber<>(number.key(), number.min(),
                    number.max(), number.read(), number.write(), number.shownWhen());
        };
    }

    private static <T, V> OptionPart<T> internal(OptionPartV2.Choice<T, V> choice) {
        return new OptionPart.Choice<>(choice.key(), choice.values(), choice.valueKey(), choice.ownName(),
                choice.read(), choice.write(), choice.shownWhen());
    }

    private static <T> ShapeOption<T> keyOf(ShapeOptionV2<T> option) {
        return new ShapeOption<>(option.key(), option.position(), option.codec(), option.defaultValue());
    }

    private OptionBridge() {
    }
}
