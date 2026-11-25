package com.exoticworlds.client.shape.feature.compactbiomes;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

import com.ascendiacore.gui.widget.label.Label;
import com.ascendiacore.gui.widget.label.model.LabelRole;
import com.exoticworlds.client.shape.feature.FeaturePanel;
import com.exoticworlds.client.shape.feature.FeatureView;
import com.exoticworlds.client.shape.feature.FeatureViews;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class CompactBiomesView implements FeatureView<ClimateScale> {
    private static final Component FACTOR =
            Component.translatable("gui.exotic_worlds.world_shape_settings.compact_biomes.factor");
    private static final String FRACTIONAL_FACTOR_FORMAT = "%.2f";

    private record Shown(ClimateScale scale, FlatShape spans) {
    }

    public static void register() {
        FeatureViews.register(CompactBiomes.FEATURE.getOption(), new CompactBiomesView());
    }

    @Override
    public void extend(FeaturePanel<ClimateScale> panel) {
        Map<Shown, Component> lines = new HashMap<>();
        Label label = new Label(panel.getFill(), LabelRole.MUTED, Component.empty());
        label.setText(() -> line(panel, lines));
        label.setVisible(() -> panel.isOffered() && !line(panel, lines).getString().isEmpty());
        panel.add(label);
    }

    private static Component line(FeaturePanel<ClimateScale> panel, Map<Shown, Component> lines) {
        Screen creation = panel.getCreationScreen();
        ClimateScale scale = panel.getValue();
        FlatShape spans = panel.getSpans();
        if (creation == null || scale == null || spans == null || scale.mode() == ClimateScale.Mode.OFF
                || scale.mode() == ClimateScale.Mode.CUSTOM) {
            return Component.empty();
        }

        return lines.computeIfAbsent(new Shown(scale, spans), shown -> factorLine(creation, shown));
    }

    private static Component factorLine(Screen creation, Shown shown) {
        OptionalDouble factor = ClimateFactorPreview.temperatureFactor(creation,
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), shown.scale()), shown.spans());
        if (factor.isEmpty()) {
            return Component.empty();
        }

        double value = factor.getAsDouble();
        return CommonComponents.optionNameValue(FACTOR, Component.literal(value == Math.rint(value)
                ? Integer.toString((int) value)
                : String.format(Locale.ROOT, FRACTIONAL_FACTOR_FORMAT, value)));
    }

    private CompactBiomesView() {
    }
}
