package com.exoticworlds.shape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.core.registry.StartupRegistry;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class WorldShapes {
    private static final String NORMAL_PATH = "normal";

    private static final Identifier NORMAL_ID = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, NORMAL_PATH);

    public static final WorldShape NORMAL = WorldShape.of(NORMAL_ID, (registries, dimensions) -> dimensions);

    private static final StartupRegistry<Identifier, WorldShape> SHAPES = new StartupRegistry<>("World shapes");

    private static final StartupRegistry<Identifier, Predicate<SpawnPoint>> SPAWN_MOVES =
            new StartupRegistry<>("World shape spawn moves");

    private static WorldShape selected = NORMAL;

    public static void register(WorldShape shape) {
        SHAPES.register(shape.id(), shape);
    }

    public static void registerSpawnMove(Identifier shapeId, Predicate<SpawnPoint> move) {
        SPAWN_MOVES.register(shapeId, move);
    }

    public static boolean tryMoveSpawn(SpawnPoint spawn) {
        Predicate<SpawnPoint> move = SPAWN_MOVES.getEntries().get(selected.id());
        return move != null && move.test(spawn);
    }

    public static List<WorldShape> getShapes() {
        List<WorldShape> own = new ArrayList<>();
        List<WorldShape> foreign = new ArrayList<>();
        for (WorldShape shape : SHAPES.getEntries().values()) {
            (ExoticWorlds.MODID.equals(shape.id().getNamespace()) ? own : foreign).add(shape);
        }
        foreign.sort(Comparator.comparing(shape -> shape.id().toString()));

        return Stream.of(Stream.of(NORMAL), own.stream(), foreign.stream())
                .flatMap(shapes -> shapes)
                .toList();
    }

    public static void select(WorldShape shape) {
        selected = shape;
    }

    public static void select(Identifier id) {
        WorldShape shape = SHAPES.getEntries().get(id);
        if (shape == null) {
            throw new IllegalArgumentException("No world shape is registered under " + id);
        }

        selected = shape;
    }

    public static void resetToDefault() {
        selected = NORMAL;
        getShapes().forEach(shape -> shape.resetSettings().run());
    }

    public static void restoreFromExisting(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        for (WorldShape shape : getShapes()) {
            if (shape.fromExisting() != null && shape.fromExisting().tryAdopt(registries, dimensions)) {
                selected = shape;
                return;
            }
        }
    }

    public static WorldDimensions applyAtCreation(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        return selected.atCreation().apply(registries, ShapedDimensions.stripShapes(dimensions));
    }

    private WorldShapes() {
    }
}
