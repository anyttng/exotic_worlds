package com.exoticworlds.shape.feature.compactbiomes.client;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

import com.exoticworlds.shape.feature.client.FeaturePanel;
import com.exoticworlds.shape.feature.client.FeatureView;
import com.exoticworlds.shape.feature.client.FeatureViews;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;

import net.minecraft.client.gui.screens.Screen;

public final class CompactBiomesView implements FeatureView<ClimateScale> {
    private static final String FRACTIONAL_FACTOR_FORMAT = "%.2f";
    private static final String OFF_FACTOR = "1";
    private static final String UNKNOWN_FACTOR = "";

    private record Shown(ClimateScale scale, FlatShape spans) {
    }

    public static void register() {
        FeatureViews.register(CompactBiomes.FEATURE.getOption(), new CompactBiomesView());
    }

    @Override
    public void extendPanel(FeaturePanel<ClimateScale> panel) {
        Map<Shown, String> factors = new HashMap<>();
        panel.setDisabledNumberText(CompactBiomes.FACTOR_KEY, scale -> factorText(panel, scale, factors));
    }

    private static String factorText(FeaturePanel<ClimateScale> panel, ClimateScale scale, Map<Shown, String> factors) {
        if (scale.mode() == ClimateScale.Mode.OFF) {
            return OFF_FACTOR;
        }

        Screen creation = panel.getCreationScreen();
        FlatShape spans = panel.getSpans();
        if (creation == null || spans == null || scale.mode() == ClimateScale.Mode.CUSTOM) {
            return UNKNOWN_FACTOR;
        }

        return factors.computeIfAbsent(new Shown(scale, spans), shown -> previewFactor(creation, shown));
    }

    private static String previewFactor(Screen creation, Shown shown) {
        OptionalDouble factor = ClimateFactorPreview.temperatureFactor(creation,
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), shown.scale()), shown.spans());
        if (factor.isEmpty()) {
            return UNKNOWN_FACTOR;
        }

        double value = factor.getAsDouble();
        return value == Math.rint(value)
                ? Integer.toString((int) value)
                : String.format(Locale.ROOT, FRACTIONAL_FACTOR_FORMAT, value);
    }

    private CompactBiomesView() {
    }
}
