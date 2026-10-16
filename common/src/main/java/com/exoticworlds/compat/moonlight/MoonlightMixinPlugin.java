package com.exoticworlds.compat.moonlight;

import org.slf4j.Logger;

import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.mojang.logging.LogUtils;

public class MoonlightMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence MOONLIGHT = ModPresence.gate(LOGGER, "[moonlight-compat] gate moonlight_present")
            .probing("net/mehvahdjukaar/moonlight/api/map/decoration/MLMapMarker.class")
            .checking("exotic_worlds.compat.moonlight.mixins.json")
            .bodyFrom("MarkerToggleWindowMixin", "net/mehvahdjukaar/moonlight/core/mixins/MapDataMixin")
            .build();

    public MoonlightMixinPlugin() {
        super(MOONLIGHT);
    }
}
