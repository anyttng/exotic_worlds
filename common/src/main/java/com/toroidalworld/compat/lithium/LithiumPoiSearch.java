package com.toroidalworld.compat.lithium;

public final class LithiumPoiSearch {
    private static final boolean APPLIES = new LithiumOptionGate("[lithium-compat] gate ai_poi",
            LithiumInjectionTargets.POI_MANAGER_MIXIN, LithiumInjectionTargets.CLOSEST_BATCH,
            "(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;I"
                    + "Lnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;J)Ljava/util/Collection;")
            .read(LithiumPoiSearch.class.getClassLoader());
    private static final boolean RETRY_APPLIES = new LithiumOptionGate("[lithium-compat] gate ai_poi_tasks",
            LithiumInjectionTargets.TASKS_ACQUIRE_POI_MIXIN, LithiumInjectionTargets.RETRY_MARKER_LAMBDA,
            "(Lnet/minecraft/core/BlockPos;JLnet/minecraft/world/entity/ai/village/poi/PoiManager;"
                    + "Ljava/util/function/Predicate;Ljava/util/function/Predicate;J"
                    + "Lnet/minecraft/world/entity/ai/behavior/AcquirePoi$JitteredLinearRetry;)V")
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
