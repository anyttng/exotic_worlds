package com.exoticworlds.compat.lithium;

public final class LithiumGameEventDispatch {
    // A game type is spelled per loader on this line, so the handler is looked up in both spellings.
    private static final boolean APPLIES = new LithiumOptionGate("[lithium-compat] gate game_events_dispatch",
            LithiumInjectionTargets.GAME_EVENT_DISPATCHER_MIXIN, LithiumInjectionTargets.NULL_DISPATCHER_HANDLER,
            "(Lnet/minecraft/world/level/gameevent/GameEventListenerRegistry;Lnet/minecraft/core/Holder;"
                    + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;"
                    + "Lnet/minecraft/world/level/gameevent/GameEventListenerRegistry$ListenerVisitor;III)Z",
            "(Lnet/minecraft/class_5713;Lnet/minecraft/class_6880;Lnet/minecraft/class_243;"
                    + "Lnet/minecraft/class_5712$class_7397;Lnet/minecraft/class_5713$class_7721;III)Z")
            .read(LithiumGameEventDispatch.class.getClassLoader());

    public static boolean applies() {
        return APPLIES;
    }

    private LithiumGameEventDispatch() {
    }
}
