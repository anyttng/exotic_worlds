package com.exoticworlds.compat.lithium;

import org.slf4j.Logger;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.mojang.logging.LogUtils;
import com.exoticworlds.MixinGatePlugin;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModSymbol;

public class LithiumMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String STORE_KEY_MIXIN = "GameEventDispatchKeyMixin";

    private static final String LITHIUM_PACKAGE = "net/caffeinemc/mods/lithium/common/";
    private static final String TRACKER_PACKAGE = LITHIUM_PACKAGE + "tracking/entity/";
    private static final String TRACKER = TRACKER_PACKAGE + "SectionedEntityMovementTracker";
    private static final String CONSTRUCTOR = "<init>";
    private static final String TRACKER_CONSTRUCTOR_DESCRIPTOR =
            "(Lnet/caffeinemc/mods/lithium/common/util/tuples/WorldSectionBox;Ljava/lang/Class;)V";

    // A game type is spelled per loader on this line, so a target class is gated by a member naming none.
    private static final ModSymbol INVENTORY_TRACKER_CONSTRUCTOR = new ModSymbol(
            TRACKER_PACKAGE + "SectionedInventoryEntityMovementTracker", CONSTRUCTOR, TRACKER_CONSTRUCTOR_DESCRIPTOR);
    private static final ModSymbol ITEM_TRACKER_CONSTRUCTOR = new ModSymbol(
            TRACKER_PACKAGE + "SectionedItemEntityMovementTracker", CONSTRUCTOR, TRACKER_CONSTRUCTOR_DESCRIPTOR);
    private static final ModSymbol GROUP_FILTER_SWITCH = new ModSymbol(LITHIUM_PACKAGE + "world/WorldHelper",
            "CUSTOM_TYPE_FILTERABLE_LIST_DISABLED", "Z");
    private static final ModSymbol COLLISION_EPSILON = new ModSymbol(LITHIUM_PACKAGE + "entity/LithiumEntityCollisions",
            "EPSILON", "D");

    private static final ModPresence LITHIUM = ModPresence.of(LOGGER, TRACKER + ".class",
            "[lithium-compat] gate lithium_present", INVENTORY_TRACKER_CONSTRUCTOR, ITEM_TRACKER_CONSTRUCTOR,
            GROUP_FILTER_SWITCH, COLLISION_EPSILON);

    @Override
    public void onLoad(String mixinPackage) {
        MixinSquaredBootstrap.init();
        LITHIUM.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(STORE_KEY_MIXIN)) {
            return LithiumGameEventDispatch.applies();
        }

        return LITHIUM.present();
    }
}
