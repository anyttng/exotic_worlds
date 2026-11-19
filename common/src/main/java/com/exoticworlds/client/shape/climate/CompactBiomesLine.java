package com.exoticworlds.client.shape.climate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class CompactBiomesLine {
    private static final Component FACTOR =
            Component.translatable("gui.exotic_worlds.world_shape_settings.compact_biomes.factor");
    private static final String FRACTIONAL_FACTOR_FORMAT = "%.2f";

    private final Map<Shown, Component> lines = new HashMap<>();
    private @Nullable Screen linesFor;

    private record Shown(ClimateScale scale, LoopSpans spans) {
    }

    public Component line(Screen shownOn, @Nullable Screen creation, ClimateScale scale, LoopSpans spans) {
        if (creation == null || scale.mode() == ClimateScale.Mode.OFF || scale.mode() == ClimateScale.Mode.CUSTOM) {
            return Component.empty();
        }

        if (this.linesFor != shownOn) {
            this.lines.clear();
            this.linesFor = shownOn;
        }

        return this.lines.computeIfAbsent(new Shown(scale, spans), shown -> factorLine(creation, shown));
    }

    private static Component factorLine(Screen creation, Shown shown) {
        OptionalDouble factor = ClimateFactorPreview.temperatureFactor(creation,
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, shown.scale()), shown.spans());
        if (factor.isEmpty()) {
            return Component.empty();
        }

        double value = factor.getAsDouble();
        return CommonComponents.optionNameValue(FACTOR, Component.literal(value == Math.rint(value)
                ? Integer.toString((int) value)
                : String.format(Locale.ROOT, FRACTIONAL_FACTOR_FORMAT, value)));
    }
}
