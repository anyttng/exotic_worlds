package com.toroidalworld.compat.lithium;

import org.slf4j.Logger;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModSymbol;

public class LithiumMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String STORE_KEY_MIXIN = "GameEventDispatchKeyMixin";
    private static final String CLOSEST_BATCH_MIXIN = "PoiClosestBatchMixin";
    private static final String RETRY_MIXIN = "AcquirePoiRetryMixin";

    private static final String LITHIUM_PACKAGE = "net/caffeinemc/mods/lithium/common/";
    private static final String TRACKER_PACKAGE = LITHIUM_PACKAGE + "tracking/entity/";
    private static final String TRACKER = TRACKER_PACKAGE + "SectionedEntityMovementTracker";
    private static final String WORLD_HELPER = LITHIUM_PACKAGE + "world/WorldHelper";
    private static final String GET_ENTITIES = "getEntities";
    private static final String GET_ENTITIES_DESCRIPTOR = "(Lnet/minecraft/world/phys/AABB;)Ljava/util/List;";

    private static final ModSymbol INVENTORY_TRACKER_QUERY = new ModSymbol(
            TRACKER_PACKAGE + "SectionedInventoryEntityMovementTracker", GET_ENTITIES, GET_ENTITIES_DESCRIPTOR);
    private static final ModSymbol ITEM_TRACKER_QUERY = new ModSymbol(
            TRACKER_PACKAGE + "SectionedItemEntityMovementTracker", GET_ENTITIES, GET_ENTITIES_DESCRIPTOR);
    private static final ModSymbol GROUP_WALK = new ModSymbol(WORLD_HELPER, "getEntitiesOfEntityGroupPlusDragonPieces",
            "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/entity/EntitySectionStorage;"
                    + "Lnet/minecraft/world/entity/Entity;L" + LITHIUM_PACKAGE + "entity/EntityClassGroup;"
                    + "Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;");
    private static final ModSymbol PUSHABLE_WALK = new ModSymbol(WORLD_HELPER, "getPushableEntities",
            "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/entity/EntitySectionStorage;"
                    + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;L" + LITHIUM_PACKAGE
                    + "entity/pushable/EntityPushablePredicate;)Ljava/util/List;");
    private static final ModSymbol ENTITY_COLLISIONS = new ModSymbol(LITHIUM_PACKAGE + "entity/LithiumEntityCollisions",
            "appendEntityCollisions",
            "(Ljava/util/List;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/phys/AABB;)V");

    private static final ModPresence LITHIUM = ModPresence.of(LOGGER, TRACKER + ".class",
            "[lithium-compat] gate lithium_present", INVENTORY_TRACKER_QUERY, ITEM_TRACKER_QUERY, GROUP_WALK,
            PUSHABLE_WALK, ENTITY_COLLISIONS);

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

        if (mixinClassName.endsWith(CLOSEST_BATCH_MIXIN)) {
            return LithiumPoiSearch.applies();
        }

        if (mixinClassName.endsWith(RETRY_MIXIN)) {
            return LithiumPoiSearch.retryApplies();
        }

        return LITHIUM.present();
    }
}
