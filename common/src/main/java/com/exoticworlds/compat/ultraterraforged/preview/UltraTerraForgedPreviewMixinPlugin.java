package com.exoticworlds.compat.ultraterraforged.preview;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class UltraTerraForgedPreviewMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ULTRATERRAFORGED_PACKAGE = "com/pandaismyname1/ultraterraforged/";

    private static final ModPresence PREVIEW = ModPresence.gate(LOGGER,
                    "[ew/ultraterraforged-compat] gate ultraterraforged_preview_present")
            .probing("com/pandaismyname1/ultraterraforged/client/gui/createworld/TerrainPreview.class")
            .checking("exotic_worlds.compat.ultraterraforged.preview.mixins.json")
            .modPackages(ULTRATERRAFORGED_PACKAGE)
            .build();

    public UltraTerraForgedPreviewMixinPlugin() {
        super(PREVIEW);
    }
}
