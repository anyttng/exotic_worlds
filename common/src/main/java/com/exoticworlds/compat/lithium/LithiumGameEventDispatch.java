package com.exoticworlds.compat.lithium;

public final class LithiumGameEventDispatch {
    private static final boolean APPLIES = new LithiumOptionGate("[lithium-compat] gate game_events_dispatch",
            LithiumInjectionTargets.GAME_EVENT_DISPATCHER_MIXIN, LithiumMixinPlugin.STORE_KEY_GATE)
            .read(LithiumGameEventDispatch.class.getClassLoader());

    public static boolean applies() {
        return APPLIES;
    }

    private LithiumGameEventDispatch() {
    }
}
