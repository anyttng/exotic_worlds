package com.exoticworlds.shape.climate;

import java.util.List;

import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.option.OptionPart;
import com.exoticworlds.core.OptionKey;
import com.exoticworlds.core.OptionKeys;
import com.exoticworlds.shape.ShapeOptionsAdapter;

import net.minecraft.network.chat.CommonComponents;

public final class CompactBiomes {
    public static final String KEY = "climate_compression";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.compact_biomes";

    private static final String FACTOR_KEY = "factor";

    private static final int POSITION = 0;

    public static final OptionKey<ClimateScale> OPTION =
            new OptionKey<>(KEY, POSITION, ClimateScale.CODEC, ClimateScale.AUTO);

    public static final DimensionOption<ClimateScale> SHOWN = DimensionOption.of(ShapeOptionsAdapter.shapeOption(OPTION), CAPTION_KEY, List.of(
            new OptionPart.Choice<>(OptionPart.OWN_CAPTION, List.of(ClimateScale.Mode.values()),
                    ClimateScale.Mode::getSerializedName,
                    mode -> mode == ClimateScale.Mode.OFF ? CommonComponents.OPTION_OFF : null,
                    ClimateScale::mode, ClimateScale::withMode, scale -> true),
            new OptionPart.WholeNumber<>(FACTOR_KEY, ClimateScale.CUSTOM_MIN, ClimateScale.CUSTOM_MAX, ClimateScale::factor,
                    (scale, factor) -> ClimateScale.custom(factor),
                    scale -> scale.mode() == ClimateScale.Mode.CUSTOM)));

    public static void register() {
        OptionKeys.register(OPTION);
        SHOWN.register();
    }

    private CompactBiomes() {
    }
}
