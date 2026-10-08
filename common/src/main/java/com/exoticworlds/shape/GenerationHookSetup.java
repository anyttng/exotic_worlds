package com.exoticworlds.shape;

import com.exoticworlds.shape.torus.CoastFieldLift;
import com.exoticworlds.shape.torus.NetherComplexStarts;

public final class GenerationHookSetup {

    public static void registerAll() {
        CoastFieldLift.register();
        NetherComplexStarts.register();
    }

    private GenerationHookSetup() {
    }
}
