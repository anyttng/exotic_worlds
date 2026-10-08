package com.exoticworlds.compat.wover;

public final class WoverInjectionTargets {
    public static final String NUMBER = "getNumber";

    public static final String NUMBER_DESCRIPTOR =
            "(Lorg/betterx/wover/surface/api/conditions/SurfaceRulesContext;)I";

    public static final String GET_NUMBER = NUMBER + NUMBER_DESCRIPTOR;

    public static final String NOISE_EVAL_3D = "Lorg/betterx/wover/math/api/noise/OpenSimplexNoise;eval(DDD)D";

    private WoverInjectionTargets() {
    }
}
