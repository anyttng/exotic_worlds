package com.exoticworlds.shape;

import com.exoticworlds.api.v1.client.WorldOptionControls;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.client.shape.BooleanOptionControl;

import com.mojang.serialization.Codec;

public record BooleanOptionDeclaration(WorldOption<Boolean> option, String labelKey) {
    private static final boolean OFF = false;

    public static BooleanOptionDeclaration of(String key, int position, String labelKey) {
        return new BooleanOptionDeclaration(new WorldOption<>(key, position, Codec.BOOL, OFF), labelKey);
    }

    public void register(boolean client) {
        WorldOptions.register(option);

        if (client) {
            WorldOptionControls.register(option, context -> new BooleanOptionControl(context, option, labelKey));
        }
    }
}
