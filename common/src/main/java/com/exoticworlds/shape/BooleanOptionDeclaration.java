package com.exoticworlds.shape;

import java.util.List;

import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.option.OptionPart;
import com.exoticworlds.core.OptionKey;
import com.exoticworlds.core.OptionKeys;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record BooleanOptionDeclaration(OptionKey<Boolean> option, DimensionOption<Boolean> shown) {
    private static final boolean OFF = false;

    public static BooleanOptionDeclaration of(String key, int position, String captionKey, Identifier shape,
            ResourceKey<LevelStem> dimension) {
        OptionKey<Boolean> option = new OptionKey<>(key, position, Codec.BOOL, OFF);
        return new BooleanOptionDeclaration(option,
                DimensionOption.of(ShapeOptionsAdapter.shapeOption(option), captionKey, List.of(OptionPart.toggle()))
                        .forShapes(List.of(shape))
                        .forDimensions(List.of(dimension)));
    }

    public void register() {
        OptionKeys.register(option);
        shown.register();
    }
}
