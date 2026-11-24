package com.exoticworlds.core.gen;

import java.util.List;
import java.util.Map;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.registry.StartupRegistry;

import net.minecraft.world.level.levelgen.RandomState;

public final class GenerationMoments {
    private static final StartupRegistry<String, RandomStateRun> RANDOM_STATE =
            new StartupRegistry<>("Generation hooks");

    private static final StartupRegistry<String, StructureStartsRun> STRUCTURE_STARTS =
            new StartupRegistry<>("Structure start hooks");

    @FunctionalInterface
    public interface RandomStateRun {
        void run(RandomState randomState, WorldFold fold, int seaLevel);
    }

    @FunctionalInterface
    public interface StructureStartsRun {
        List<AddedStart> starts(StructureStartSet set);
    }

    public static void atRandomState(String key, RandomStateRun run) {
        RANDOM_STATE.register(key, run);
    }

    public static void runAtRandomState(RandomState randomState, WorldFold fold, int seaLevel) {
        RANDOM_STATE.getEntries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entry.getValue().run(randomState, fold, seaLevel));
    }

    public static void atStructureStarts(String key, StructureStartsRun run) {
        STRUCTURE_STARTS.register(key, run);
    }

    public static boolean hasStructureStartHooks() {
        return !STRUCTURE_STARTS.getEntries().isEmpty();
    }

    public static List<AddedStart> runAtStructureStarts(StructureStartSet set) {
        return STRUCTURE_STARTS.getEntries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().starts(set).stream())
                .toList();
    }

    private GenerationMoments() {
    }
}
