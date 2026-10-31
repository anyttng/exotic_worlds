package com.exoticworlds.compat.naturaltemperature;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class NaturalTemperatureMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NATURAL_TEMPERATURE_PACKAGE = "com/naturaltemperature/";

    private static final ModPresence NATURAL_TEMPERATURE = ModPresence
            .gate(LOGGER, "[ew/naturaltemperature-compat] gate natural_temperature_present")
            .probing("com/naturaltemperature/Painter/PainterCore.class")
            .checking("exotic_worlds.compat.naturaltemperature.mixins.json")
            .modPackages(NATURAL_TEMPERATURE_PACKAGE)
            .build();

    public NaturalTemperatureMixinPlugin() {
        super(NATURAL_TEMPERATURE);
    }
}
