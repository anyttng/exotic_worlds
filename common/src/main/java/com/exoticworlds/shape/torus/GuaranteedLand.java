package com.exoticworlds.shape.torus;

import com.exoticworlds.api.v1.client.WorldOptionControls;
import com.exoticworlds.client.shape.BooleanOptionControl;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;

import com.mojang.serialization.Codec;

public final class GuaranteedLand {
    public static final String KEY = "guaranteed_land";

    private static final String LABEL_KEY = "gui.exotic_worlds.toroidal_settings.guaranteed_land";

    private static final int POSITION = 1;

    private static final boolean OFF = false;

    public static final WorldOption<Boolean> OPTION = new WorldOption<>(
            KEY, POSITION, Codec.BOOL, OFF);

    public static void register(boolean client) {
        WorldOptions.register(OPTION);

        if (client) {
            WorldOptionControls.register(OPTION, context -> new BooleanOptionControl(context, OPTION, LABEL_KEY));
        }
    }

    private GuaranteedLand() {
    }
}
