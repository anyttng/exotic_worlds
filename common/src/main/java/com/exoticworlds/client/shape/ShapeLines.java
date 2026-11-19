package com.exoticworlds.client.shape;

import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.network.chat.Component;

public final class ShapeLines {
    private static final String PRESET_KEY_PREFIX = "gui.exotic_worlds.toroidal_settings.preset.";

    public static Component presetLabel(WorldLoopPresets preset) {
        return Component.translatable(PRESET_KEY_PREFIX + preset.id());
    }

    private ShapeLines() {
    }
}
