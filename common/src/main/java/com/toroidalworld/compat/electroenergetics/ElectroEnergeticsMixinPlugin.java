package com.toroidalworld.compat.electroenergetics;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModPresenceGatePlugin;
import com.toroidalworld.compat.ModSymbol;

public class ElectroEnergeticsMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String COLLECTOR_HANDLER = "(Ljava/util/function/UnaryOperator;Ljava/util/function/UnaryOperator;)V";

    private static final ModSymbol SECTION_INDEX = new ModSymbol(
            "com/george_vi/electroenergetics/simulation/infrastructure/WireSimulationState",
            "getConnectionsInSection", "(J)Ljava/util/Map;");

    private static final ModSymbol DETACHED_NODE_TICK = new ModSymbol(
            "com/george_vi/electroenergetics/simulation/infrastructure/detached_nodes/DetachedNodeEntity",
            "tick", "()V");

    private static final ModSymbol CHANGE_LENGTH_PACKET = new ModSymbol(
            "com/george_vi/electroenergetics/content/wire_spool/ChangeLengthWirePacket",
            "handle", "(Lnet/minecraft/server/level/ServerPlayer;)V");

    private static final ModSymbol LINEMANS_STICK = new ModSymbol(
            "com/george_vi/electroenergetics/content/linemans_stick/LinemansStickWireInteractionBehaviour",
            "interactWire", "(Lcom/george_vi/electroenergetics/foundation/nodes/NodeConnectionPoint;"
                    + "Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/item/ItemStack;)V");

    private static final ModSymbol PLAYER_BOX = new ModSymbol(
            "com/george_vi/electroenergetics/content/wire/WireSync$ChunkCuboidEntry", "includes", "(II)Z");

    private static final ModSymbol LEAVING_NODES = new ModSymbol(
            "com/george_vi/electroenergetics/content/wire/WireSync", "lambda$handlePlayerEnterNewSection$4",
            "(Lit/unimi/dsi/fastutil/longs/LongList;Lcom/george_vi/electroenergetics/foundation/nodes/InWorldNode;)Z");

    private static final ModSymbol TRAIN_COLLECTORS = new ModSymbol(
            "com/george_vi/electroenergetics/simulation/infrastructure/CatenaryModule", "buildCircuit",
            "(Lcom/george_vi/electroenergetics/simulation/CircuitBuilder;)V");

    private static final ModSymbol PANTOGRAPH_BLOCK = new ModSymbol(
            "com/george_vi/electroenergetics/content/railway_electrification/pantograph/PantographBlockEntity",
            "handleOnServer", COLLECTOR_HANDLER);

    private static final ModSymbol SHOE_BLOCK = new ModSymbol(
            "com/george_vi/electroenergetics/content/railway_electrification/third_rail/RailContactShoeBlockEntity",
            "handleOnServer", COLLECTOR_HANDLER);

    private static final ModPresence ELECTRO_ENERGETICS = ModPresence.of(LOGGER,
            "com/george_vi/electroenergetics/CreateElectroEnergetics.class",
            "[electroenergetics-compat] gate electroenergetics_present",
            SECTION_INDEX, DETACHED_NODE_TICK, CHANGE_LENGTH_PACKET, LINEMANS_STICK, PLAYER_BOX, LEAVING_NODES,
            TRAIN_COLLECTORS, PANTOGRAPH_BLOCK, SHOE_BLOCK);

    private static final String SPAWN_GUARD_MIXIN = "BulbSpawnGuardMixin";

    private static final ModPresence SPAWN_GUARD = ModPresence.of(LOGGER,
            "com/george_vi/electroenergetics/foundation/device/SpawnPreventingDevice.class",
            "[electroenergetics-compat] gate spawn_guard_present",
            new ModSymbol("com/george_vi/electroenergetics/events/GameEvents", "lambda$spawnMob$2",
                    "(Lnet/neoforged/neoforge/event/entity/living/MobSpawnEvent$SpawnPlacementCheck;"
                            + "Lcom/george_vi/electroenergetics/devices/device/SimulatedDevice;)Z"));

    public ElectroEnergeticsMixinPlugin() {
        super(ELECTRO_ENERGETICS);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return super.shouldApplyMixin(targetClassName, mixinClassName)
                && (!mixinClassName.endsWith(SPAWN_GUARD_MIXIN) || SPAWN_GUARD.present());
    }
}
