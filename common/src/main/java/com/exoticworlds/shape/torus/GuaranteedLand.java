package com.exoticworlds.shape.torus;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.shape.BuiltInShapes;
import com.exoticworlds.shape.BooleanOptionDeclaration;

import net.minecraft.world.level.dimension.LevelStem;

public final class GuaranteedLand {
    public static final String KEY = "guaranteed_land";

    private static final String CAPTION_KEY = "gui.exotic_worlds.world_shape_settings.guaranteed_land";

    private static final int POSITION = 1;

    private static final BooleanOptionDeclaration DECLARATION =
            BooleanOptionDeclaration.of(KEY, POSITION, CAPTION_KEY, BuiltInShapes.TORUS, LevelStem.OVERWORLD);

    public static final WorldOption<Boolean> OPTION = DECLARATION.option();

    public static final DimensionOption<Boolean> SHOWN = DECLARATION.shown();

    public static void register() {
        DECLARATION.register();
    }

    private GuaranteedLand() {
    }
}
