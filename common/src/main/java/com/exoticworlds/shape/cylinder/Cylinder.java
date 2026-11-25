package com.exoticworlds.shape.cylinder;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.dimension.AbstractDimensionShape;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.LoopSizeLines;

import net.minecraft.network.chat.Component;

public final class Cylinder extends AbstractDimensionShape {
    private static final String NAME_KEY = LoopSizeLines.CAPTION_KEY + ".shape.cylinder";
    private static final String HINT_KEY = LoopSizeLines.CAPTION_KEY + ".shape.hint.cylinder";
    private static final String AXIS_SEPARATOR = " ";

    public static final Cylinder SHAPE = new Cylinder();

    private Cylinder() {
        super(DeclaredShapes.CYLINDER, new CylinderLogic(), new CylinderParameters());
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
        CylinderSize size = options.get(CylinderParameters.SIZE);
        return this.getName().copy().append(AXIS_SEPARATOR).append(LoopSizeLines.axisName(size.axis()))
                .append(LoopSizeLines.SEPARATOR + size.widthChunks());
    }
}
