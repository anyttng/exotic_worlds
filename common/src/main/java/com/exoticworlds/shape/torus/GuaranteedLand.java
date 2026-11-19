package com.exoticworlds.shape.torus;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.shape.BooleanOptionDeclaration;

public final class GuaranteedLand {
    public static final String KEY = "guaranteed_land";

    private static final int POSITION = 1;

    private static final BooleanOptionDeclaration DECLARATION = BooleanOptionDeclaration.of(KEY, POSITION);

    public static final WorldOption<Boolean> OPTION = DECLARATION.option();

    public static void register() {
        DECLARATION.register();
    }

    private GuaranteedLand() {
    }
}
