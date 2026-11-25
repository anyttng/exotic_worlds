package com.exoticworlds.api.v2.feature;

import net.minecraft.resources.Identifier;

/**
 * One thing a world shape can do beyond its geometry: a rule of generation, with or without an option the player
 * sets. A feature is one object. It says what it does by implementing the moment interfaces of this package
 * ({@link RandomStateMomentV2}, {@link StructureStartsMomentV2}, {@link NoiseMomentV2}, {@link SpawnMomentV2}), as
 * many as it needs, and the mod calls each one only in a world whose shape carries the feature, in a dimension the
 * feature was added for, and while the feature is on.
 *
 * <p>A feature with nothing for the player to set extends this class and is always on. One with an option extends
 * {@link OptionFeatureV2}. Register it once with {@link FeaturesV2#register}.
 */
public abstract class FeatureV2 {
    private final Identifier id;

    /**
     * @param id the feature's own id under the mod's namespace, {@code example_mod:example_feature}; it is also the
     *        key the feature's option is saved under
     */
    protected FeatureV2(Identifier id) {
        this.id = id;
    }

    /** The id this feature was created with. */
    public final Identifier getId() {
        return this.id;
    }
}
