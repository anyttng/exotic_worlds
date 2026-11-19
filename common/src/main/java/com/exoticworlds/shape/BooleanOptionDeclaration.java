package com.exoticworlds.shape;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;

import com.mojang.serialization.Codec;

public record BooleanOptionDeclaration(WorldOption<Boolean> option) {
    private static final boolean OFF = false;

    public static BooleanOptionDeclaration of(String key, int position) {
        return new BooleanOptionDeclaration(new WorldOption<>(key, position, Codec.BOOL, OFF));
    }

    public void register() {
        WorldOptions.register(option);
    }
}
