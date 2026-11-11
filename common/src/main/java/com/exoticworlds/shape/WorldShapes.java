package com.exoticworlds.shape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.shape.ShapeModule;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.StartupRegistry;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class WorldShapes {
    private static final String NORMAL_PATH = "normal";

    private static final ResourceLocation NORMAL_ID = ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, NORMAL_PATH);

    public static final WorldShape NORMAL = WorldShape.of(
            NORMAL_ID,
            WorldShape.label(NORMAL_ID),
            WorldShape.hint(NORMAL_ID),
            (registries, dimensions) -> dimensions);

    private static final StartupRegistry<ResourceLocation, WorldShape> SHAPES = new StartupRegistry<>("World shapes");

    private static final StartupRegistry<ResourceLocation, Predicate<SpawnPoint>> SPAWN_MOVES =
            new StartupRegistry<>("World shape spawn moves");

    private static WorldShape selected = NORMAL;

    @FunctionalInterface
    public interface SpawnMove<S> {
        @Nullable S withSpawn(S settings, SpawnPoint spawn);
    }

    public static void register(WorldShape shape) {
        SHAPES.register(shape.id(), shape);
    }

    public static <S> void takesSpawn(ShapeModule<S> module, SpawnMove<S> move) {
        SPAWN_MOVES.register(module.id(), spawn -> {
            S moved = move.withSpawn(module.settings(), spawn);
            if (moved == null) {
                return false;
            }

            module.settings(moved);
            return true;
        });
    }

    public static boolean moveSpawn(SpawnPoint spawn) {
        Predicate<SpawnPoint> move = SPAWN_MOVES.entries().get(selected.id());
        return move != null && move.test(spawn);
    }

    public static List<WorldShape> shapes() {
        List<WorldShape> own = new ArrayList<>();
        List<WorldShape> foreign = new ArrayList<>();
        for (WorldShape shape : SHAPES.entries().values()) {
            (ExoticWorlds.MODID.equals(shape.id().getNamespace()) ? own : foreign).add(shape);
        }
        foreign.sort(Comparator.comparing(shape -> shape.id().toString()));

        return Stream.of(Stream.of(NORMAL), own.stream(), foreign.stream())
                .flatMap(shapes -> shapes)
                .toList();
    }

    public static WorldShape selected() {
        return selected;
    }

    public static void select(WorldShape shape) {
        selected = shape;
    }

    public static void resetToDefault() {
        selected = NORMAL;
        shapes().forEach(shape -> shape.resetSettings().run());
    }

    public static void restoreFromExisting(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        for (WorldShape shape : shapes()) {
            if (shape.fromExisting() != null && shape.fromExisting().adopt(registries, dimensions)) {
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
