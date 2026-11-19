package com.exoticworlds.shape;

import java.util.List;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.option.OptionPart;

import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record BooleanOptionDeclaration(WorldOption<Boolean> option, DimensionOption<Boolean> shown) {
    private static final boolean OFF = false;

    public static BooleanOptionDeclaration of(String key, int position, String captionKey, ResourceLocation shape,
            ResourceKey<LevelStem> dimension) {
        WorldOption<Boolean> option = new WorldOption<>(key, position, Codec.BOOL, OFF);
        return new BooleanOptionDeclaration(option,
                DimensionOption.of(option, captionKey, List.of(OptionPart.toggle()))
                        .forShapes(List.of(shape))
                        .forDimensions(List.of(dimension)));
    }

    public void register() {
        WorldOptions.register(option);
        shown.register();
    }
}
