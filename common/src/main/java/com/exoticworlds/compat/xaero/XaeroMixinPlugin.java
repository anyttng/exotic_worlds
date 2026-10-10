package com.exoticworlds.compat.xaero;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class XaeroMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG = "exotic_worlds.compat.xaero.mixins.json";
    private static final String MINIMAP_RESOURCE = "xaero/common/HudMod.class";
    private static final String WORLDMAP_RESOURCE = "xaero/map/WorldMap.class";
    private static final String WORLDMAP_MIXIN_PACKAGE = "map.";
    private static final String SUPPORT_WORLDMAP_MIXIN = WORLDMAP_MIXIN_PACKAGE + "SupportXaeroWorldmapMixin";

    private static final ModPresence XAERO_MINIMAP = ModPresence.gate(LOGGER,
                    "[xaero-compat] gate xaero_minimap_present")
            .probing(MINIMAP_RESOURCE)
            .checking(CONFIG, mixin -> !mixin.startsWith(WORLDMAP_MIXIN_PACKAGE))
            .build();

    private static final ModPresence XAERO_WORLDMAP = ModPresence.gate(LOGGER,
                    "[xaero-compat] gate xaero_worldmap_present")
            .probing(WORLDMAP_RESOURCE)
            .checking(CONFIG, mixin -> mixin.startsWith(WORLDMAP_MIXIN_PACKAGE)
                    && !mixin.equals(SUPPORT_WORLDMAP_MIXIN))
            .build();

    private static final ModPresence XAERO_BOTH = ModPresence.gate(LOGGER,
                    "[xaero-compat] gate xaero_minimap_worldmap_present")
            .probing(MINIMAP_RESOURCE, WORLDMAP_RESOURCE)
            .checking(CONFIG, SUPPORT_WORLDMAP_MIXIN::equals)
            .build();

    public XaeroMixinPlugin() {
        super(XAERO_MINIMAP, XAERO_WORLDMAP, XAERO_BOTH);
    }
}
