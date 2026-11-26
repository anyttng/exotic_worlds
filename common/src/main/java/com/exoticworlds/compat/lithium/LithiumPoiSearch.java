package com.exoticworlds.compat.lithium;

public final class LithiumPoiSearch {
    private static final boolean IS_APPLIED = new LithiumOptionGate("[ew/lithium-compat] gate ai_poi",
            LithiumInjectionTargets.POI_MANAGER_MIXIN, LithiumMixinPlugin.CLOSEST_BATCH_GATE)
            .read(LithiumPoiSearch.class.getClassLoader());
    private static final boolean IS_RETRY_APPLIED = new LithiumOptionGate("[ew/lithium-compat] gate ai_poi_tasks",
            LithiumInjectionTargets.TASKS_ACQUIRE_POI_MIXIN, LithiumMixinPlugin.RETRY_GATE)
            .read(LithiumPoiSearch.class.getClassLoader());

    public static boolean isApplied() {
        return IS_APPLIED;
    }

    public static boolean isRetryApplied() {
        return IS_RETRY_APPLIED;
    }

    private LithiumPoiSearch() {
    }
}
