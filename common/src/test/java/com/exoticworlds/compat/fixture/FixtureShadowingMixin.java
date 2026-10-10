package com.exoticworlds.compat.fixture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FixtureTarget.class)
public abstract class FixtureShadowingMixin {
    @Shadow
    public abstract int helper(int value);

    public void handler() {
        helper(1);
    }
}
