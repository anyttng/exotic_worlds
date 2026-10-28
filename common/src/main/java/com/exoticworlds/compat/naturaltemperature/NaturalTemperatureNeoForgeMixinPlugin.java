package com.exoticworlds.compat.naturaltemperature;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class NaturalTemperatureNeoForgeMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String WORLD_PAINTER = "com/naturaltemperature/mixin/WorldPainter";

    private static final ModPresence NATURAL_TEMPERATURE = ModPresence
            .gate(LOGGER, "[ew/naturaltemperature-compat] gate natural_temperature_present")
            .probing(WORLD_PAINTER + ".class")
            .checking("exotic_worlds.compat.naturaltemperature.neoforge.mixins.json")
            .bodyFrom("PaintedFieldMixin", WORLD_PAINTER)
            .build();

    public NaturalTemperatureNeoForgeMixinPlugin() {
        super(NATURAL_TEMPERATURE);
    }
}
