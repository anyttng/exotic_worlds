package com.exoticworlds.compat.supplementaries;

import org.slf4j.Logger;

import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;
import com.mojang.logging.LogUtils;

public class SupplementariesMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUPPLEMENTARIES_PACKAGE = "net/mehvahdjukaar/supplementaries/";
    private static final String MOONLIGHT_PACKAGE = "net/mehvahdjukaar/moonlight/";

    static final ModPresence SUPPLEMENTARIES =ModPresence.gate(LOGGER,
                    "[ew/supplementaries-compat] gate supplementaries_present")
            .probing("net/mehvahdjukaar/supplementaries/common/misc/map_data/WeatheredHandler$WeatheredMapData.class")
            .checking("exotic_worlds.compat.supplementaries.mixins.json")
            .modPackages(SUPPLEMENTARIES_PACKAGE, MOONLIGHT_PACKAGE)
            .build();

    public SupplementariesMixinPlugin() {
        super(SUPPLEMENTARIES);
    }
}
