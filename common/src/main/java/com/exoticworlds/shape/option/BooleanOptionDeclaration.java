package com.exoticworlds.shape.option;

import java.util.List;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record BooleanOptionDeclaration(ShapeOption<Boolean> option, DimensionOption<Boolean> shown) {
    private static final boolean OFF = false;

    public static BooleanOptionDeclaration of(String key, int position, String captionKey, Identifier shape,
            ResourceKey<LevelStem> dimension) {
        ShapeOption<Boolean> option = new ShapeOption<>(key, position, Codec.BOOL, OFF);
        return new BooleanOptionDeclaration(option,
                DimensionOption.of(option, captionKey, List.of(OptionPart.toggle()))
                        .forShapes(List.of(shape))
                        .forDimensions(List.of(dimension)));
    }

    public void register() {
        ShapeOptionRegistry.register(option);
        DeclaredOptions.register(shown);
    }
}
