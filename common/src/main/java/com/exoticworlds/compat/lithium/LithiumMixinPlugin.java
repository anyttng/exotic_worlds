package com.exoticworlds.compat.lithium;

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

    private static final ModPresence LITHIUM = ModPresence.gate(LOGGER, "[lithium-compat] gate lithium_present")
            .probing("net/caffeinemc/mods/lithium/common/tracking/entity/SectionedEntityMovementTracker.class")
            .checking(CONFIG, mixin -> !mixin.equals(STORE_KEY_MIXIN))
            .build();

    static final ModPresence STORE_KEY_GATE = optionGate("[lithium-compat] gate game_events_dispatch_present",
            STORE_KEY_MIXIN, LithiumInjectionTargets.GAME_EVENT_DISPATCHER_MIXIN);

    public LithiumMixinPlugin() {
        super(LITHIUM, STORE_KEY_GATE);
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
