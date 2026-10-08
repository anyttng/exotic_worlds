package com.exoticworlds.compat.terrablender;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class TerraBlenderMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol ZOOM_RESULT = new ModSymbol(
            "terrablender/worldgen/noise/AreaContext", "createResult",
            "(Lterrablender/worldgen/noise/PixelTransformer;Lterrablender/worldgen/noise/Area;)"
                    + "Lterrablender/worldgen/noise/Area;");

    private static final ModPresence TERRABLENDER = ModPresence.of(LOGGER,
            "terrablender/worldgen/noise/Area.class",
            "[terrablender-compat] gate terrablender_present", ZOOM_RESULT);

    public TerraBlenderMixinPlugin() {
        super(TERRABLENDER);
    }
}
