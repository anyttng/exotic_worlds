package com.exoticworlds.compat.fixture.shared.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.compat.fixture.mod.FixtureModApi;
import com.exoticworlds.compat.fixture.mod.FixtureModLater;

@Mixin(FixtureModApi.class)
public abstract class FixtureSharedFirstMixin {
    public static Object later() {
        return new FixtureModLater();
    }
}
