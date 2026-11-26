package com.exoticworlds.shape.layout;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.WorldShapes;
import com.exoticworlds.shape.feature.OptionFeature;

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

    public static void register() {
        chosen = WorldLayout.newWorld();
        WorldShapes.register(WorldShape.of(ID,
                (registries, dimensions) -> LayoutDimensions.apply(dimensions, chosen),
                () -> chosen = WorldLayout.newWorld(),
                (registries, dimensions) -> tryAdopt(dimensions)));
    }

    private static boolean tryAdopt(WorldDimensions dimensions) {
        WorldLayout read = LayoutDimensions.read(dimensions);
        if (read == null) {
            return false;
        }

        chosen = read;
        return true;
    }

    public static <T> boolean trySetOverworldValue(OptionFeature<T> feature, T value) {
        WorldLayout changed = chosen.withOverworldValue(feature, value);
        if (changed == null) {
            return false;
        }

        chosen = changed;
        return true;
    }

    private LayoutShape() {
    }
}
