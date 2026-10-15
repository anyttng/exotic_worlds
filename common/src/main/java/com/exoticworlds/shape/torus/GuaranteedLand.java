package com.exoticworlds.shape.torus;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.shape.BooleanOptionDeclaration;

public final class GuaranteedLand {
    public static final String KEY = "guaranteed_land";

    private static final String LABEL_KEY = "gui.exotic_worlds.toroidal_settings.guaranteed_land";

    private static final int POSITION = 1;

    private static final BooleanOptionDeclaration DECLARATION = BooleanOptionDeclaration.of(KEY, POSITION, LABEL_KEY);

    public static final WorldOption<Boolean> OPTION = DECLARATION.option();

    public static void register(boolean client) {
        DECLARATION.register(client);
    }

    private GuaranteedLand() {
    }
}
