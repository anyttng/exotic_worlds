package com.exoticworlds.shape.dimension;

import java.util.List;
import java.util.stream.Stream;

import com.exoticworlds.core.feature.DimensionScope;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.registry.NamespaceOrder;
import com.exoticworlds.core.registry.StartupRegistry;
import com.exoticworlds.shape.feature.OptionFeature;
import com.exoticworlds.shape.feature.ShapeFeature;
import com.exoticworlds.shape.option.ScopedOption;

import net.minecraft.resources.Identifier;

public final class AttachedFeatures {
    private record Line(String namespace, ScopedOption option) {
    }

    private static final StartupRegistry<Identifier, List<Line>> OPTIONS =
            new StartupRegistry<>("Shape option lines");

    public static void attach(Identifier shape, ShapeFeature feature, DimensionScope scope) {
        ShapeFeatures.attach(shape, feature, scope);
        if (feature instanceof OptionFeature<?> offered) {
            Line line = new Line(feature.getNamespace(), new ScopedOption(offered.getShown(), scope));
            OPTIONS.merge(shape, List.of(line), (held, added) -> Stream.concat(held.stream(), added.stream()).toList());
        }
    }

    public static List<ScopedOption> optionsOf(Identifier shape) {
        List<Line> lines = OPTIONS.getEntries().getOrDefault(shape, List.of());
        return NamespaceOrder.sorted(lines, Line::namespace).stream().map(Line::option).toList();
    }

    private AttachedFeatures() {
    }
}
