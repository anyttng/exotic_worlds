package com.exoticworlds.compat.create;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class CreateMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CREATE_PACKAGE = "com/zurrtum/create/";
    private static final String STATION_MAP_MARKER_MIXIN = "StationMapMarkerMixin";
    private static final String MAP_ITEM_SAVED_DATA_MIXIN = "com/zurrtum/create/mixin/MapItemSavedDataMixin";

    static final ModPresence CREATE = ModPresence.gate(LOGGER, "[ew/create-compat] gate create_present")
            .probing("com/zurrtum/create/Create.class")
            .checking("exotic_worlds.compat.create.mixins.json")
            .modPackages(CREATE_PACKAGE)
            .bodyFrom(STATION_MAP_MARKER_MIXIN, MAP_ITEM_SAVED_DATA_MIXIN)
            .build();

    public CreateMixinPlugin() {
        super(CREATE);
    }
}
