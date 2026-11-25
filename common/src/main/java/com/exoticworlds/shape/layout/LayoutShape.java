package com.exoticworlds.shape.layout;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.client.shape.layout.LayoutShapeSetup;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class LayoutShape {
    private static final String LAYOUT_PATH = "layout";

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, LAYOUT_PATH);

    private static WorldLayout chosen = WorldLayout.newWorld();

    public static WorldLayout getLayout() {
        return chosen;
    }

    public static void choose(WorldLayout layout) {
        chosen = layout;
    }

    public static void register(boolean isClient) {
        chosen = WorldLayout.newWorld();
        WorldShapes.register(WorldShape.of(ID, WorldShape.label(ID), WorldShape.hint(ID),
                (registries, dimensions) -> LayoutDimensions.apply(dimensions, chosen),
                () -> chosen = WorldLayout.newWorld(),
                (registries, dimensions) -> tryAdopt(dimensions)));
        WorldShapes.takesSpawn(ID, LayoutShape::tryMoveSpawn);

        if (isClient) {
            LayoutShapeSetup.register();
        }
    }

    private static boolean tryAdopt(WorldDimensions dimensions) {
        WorldLayout read = LayoutDimensions.read(dimensions);
        if (read == null) {
            return false;
        }

        chosen = read;
        return true;
    }

    private static boolean tryMoveSpawn(SpawnPoint spawn) {
        WorldLayout moved = chosen.withSpawn(spawn);
        if (moved == null) {
            return false;
        }

        chosen = moved;
        return true;
    }

    private LayoutShape() {
    }
}
