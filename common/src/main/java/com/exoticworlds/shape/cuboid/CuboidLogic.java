package com.exoticworlds.shape.cuboid;

import java.util.Optional;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CuboidShape;
import com.exoticworlds.core.world.ShapeModel;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.ShapeLogic;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class CuboidLogic implements ShapeLogic {

    CuboidLogic() {
    }

    @Override
    public CuboidShape spans(DimensionContext context) {
        return context.options().get(CuboidParameters.SIZE).toShape();
    }

    @Override
    public boolean isOfferedIn(ResourceKey<LevelStem> dimension) {
        return dimension == LevelStem.OVERWORLD;
    }

    @Override
    public Optional<ShapeOptions> optionsFor(ShapeModel spans) {
        return spans instanceof CuboidShape cuboid
                ? Optional.of(ShapeOptions.DEFAULT.with(CuboidParameters.SIZE, CuboidSize.of(cuboid)))
                : Optional.empty();
    }
}
