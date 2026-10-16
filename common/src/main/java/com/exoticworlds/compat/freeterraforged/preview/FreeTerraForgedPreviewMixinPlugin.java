package com.exoticworlds.compat.freeterraforged.preview;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class FreeTerraForgedPreviewMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence PREVIEW = ModPresence.gate(LOGGER,
                    "[ew/freeterraforged-compat] gate freeterraforged_preview_present")
            .probing("etcodehome/freeterraforged/client/gui/screen/presetconfig/IPreviewHandler.class")
            .checking("exotic_worlds.compat.freeterraforged.preview.mixins.json")
            .build();

    public FreeTerraForgedPreviewMixinPlugin() {
        super(PREVIEW);
    }
}
