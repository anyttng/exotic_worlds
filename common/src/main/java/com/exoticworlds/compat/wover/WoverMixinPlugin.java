package com.exoticworlds.compat.wover;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class WoverMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String INIT_MAP_NAME = "onInitMap";

    private static final String INIT_MAP_DESCRIPTOR = "(J)V";

    private static final String RAW_BIOME_NAME = "getRawBiome";

    private static final String RAW_BIOME_DESCRIPTOR =
            "(DD)Lorg/betterx/wover/generator/api/biomesource/WoverBiomePicker$PickableBiome;";

    private static final String MAP_STACK = "org/betterx/wover/generator/impl/map/MapStack";

    private static final String SQUARE_MAP = "org/betterx/wover/generator/impl/map/square/SquareBiomeMap";

    private static final String CONSTRUCTOR = "<init>";

    private static final String THRESHOLD_CONDITION =
            "org/betterx/wover/surface/impl/conditions/ThresholdConditionImpl";

    private static final String VOLUME_CONDITION =
            "org/betterx/wover/surface/impl/conditions/VolumeThresholdConditionImpl";

    static final ModSymbol RAW_BIOME = new ModSymbol(
            "org/betterx/wover/generator/impl/map/hex/HexBiomeMap", RAW_BIOME_NAME, RAW_BIOME_DESCRIPTOR);

    static final ModSymbol SQUARE_RAW_BIOME = new ModSymbol(SQUARE_MAP, RAW_BIOME_NAME, RAW_BIOME_DESCRIPTOR);

    static final ModSymbol SQUARE_CONSTRUCTOR = new ModSymbol(SQUARE_MAP, CONSTRUCTOR,
            "(JILorg/betterx/wover/generator/api/biomesource/WoverBiomePicker;)V");

    static final ModSymbol STACK_BIOME = new ModSymbol(MAP_STACK, "getBiome",
            "(DDD)Lorg/betterx/wover/generator/api/biomesource/WoverBiomePicker$PickableBiome;");

    static final ModSymbol STACK_CONSTRUCTOR = new ModSymbol(MAP_STACK, CONSTRUCTOR,
            "(JILorg/betterx/wover/generator/api/biomesource/WoverBiomePicker;II"
                    + "Lorg/betterx/wover/generator/api/map/MapBuilderFunction;)V");

    // A game type is spelled per loader on this line, so a target class is gated by a member naming none.
    static final ModSymbol NETHER_INIT_MAP = new ModSymbol(
            "org/betterx/wover/generator/impl/biomesource/nether/WoverNetherBiomeSource", INIT_MAP_NAME,
            INIT_MAP_DESCRIPTOR);

    static final ModSymbol END_INIT_MAP = new ModSymbol(
            "org/betterx/wover/generator/impl/biomesource/end/WoverEndBiomeSource", INIT_MAP_NAME,
            INIT_MAP_DESCRIPTOR);

    static final ModSymbol GENERATOR_INITIALIZE = new ModSymbol(
            "org/betterx/wover/generator/impl/chunkgenerator/WoverChunkGeneratorImpl", "initialize", "()V");

    static final ModSymbol THRESHOLD_TEST = new ModSymbol(THRESHOLD_CONDITION, "test",
            "(Lorg/betterx/wover/surface/api/conditions/SurfaceRulesContext;)Z");

    static final ModSymbol VOLUME_VALUE = new ModSymbol(VOLUME_CONDITION, "getValue", "(III)D");

    static final ModSymbol NETHER_NOISE_NUMBER = new ModSymbol(
            "org/betterx/wover/surface/impl/numeric/NetherNoiseCondition", WoverInjectionTargets.NUMBER,
            WoverInjectionTargets.NUMBER_DESCRIPTOR);

    static final ModSymbol NOISE_EVAL_3D = new ModSymbol("org/betterx/wover/math/api/noise/OpenSimplexNoise", "eval",
            "(DDD)D");

    static final ModSymbol[] SYMBOLS = {RAW_BIOME, SQUARE_RAW_BIOME, SQUARE_CONSTRUCTOR, STACK_BIOME,
            STACK_CONSTRUCTOR, NETHER_INIT_MAP, END_INIT_MAP, GENERATOR_INITIALIZE, THRESHOLD_TEST, VOLUME_VALUE,
            NETHER_NOISE_NUMBER, NOISE_EVAL_3D};

    private static final ModPresence WOVER = ModPresence.of(LOGGER,
            "org/betterx/wover/generator/impl/map/hex/HexBiomeMap.class",
            "[wover-compat] gate wover_present", SYMBOLS);

    public WoverMixinPlugin() {
        super(WOVER);
    }
}
