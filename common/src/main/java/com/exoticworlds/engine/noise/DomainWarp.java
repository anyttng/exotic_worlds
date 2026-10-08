package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

public final class DomainWarp {
    public record Divisor(WorldFold fold, double value) {
    }

    public static double apply(WrapDomain domain, int block, double shift, double divisor) {
        return domain.wrap(block) + shift / divisor;
    }

    private DomainWarp() {
    }
}
