package com.exoticworlds.compat.cobblemon;

import java.util.Set;

import org.slf4j.Logger;

import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.mojang.logging.LogUtils;

public class CobblemonMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG = "exotic_worlds.compat.cobblemon.mixins.json";
    private static final String CAN_SEE_MIXIN = "CanSeeAimMixin";
    private static final String POLLINATE_MIXIN = "PollinateHoverMixin";
    private static final String HONEY_MIXIN = "HoneyHoverMixin";
    private static final String HIVE_SIDE_MIXIN = "HiveSideOrderMixin";
    private static final Set<String> OWN_GATES = Set.of(CAN_SEE_MIXIN, POLLINATE_MIXIN, HONEY_MIXIN, HIVE_SIDE_MIXIN);

    private static final ModPresence COBBLEMON = ModPresence.gate(LOGGER, "[ew/cobblemon-compat] gate cobblemon_present")
            .probing("com/cobblemon/mod/common/util/PlayerExtensionsKt.class")
            .checking(CONFIG, mixin -> !OWN_GATES.contains(mixin))
            .bodyFrom("EnderPearlLookMixin", "com/cobblemon/mod/common/mixin/EnderPearlItemMixin")
            .build();

    private static final ModPresence CAN_SEE = ModPresence.gate(LOGGER, "[ew/cobblemon-compat] gate can_see_present")
            .probing("com/cobblemon/mod/common/api/molang/function/LivingEntityMoLangFunctions.class")
            .checking(CONFIG, CAN_SEE_MIXIN::equals)
            .build();

    private static final ModPresence POLLINATE = ModPresence.gate(LOGGER, "[ew/cobblemon-compat] gate pollinate_present")
            .probing("com/cobblemon/mod/common/api/ai/config/task/PollinateFlowerTaskConfig$createTask$1.class")
            .checking(CONFIG, POLLINATE_MIXIN::equals)
            .build();

    private static final ModPresence HONEY = ModPresence.gate(LOGGER, "[ew/cobblemon-compat] gate honey_present")
            .probing("com/cobblemon/mod/common/api/ai/config/task/PlaceHoneyInSaccLeavesTaskConfig$createTask$1.class")
            .checking(CONFIG, HONEY_MIXIN::equals)
            .build();

    private static final ModPresence HIVE_SIDE = ModPresence.gate(LOGGER, "[ew/cobblemon-compat] gate hive_side_present")
            .probing("com/cobblemon/mod/common/api/ai/config/task/"
                    + "PathToBeeHiveTaskConfig$createTask$1$start$$inlined$sortedBy$1.class")
            .checking(CONFIG, HIVE_SIDE_MIXIN::equals)
            .build();

    public CobblemonMixinPlugin() {
        super(COBBLEMON, CAN_SEE, POLLINATE, HONEY, HIVE_SIDE);
    }
}
