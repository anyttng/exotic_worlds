package com.exoticworlds.core;

import java.util.List;
import java.util.Map;

import com.exoticworlds.api.v1.gen.GenerationHooks;
import com.exoticworlds.api.v2.gen.StructureStarts;

import net.minecraft.world.level.levelgen.RandomState;

public final class GenerationMoments {
    private static final StartupRegistry<String, RandomStateRun> RANDOM_STATE =
            new StartupRegistry<>("Generation hooks");

    private static final StartupRegistry<String, StructureStartsRun> STRUCTURE_STARTS =
            new StartupRegistry<>("Structure start hooks");

    @FunctionalInterface
    private interface RandomStateRun {
        void run(RandomState randomState, WorldFold fold, int seaLevel);
    }

    @FunctionalInterface
    private interface StructureStartsRun {
        List<StructureStarts.Added> starts(StructureStarts set, WorldFold fold);
    }

    public static void atRandomStateV1(String key, GenerationHooks.RandomStateHook hook) {
        RANDOM_STATE.register(key, (randomState, fold, seaLevel) ->
                hook.run(randomState, fold.shapeView(), fold.generationOptions(), seaLevel));
    }

    public static void atRandomState(String key, com.exoticworlds.api.v2.gen.GenerationHooks.RandomStateHook hook) {
        RANDOM_STATE.register(key, (randomState, fold, seaLevel) ->
                hook.run(randomState, fold.geometryView(), fold.generationOptions(), seaLevel));
    }

    public static void runAtRandomState(RandomState randomState, WorldFold fold, int seaLevel) {
        if (fold.cuboidAtlas() != null) {
            return;
        }

        RANDOM_STATE.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entry.getValue().run(randomState, fold, seaLevel));
    }

    public static void atStructureStartsV1(String key, GenerationHooks.StructureStartsHook hook) {
        STRUCTURE_STARTS.register(key, (set, fold) ->
                hook.starts(new StructureStartsV1View(set, fold.shapeView())).stream()
                        .map(added -> new StructureStarts.Added(added.structure(), added.chunk()))
                        .toList());
    }

    public static void atStructureStarts(String key,
            com.exoticworlds.api.v2.gen.GenerationHooks.StructureStartsHook hook) {
        STRUCTURE_STARTS.register(key, (set, fold) -> hook.starts(set));
    }

    public static boolean hasStructureStartHooks() {
        return !STRUCTURE_STARTS.entries().isEmpty();
    }

    public static List<StructureStarts.Added> runAtStructureStarts(StructureStarts set, WorldFold fold) {
        return STRUCTURE_STARTS.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().starts(set, fold).stream())
                .toList();
    }

    private GenerationMoments() {
    }
}
