package com.exoticworlds.shape.layout;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.core.Direction;

public final class LayoutPreset {

    public static WorldLayout apply(WorldLayout layout, WorldLoopPresets preset) {
        DimensionLayout overworld = layout.overworld();
        LoopSpans current = overworld.spans();
        LoopSpans spans = current != null && overworld.kind() == ShapeKind.CYLINDER
                ? LoopSpans.ofWidth(loopedAxis(current), preset.chunkWidth())
                : LoopSpans.ofWidth(preset.chunkWidth());
        return new WorldLayout(DimensionLayout.looped(spans, overworld.options()),
                DimensionLayout.fromOverworld(layout.nether().options()),
                DimensionLayout.fromOverworld(layout.end().options()),
                layout.modDimensions(), preset.netherScale());
    }

    public static boolean matches(WorldLayout layout, WorldLoopPresets preset) {
        LoopSpans spans = layout.overworld().spans();
        if (spans == null || layout.nether().kind() != ShapeKind.FROM_OVERWORLD
                || layout.end().kind() != ShapeKind.FROM_OVERWORLD || layout.netherScale() != preset.netherScale()) {
            return false;
        }

        int chunkWidth = preset.chunkWidth();
        if (layout.overworld().kind() == ShapeKind.CYLINDER) {
            return spans.chunkWidth(loopedAxis(spans)) == chunkWidth;
        }

        return spans.chunkWidth(Direction.Axis.X) == chunkWidth && spans.chunkWidth(Direction.Axis.Z) == chunkWidth
                && spans.skewChunks() == FlatShape.NO_SKEW;
    }

    private static Direction.Axis loopedAxis(LoopSpans spans) {
        return spans.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private LayoutPreset() {
    }
}
