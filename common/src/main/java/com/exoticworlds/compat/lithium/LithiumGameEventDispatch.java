package com.exoticworlds.compat.lithium;

public final class LithiumGameEventDispatch {
    private static final boolean APPLIES = new LithiumOptionGate("[lithium-compat] gate game_events_dispatch",
            LithiumInjectionTargets.GAME_EVENT_DISPATCHER_MIXIN, LithiumInjectionTargets.NULL_DISPATCHER_HANDLER,
            "(Lnet/minecraft/world/level/gameevent/GameEventListenerRegistry;Lnet/minecraft/core/Holder;"
                    + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;"
                    + "Lnet/minecraft/world/level/gameevent/GameEventListenerRegistry$ListenerVisitor;III)Z")
            .read(LithiumGameEventDispatch.class.getClassLoader());

    public static boolean applies() {
        return APPLIES;
    }

    private LithiumGameEventDispatch() {
    }
}
