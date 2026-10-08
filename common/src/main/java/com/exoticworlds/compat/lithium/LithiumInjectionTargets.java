package com.exoticworlds.compat.lithium;

public final class LithiumInjectionTargets {
    public static final String GAME_EVENT_DISPATCHER_MIXIN =
            "net.caffeinemc.mods.lithium.mixin.world.game_events.dispatch.GameEventDispatcherMixin";
    public static final String NULL_DISPATCHER_HANDLER = "handleNullDispatcher";
    public static final String POI_MANAGER_MIXIN = "net.caffeinemc.mods.lithium.mixin.ai.poi.PoiManagerMixin";
    public static final String CLOSEST_BATCH = "lithium$getNClosestFirstWithType";
    public static final String TASKS_ACQUIRE_POI_MIXIN = "net.caffeinemc.mods.lithium.mixin.ai.poi.tasks.AcquirePoiMixin";
    public static final String RETRY_MARKER_LAMBDA = "lambda$getNClosestFirstWithType$1";
    public static final String DISTANCES_DISTANCE_SQ =
            "Lnet/caffeinemc/mods/lithium/common/util/Distances;distanceSq(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)J";

    private LithiumInjectionTargets() {
    }
}
