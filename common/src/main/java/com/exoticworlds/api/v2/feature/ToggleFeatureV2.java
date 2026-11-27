package com.exoticworlds.api.v2.feature;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;

/**
 * A feature the player switches on or off. It is off in every world until it is switched on, and its moments are called
 * only while it is on.
 */
public abstract class ToggleFeatureV2 extends OptionFeatureV2<Boolean> {
    private static final boolean OFF = false;

    /**
     * @param id the feature's id, also the key the switch is saved under
     * @param captionKey the translation key of the switch's caption
     */
    protected ToggleFeatureV2(Identifier id, String captionKey) {
        super(id, Codec.BOOL, OFF, OFF, captionKey, List.of(OptionPartV2.toggle()));
    }

    @Override
    public final boolean isOn(Boolean value) {
        return value;
    }
}
