package com.exoticworlds.shape.climate;

import com.exoticworlds.api.v1.client.WorldOptionControls;
import com.exoticworlds.client.shape.climate.CompactBiomesControl;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;

public final class CompactBiomes {
    public static final String KEY = "climate_compression";

    private static final int POSITION = 0;

    public static final WorldOption<ClimateScale> OPTION = new WorldOption<>(
            KEY, POSITION, ClimateScale.CODEC, ClimateScale.AUTO);

    public static void register(boolean client) {
        WorldOptions.register(OPTION);

        if (client) {
            WorldOptionControls.register(OPTION, CompactBiomesControl::new);
        }
    }

    private CompactBiomes() {
    }
}
