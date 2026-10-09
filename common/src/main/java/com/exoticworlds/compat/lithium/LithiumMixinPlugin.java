package com.exoticworlds.compat.lithium;

import java.util.List;

import org.slf4j.Logger;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class LithiumMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG = "exotic_worlds.compat.lithium.mixins.json";
    private static final String CLASS_SUFFIX = ".class";

    private static final String STORE_KEY_MIXIN = "GameEventDispatchKeyMixin";
    private static final String CLOSEST_BATCH_MIXIN = "PoiClosestBatchMixin";
    private static final String RETRY_MIXIN = "AcquirePoiRetryMixin";
    private static final List<String> OPTION_MIXINS = List.of(STORE_KEY_MIXIN, CLOSEST_BATCH_MIXIN, RETRY_MIXIN);

    private static final ModPresence LITHIUM = ModPresence.gate(LOGGER, "[lithium-compat] gate lithium_present")
            .probing("net/caffeinemc/mods/lithium/common/tracking/entity/SectionedEntityMovementTracker.class")
            .checking(CONFIG, mixin -> !OPTION_MIXINS.contains(mixin))
            .build();

    static final ModPresence STORE_KEY_GATE = optionGate("[lithium-compat] gate game_events_dispatch_present",
            STORE_KEY_MIXIN, LithiumInjectionTargets.GAME_EVENT_DISPATCHER_MIXIN);
    static final ModPresence CLOSEST_BATCH_GATE = optionGate("[lithium-compat] gate ai_poi_present",
            CLOSEST_BATCH_MIXIN, LithiumInjectionTargets.POI_MANAGER_MIXIN);
    static final ModPresence RETRY_GATE = optionGate("[lithium-compat] gate ai_poi_tasks_present",
            RETRY_MIXIN, LithiumInjectionTargets.TASKS_ACQUIRE_POI_MIXIN);

    public LithiumMixinPlugin() {
        super(LITHIUM, STORE_KEY_GATE, CLOSEST_BATCH_GATE, RETRY_GATE);
    }

    @Override
    public void onLoad(String mixinPackage) {
        MixinSquaredBootstrap.init();
        super.onLoad(mixinPackage);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (STORE_KEY_GATE.covers(mixinClassName)) {
            return LithiumGameEventDispatch.applies();
        }

        if (CLOSEST_BATCH_GATE.covers(mixinClassName)) {
            return LithiumPoiSearch.applies();
        }

        if (RETRY_GATE.covers(mixinClassName)) {
            return LithiumPoiSearch.retryApplies();
        }

        return super.shouldApplyMixin(targetClassName, mixinClassName);
    }

    private static ModPresence optionGate(String gateLabel, String mixin, String lithiumMixin) {
        String lithiumMixinClass = lithiumMixin.replace('.', '/');
        return ModPresence.gate(LOGGER, gateLabel)
                .probing(lithiumMixinClass + CLASS_SUFFIX)
                .checking(CONFIG, mixin::equals)
                .bodyFrom(mixin, lithiumMixinClass)
                .build();
    }
}
