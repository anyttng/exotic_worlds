package com.exoticworlds.shape.feature.compactbiomes;

import java.util.List;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.shape.feature.OptionFeature;
import com.exoticworlds.shape.option.OptionPart;

import net.minecraft.network.chat.CommonComponents;

public final class CompactBiomes extends OptionFeature<ClimateScale> {
    public static final String KEY = "climate_compression";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.compact_biomes";

    private static final String FACTOR_KEY = "factor";

    private static final int POSITION = 0;

    public static final CompactBiomes FEATURE = new CompactBiomes();

    private CompactBiomes() {
        super(new ShapeOption<>(KEY, POSITION, ClimateScale.CODEC, ClimateScale.AUTO), CAPTION_KEY, List.of(
                new OptionPart.Choice<>(OptionPart.OWN_CAPTION, List.of(ClimateScale.Mode.values()),
                        ClimateScale.Mode::getSerializedName,
                        mode -> mode == ClimateScale.Mode.OFF ? CommonComponents.OPTION_OFF : null,
                        ClimateScale::mode, ClimateScale::withMode, scale -> true),
                new OptionPart.WholeNumber<>(FACTOR_KEY, ClimateScale.CUSTOM_MIN, ClimateScale.CUSTOM_MAX,
                        ClimateScale::factor, (scale, factor) -> ClimateScale.custom(factor),
                        scale -> scale.mode() == ClimateScale.Mode.CUSTOM)));
    }
}
