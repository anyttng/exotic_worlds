package com.exoticworlds.shape.torus;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.option.BooleanOptionDeclaration;
import com.exoticworlds.shape.option.DimensionOption;

import net.minecraft.world.level.dimension.LevelStem;

public final class GuaranteedLand {
    public static final String KEY = "guaranteed_land";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.guaranteed_land";

    private static final int POSITION = 1;

    private static final BooleanOptionDeclaration DECLARATION =
            BooleanOptionDeclaration.of(KEY, POSITION, CAPTION_KEY, DeclaredShapes.TORUS, LevelStem.OVERWORLD);

    public static final ShapeOption<Boolean> OPTION = DECLARATION.option();

    public static final DimensionOption<Boolean> SHOWN = DECLARATION.shown();

    public static void register() {
        DECLARATION.register();
    }

    private GuaranteedLand() {
    }
}
