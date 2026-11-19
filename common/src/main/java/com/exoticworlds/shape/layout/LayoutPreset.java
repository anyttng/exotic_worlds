package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v2.shape.DimensionContext;
import com.exoticworlds.api.v2.shape.DimensionShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

public final class LayoutPreset {

    public static boolean offered(WorldLayout layout) {
        Identifier declared = layout.overworld().shape();
        return declared == null || presetShape(declared) != null;
    }

    public static WorldLayout apply(WorldLayout layout, WorldLoopPresets preset, double overworldScale) {
        DimensionLayout overworld = sized(layout.overworld(), preset, overworldScale);
        return overworld == null
                ? layout
                : new WorldLayout(overworld,
                        DimensionLayout.fromOverworld(layout.nether().options()),
                        DimensionLayout.fromOverworld(layout.end().options()),
                        layout.modDimensions(), preset.netherScale());
    }

    public static boolean matches(WorldLayout layout, WorldLoopPresets preset) {
        DimensionLayout overworld = layout.overworld();
        if (layout.nether().kind() != ShapeKind.FROM_OVERWORLD || layout.end().kind() != ShapeKind.FROM_OVERWORLD
                || layout.netherScale() != preset.netherScale()) {
            return false;
        }

        Identifier declared = overworld.shape();
        if (declared != null) {
            DimensionShape shape = presetShape(declared);
            return shape != null
                    && shape.withPreset(preset.chunkWidth(), overworld.options()).equals(overworld.options());
        }

        LoopSpans spans = overworld.spans();
        if (spans == null) {
            return false;
        }

        int chunkWidth = preset.chunkWidth();
        if (overworld.kind() == ShapeKind.CYLINDER) {
            return spans.chunkWidth(loopedAxis(spans)) == chunkWidth;
        }

        return spans.chunkWidth(Direction.Axis.X) == chunkWidth && spans.chunkWidth(Direction.Axis.Z) == chunkWidth
                && spans.skewChunks() == FlatShape.NO_SKEW;
    }

    private static @Nullable DimensionLayout sized(DimensionLayout overworld, WorldLoopPresets preset,
            double overworldScale) {
        Identifier declared = overworld.shape();
        if (declared != null) {
            DimensionShape shape = presetShape(declared);
            if (shape == null) {
                return null;
            }

            GenerationOptions options = shape.withPreset(preset.chunkWidth(), overworld.options());
            return DimensionLayout.declared(declared, DeclaredShapes.resolve(shape, new DimensionContext(
                    LevelStem.OVERWORLD, options, null, overworldScale, overworldScale)), options);
        }

        LoopSpans current = overworld.spans();
        return DimensionLayout.looped(current != null && overworld.kind() == ShapeKind.CYLINDER
                ? LoopSpans.ofWidth(loopedAxis(current), preset.chunkWidth())
                : LoopSpans.ofWidth(preset.chunkWidth()), overworld.options());
    }

    private static @Nullable DimensionShape presetShape(Identifier declared) {
        DimensionShape shape = DeclaredShapes.of(declared);
        return shape != null && shape.takesPresets() ? shape : null;
    }

    private static Direction.Axis loopedAxis(LoopSpans spans) {
        return spans.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private LayoutPreset() {
    }
}
