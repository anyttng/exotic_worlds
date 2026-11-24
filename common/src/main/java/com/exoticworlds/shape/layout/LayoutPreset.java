package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.torus.Torus;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

public final class LayoutPreset {

    public static boolean isOffered(WorldLayout layout) {
        Identifier declared = layout.overworld().shape();
        return declared == null || getPresetShape(declared) != null;
    }

    public static WorldLayout apply(WorldLayout layout, WorldLoopPresets preset, double overworldScale) {
        DimensionLayout overworld = sized(layout.overworld(), preset, overworldScale);
        return overworld == null
                ? layout
                : new WorldLayout(overworld,
                        DimensionLayout.fromOverworld(layout.nether().options()),
                        DimensionLayout.fromOverworld(layout.end().options()),
                        layout.modDimensions(), preset.getNetherScale());
    }

    public static boolean isMatching(WorldLayout layout, WorldLoopPresets preset) {
        DimensionLayout overworld = layout.overworld();
        if (layout.nether().kind() != ShapeKind.FROM_OVERWORLD || layout.end().kind() != ShapeKind.FROM_OVERWORLD
                || layout.netherScale() != preset.getNetherScale()) {
            return false;
        }

        Identifier declared = overworld.shape();
        if (declared != null) {
            DimensionShape shape = getPresetShape(declared);
            ShapeOptions chosen = overworld.options();
            return shape != null && shape.withPreset(preset.getWidthChunks(), chosen).equals(chosen);
        }

        DimensionShape shape = overworld.kind().getBuiltIn();
        FlatShape spans = overworld.spans();
        if (shape == null || spans == null) {
            return false;
        }

        ShapeOptions size = shape.optionsFor(spans);
        return shape.withPreset(preset.getWidthChunks(), size).equals(size);
    }

    private static @Nullable DimensionLayout sized(DimensionLayout overworld, WorldLoopPresets preset,
            double overworldScale) {
        Identifier declared = overworld.shape();
        if (declared != null) {
            DimensionShape shape = getPresetShape(declared);
            if (shape == null) {
                return null;
            }

            ShapeOptions options = shape.withPreset(preset.getWidthChunks(), overworld.options());
            return DimensionLayout.declared(declared, DeclaredShapes.resolve(shape, new DimensionContext(
                    LevelStem.OVERWORLD, options, null, overworldScale, overworldScale)), options);
        }

        DimensionShape own = overworld.kind().getBuiltIn();
        DimensionShape shape = own != null ? own : Torus.SHAPE;
        FlatShape current = overworld.spans();
        ShapeOptions size = own != null && current != null ? shape.optionsFor(current) : ShapeOptions.DEFAULT;
        FlatShape spans = shape.spans(new DimensionContext(LevelStem.OVERWORLD,
                shape.withPreset(preset.getWidthChunks(), size), null, overworldScale, overworldScale));
        return DimensionLayout.looped(spans, overworld.options());
    }

    private static @Nullable DimensionShape getPresetShape(Identifier declared) {
        DimensionShape shape = DeclaredShapes.of(declared);
        return shape != null && shape.canTakePresets() ? shape : null;
    }

    private LayoutPreset() {
    }
}
