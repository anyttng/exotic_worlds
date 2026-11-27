package com.exoticworlds.shape.cuboid;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.dimension.AbstractDimensionShape;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.LoopSizeLines;

import net.minecraft.network.chat.Component;

public final class Cuboid extends AbstractDimensionShape {
    private static final String NAME_KEY = LoopSizeLines.CAPTION_KEY + ".shape.cuboid";
    private static final String HINT_KEY = LoopSizeLines.CAPTION_KEY + ".shape.hint.cuboid";

    public static final Cuboid SHAPE = new Cuboid();

    private Cuboid() {
        super(DeclaredShapes.CUBOID, new CuboidLogic(), new CuboidParameters());
    }

    @Override
    public Component getName() {
        return Component.translatable(NAME_KEY);
    }

    @Override
    public Component getHint() {
        return Component.translatable(HINT_KEY);
    }

    @Override
    public Component describe(ShapeOptions options) {
        CuboidSize size = options.get(CuboidParameters.SIZE);
        return this.getName().copy().append(LoopSizeLines.SEPARATOR + size.aChunks() + LoopSizeLines.TIMES
                + size.bChunks() + LoopSizeLines.TIMES + size.cChunks());
    }
}
