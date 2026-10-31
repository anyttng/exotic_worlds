package com.exoticworlds.compat.xaero;

import java.util.List;
import java.util.stream.Stream;

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
    private static final String GUI_MAP_REGION_MIXIN = WORLDMAP_MIXIN_PACKAGE + "GuiMapRegionMixin";
    private static final String GUI_MAP_VIEW_MIXIN = WORLDMAP_MIXIN_PACKAGE + "GuiMapViewMixin";
    private static final String MINIMAP_PACKAGE = "xaero/common/";
    private static final String HUD_PACKAGE = "xaero/hud/";
    private static final String WORLDMAP_PACKAGE = "xaero/map/";
    private static final String XAERO_LIB_PACKAGE = "xaero/lib/";
    private static final List<String> WORLDMAP_CLASSES = Stream.of(XaeroCursorLap.class, XaeroExportAssembly.class,
                    XaeroMapView.class, XaeroRegionFrame.class, XaeroRegionLoads.class, XaeroWorldMapFold.class,
                    XaeroFrameBudget.class)
            .map(Class::getSimpleName)
            .toList();
    private static final String BOTH_CLASS = XaeroMinimapTiles.class.getSimpleName();

    private static final ModPresence XAERO_MINIMAP = ModPresence.gate(LOGGER,
                    "[ew/xaero-compat] gate xaero_minimap_present")
            .probing(MINIMAP_RESOURCE)
            .checking(CONFIG, mixin -> !mixin.startsWith(WORLDMAP_MIXIN_PACKAGE))
            .modPackages(MINIMAP_PACKAGE, HUD_PACKAGE)
            .scanning(name -> !WORLDMAP_CLASSES.contains(name) && !name.equals(BOTH_CLASS))
            .build();

    private static final ModPresence XAERO_WORLDMAP = ModPresence.gate(LOGGER,
                    "[ew/xaero-compat] gate xaero_worldmap_present")
            .probing(WORLDMAP_RESOURCE)
            .checking(CONFIG, mixin -> mixin.startsWith(WORLDMAP_MIXIN_PACKAGE)
                    && !mixin.equals(SUPPORT_WORLDMAP_MIXIN))
            .withholding(GUI_MAP_REGION_MIXIN, XaeroFrameBudget::exceeded)
            .withholding(GUI_MAP_VIEW_MIXIN, XaeroFrameBudget::exceeded)
            .modPackages(WORLDMAP_PACKAGE, XAERO_LIB_PACKAGE)
            .scanning(WORLDMAP_CLASSES::contains)
            .build();

    private static final ModPresence XAERO_BOTH = ModPresence.gate(LOGGER,
                    "[ew/xaero-compat] gate xaero_minimap_worldmap_present")
            .probing(MINIMAP_RESOURCE, WORLDMAP_RESOURCE)
            .checking(CONFIG, SUPPORT_WORLDMAP_MIXIN::equals)
            .modPackages(MINIMAP_PACKAGE, HUD_PACKAGE, WORLDMAP_PACKAGE, XAERO_LIB_PACKAGE)
            .scanning(BOTH_CLASS::equals)
            .build();

    public XaeroMixinPlugin() {
        super(XAERO_MINIMAP, XAERO_WORLDMAP, XAERO_BOTH);
    }
}
