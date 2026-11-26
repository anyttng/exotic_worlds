package com.exoticworlds.api.v1.option;

import java.util.List;

import com.exoticworlds.bridge.v1.option.OptionBridge;

public final class WorldOptions {
    public static <T> WorldOption<T> register(WorldOption<T> option) {
        return OptionBridge.register(option);
    }

    public static List<WorldOption<?>> all() {
        return OptionBridge.getAll();
    }

    private WorldOptions() {
    }
}
