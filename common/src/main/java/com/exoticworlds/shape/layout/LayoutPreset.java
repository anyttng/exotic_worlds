package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeOptions;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;

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
            ShapeOptions chosen = overworld.options();
            return shape != null && shape.withPreset(preset.chunkWidth(), chosen).equals(chosen);
        }

        FlatShape spans = overworld.spans();
        if (spans == null) {
            return false;
        }

        int chunkWidth = preset.chunkWidth();
        if (overworld.kind() == ShapeKind.CYLINDER) {
            return spans.bounds().chunkWidth(loopedAxis(spans)) == chunkWidth;
        }

        return spans.bounds().chunkWidth(Direction.Axis.X) == chunkWidth
                && spans.bounds().chunkWidth(Direction.Axis.Z) == chunkWidth
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

            ShapeOptions options = shape.withPreset(preset.chunkWidth(), overworld.options());
            return DimensionLayout.declared(declared, DeclaredShapes.resolve(shape, new DimensionContext(
                    LevelStem.OVERWORLD, options, null, overworldScale, overworldScale)), options);
        }

        FlatShape current = overworld.spans();
        return DimensionLayout.looped(current != null && overworld.kind() == ShapeKind.CYLINDER
                ? FlatShape.unskewed(WorldLoopBounds.ofWidth(loopedAxis(current), preset.chunkWidth()))
                : FlatShape.unskewed(WorldLoopBounds.ofWidth(preset.chunkWidth())), overworld.options());
    }

    private static @Nullable DimensionShape presetShape(Identifier declared) {
        DimensionShape shape = DeclaredShapes.of(declared);
        return shape != null && shape.takesPresets() ? shape : null;
    }

    private static Direction.Axis loopedAxis(FlatShape spans) {
        return spans.bounds().loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private LayoutPreset() {
    }
}
