package com.exoticworlds.api.v1.gen;

import java.util.List;

import com.exoticworlds.api.v1.Broken;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.option.GenerationOptions;

import net.minecraft.world.level.levelgen.RandomState;

/**
 * The two generation hooks of {@code api.v1}. A hook given here is accepted and never run.
 *
 * @deprecated generation no longer runs these hooks. Declare a feature through
 *     {@link com.exoticworlds.api.v2.feature.FeaturesV2} that implements
 *     {@link com.exoticworlds.api.v2.feature.RandomStateMomentV2} or
 *     {@link com.exoticworlds.api.v2.feature.StructureStartsMomentV2}.
 */
@Deprecated
@Broken(GenerationHooks.NO_HOOKS)
public final class GenerationHooks {
    static final String NO_HOOKS = "Generation no longer runs these hooks, so a hook given here is never called."
            + " Declare an api.v2.feature.FeatureV2 that implements RandomStateMomentV2 or StructureStartsMomentV2.";

    /** The hook {@link #atRandomState} takes. It is never run. */
    @FunctionalInterface
    public interface RandomStateHook {
        void run(RandomState randomState, ToroidalShape shape, GenerationOptions options, int seaLevel);
    }

    /** Does nothing. */
    public static void atRandomState(String key, RandomStateHook hook) {
    }

    /** The hook {@link #atStructureStarts} takes. It is never asked. */
    @FunctionalInterface
    public interface StructureStartsHook {
        List<StructureStarts.Added> starts(StructureStarts set);
    }

    /** Does nothing. */
    public static void atStructureStarts(String key, StructureStartsHook hook) {
    }

    private GenerationHooks() {
    }
}
