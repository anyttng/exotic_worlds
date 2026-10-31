package com.exoticworlds.compat.reterraforged.preview;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class ReTerraForgedPreviewMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RETERRAFORGED_PACKAGE = "raccoonman/reterraforged/";

    private static final ModPresence PREVIEW = ModPresence.gate(LOGGER,
                    "[ew/reterraforged-compat] gate reterraforged_preview_present")
            .probing("raccoonman/reterraforged/client/gui/screen/presetconfig/Preview2D.class")
            .checking("exotic_worlds.compat.reterraforged.preview.mixins.json")
            .modPackages(RETERRAFORGED_PACKAGE)
            .build();

    public ReTerraForgedPreviewMixinPlugin() {
        super(PREVIEW);
    }
}
