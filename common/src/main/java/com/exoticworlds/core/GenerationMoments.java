package com.exoticworlds.core;

import java.util.List;
import java.util.Map;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.gen.GenerationHooks;
import com.exoticworlds.api.v1.gen.StructureStarts;
import com.exoticworlds.api.v1.option.GenerationOptions;

import net.minecraft.world.level.levelgen.RandomState;

public final class GenerationMoments {
    private static final StartupRegistry<String, GenerationHooks.RandomStateHook> RANDOM_STATE =
            new StartupRegistry<>("Generation hooks");

    private static final StartupRegistry<String, GenerationHooks.StructureStartsHook> STRUCTURE_STARTS =
            new StartupRegistry<>("Structure start hooks");

    public static void atRandomState(String key, GenerationHooks.RandomStateHook hook) {
        RANDOM_STATE.register(key, hook);
    }

    public static void runAtRandomState(RandomState randomState, ToroidalShape shape, GenerationOptions options,
            int seaLevel) {
        RANDOM_STATE.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entry.getValue().run(randomState, shape, options, seaLevel));
    }

    public static void atStructureStarts(String key, GenerationHooks.StructureStartsHook hook) {
        STRUCTURE_STARTS.register(key, hook);
    }

    public static boolean hasStructureStartHooks() {
        return !STRUCTURE_STARTS.entries().isEmpty();
    }

    public static List<StructureStarts.Added> runAtStructureStarts(StructureStarts set) {
        return STRUCTURE_STARTS.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().starts(set).stream())
                .toList();
    }

    private GenerationMoments() {
    }
}
