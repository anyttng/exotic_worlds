package com.exoticworlds.shape.torus;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.shape.BooleanOptionDeclaration;

public final class GuaranteedNetherComplexes {
    public static final String KEY = "guaranteed_nether_complexes";

    private static final String LABEL_KEY = "gui.exotic_worlds.toroidal_settings.guaranteed_nether_complexes";

    private static final int POSITION = 2;

    private static final BooleanOptionDeclaration DECLARATION = BooleanOptionDeclaration.of(KEY, POSITION, LABEL_KEY);

    public static final WorldOption<Boolean> OPTION = DECLARATION.option();

    public static void register(boolean client) {
        DECLARATION.register(client);
    }

    private GuaranteedNetherComplexes() {
    }
}
