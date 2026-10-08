package com.exoticworlds.compat.simpleatlas;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class SimpleAtlasMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MAP_SELECTOR = "rubbertoe/simple_atlas/map/AtlasMapSelector";

    private static final String WORLD_POINT = "rubbertoe/simple_atlas/client/screen/icon/AtlasIcon$WorldPoint";

    private static final String POINT_COORD = "()D";

    static final ModSymbol POINT_X = new ModSymbol(WORLD_POINT, "x", POINT_COORD);

    static final ModSymbol POINT_Z = new ModSymbol(WORLD_POINT, "z", POINT_COORD);

    static final ModSymbol CURRENT_MAP = new ModSymbol(MAP_SELECTOR, "findCurrentMapRawId",
            "(Lnet/minecraft/world/level/Level;DDLjava/util/List;Ljava/lang/Integer;)Ljava/lang/Integer;");

    static final ModSymbol CONTAINS_POSITION = new ModSymbol(MAP_SELECTOR, "mapContainsPosition",
            "(Lnet/minecraft/world/level/Level;DDLnet/minecraft/world/level/saveddata/maps/MapItemSavedData;)Z");

    static final ModSymbol TO_DECORATION = new ModSymbol("rubbertoe/simple_atlas/server/AtlasWaypointDecorations",
            "toDecoration", "(Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;"
                    + "Lrubbertoe/simple_atlas/component/AtlasContents$WaypointData;)"
                    + "Lnet/minecraft/world/level/saveddata/maps/MapDecoration;");

    static final ModSymbol WAYPOINT_ON_MAP = new ModSymbol("rubbertoe/simple_atlas/network/ModNetworking",
            "isWaypointOnMap", "(Lrubbertoe/simple_atlas/component/AtlasContents$WaypointData;"
                    + "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;)Z");

    static final ModSymbol SCREEN_TO_WORLD = new ModSymbol("rubbertoe/simple_atlas/client/screen/AtlasScreen",
            "screenToWorldPoint", "(DDFFFLjava/util/List;Ljava/lang/String;)"
                    + "Lrubbertoe/simple_atlas/client/screen/AtlasScreen$WorldPoint;");

    static final ModSymbol[] SYMBOLS = {POINT_X, POINT_Z, CURRENT_MAP, CONTAINS_POSITION, TO_DECORATION,
            WAYPOINT_ON_MAP, SCREEN_TO_WORLD};

    private static final ModPresence SIMPLE_ATLAS = ModPresence.of(LOGGER, MAP_SELECTOR + ".class",
            "[simpleatlas-compat] gate simpleatlas_present", SYMBOLS);

    public SimpleAtlasMixinPlugin() {
        super(SIMPLE_ATLAS);
    }
}
