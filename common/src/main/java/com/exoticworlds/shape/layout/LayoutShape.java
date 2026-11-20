package com.exoticworlds.shape.layout;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.client.shape.layout.LayoutShapeSetup;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class LayoutShape {
    private static final String LAYOUT_PATH = "layout";

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, LAYOUT_PATH);

    private static WorldLayout chosen = WorldLayout.DEFAULT;

    public static WorldLayout layout() {
        return chosen;
    }

    public static void choose(WorldLayout layout) {
        chosen = layout;
    }

    public static void register(boolean client) {
        WorldShapes.register(WorldShape.of(ID, WorldShape.label(ID), WorldShape.hint(ID),
                (registries, dimensions) -> LayoutDimensions.apply(dimensions, chosen),
                () -> chosen = WorldLayout.DEFAULT,
                (registries, dimensions) -> adopt(dimensions)));
        WorldShapes.takesSpawn(ID, LayoutShape::moveSpawn);

        if (client) {
            LayoutShapeSetup.register();
        }
    }

    private static boolean adopt(WorldDimensions dimensions) {
        WorldLayout read = LayoutDimensions.read(dimensions);
        if (read == null) {
            return false;
        }

        chosen = read;
        return true;
    }

    private static boolean moveSpawn(SpawnPoint spawn) {
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
