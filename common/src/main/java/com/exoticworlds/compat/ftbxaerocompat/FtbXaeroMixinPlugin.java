package com.exoticworlds.compat.ftbxaerocompat;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class FtbXaeroMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FTB_XAERO_COMPAT_PACKAGE = "dev/satherov/ftbxaerocompat/";
    private static final String FTBCHUNKS_PACKAGE = "dev/ftb/mods/ftbchunks/";
    private static final String FTBLIBRARY_PACKAGE = "dev/ftb/mods/ftblibrary/";
    private static final String WORLDMAP_PACKAGE = "xaero/map/";

    private static final ModPresence FTBXAERO = ModPresence.gate(LOGGER, "[ew/ftbxaero-compat] gate ftbxaerocompat_present")
            .probing("dev/satherov/ftbxaerocompat/FTBXaeroCompat.class")
            .checking("exotic_worlds.compat.ftbxaerocompat.mixins.json")
            .modPackages(FTB_XAERO_COMPAT_PACKAGE, FTBCHUNKS_PACKAGE, FTBLIBRARY_PACKAGE, WORLDMAP_PACKAGE)
            .build();

    public FtbXaeroMixinPlugin() {
        super(FTBXAERO);
    }
}
