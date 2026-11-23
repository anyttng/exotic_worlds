package com.exoticworlds.bridge.v1.shape;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import com.exoticworlds.shape.WorldShape;
import com.exoticworlds.shape.WorldShapes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class ModuleBridge {

    public static void register(Identifier id,
            BiFunction<RegistryAccess.Frozen, WorldDimensions, WorldDimensions> atCreation, Runnable resetSettings,
            BiPredicate<RegistryAccess.Frozen, WorldDimensions> fromExisting) {
        WorldShapes.register(WorldShape.of(id, WorldShape.label(id), WorldShape.hint(id), atCreation::apply,
                resetSettings, fromExisting::test));
    }

    private ModuleBridge() {
    }
}
