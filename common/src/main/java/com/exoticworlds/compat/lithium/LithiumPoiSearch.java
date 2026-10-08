package com.exoticworlds.compat.lithium;

public final class LithiumPoiSearch {
    private static final boolean APPLIES = new LithiumOptionGate("[lithium-compat] gate ai_poi",
            LithiumInjectionTargets.POI_MANAGER_MIXIN, LithiumMixinPlugin.CLOSEST_BATCH_GATE)
            .read(LithiumPoiSearch.class.getClassLoader());
    private static final boolean RETRY_APPLIES = new LithiumOptionGate("[lithium-compat] gate ai_poi_tasks",
            LithiumInjectionTargets.TASKS_ACQUIRE_POI_MIXIN, LithiumMixinPlugin.RETRY_GATE)
            .read(LithiumPoiSearch.class.getClassLoader());

    public static boolean applies() {
        return APPLIES;
    }

    public static boolean retryApplies() {
        return RETRY_APPLIES;
    }

    private LithiumPoiSearch() {
    }
}
