package com.exoticworlds.shape.torus;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.dimension.AbstractDimensionShape;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.LoopSizeLines;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Torus extends AbstractDimensionShape {
    private static final String NAME_KEY = LoopSizeLines.CAPTION_KEY + ".shape.torus";
    private static final String HINT_KEY = LoopSizeLines.CAPTION_KEY + ".shape.hint.torus";

    public static final Torus SHAPE = new Torus();

    private Torus() {
        super(DeclaredShapes.TORUS, new TorusLogic(), new TorusParameters());
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
        TorusSize size = options.get(TorusParameters.SIZE);
        MutableComponent summary = this.getName().copy().append(LoopSizeLines.SEPARATOR + size.xWidthChunks()
                + LoopSizeLines.TIMES + size.zWidthChunks());
        return size.shiftChunks() == 0
                ? summary
                : summary.append(LoopSizeLines.SEPARATOR).append(LoopSizeLines.shiftSummary(size.shiftChunks()));
    }
}
