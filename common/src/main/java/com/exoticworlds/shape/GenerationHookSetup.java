package com.exoticworlds.shape;

import com.exoticworlds.shape.noise.FieldNoiseShaping;
import com.exoticworlds.shape.feature.guaranteedland.CoastFieldLift;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.NetherComplexStarts;

public final class GenerationHookSetup {

    public static void registerAll() {
        FieldNoiseShaping.register();
        CoastFieldLift.register();
        NetherComplexStarts.register();
    }

    private GenerationHookSetup() {
    }
}
