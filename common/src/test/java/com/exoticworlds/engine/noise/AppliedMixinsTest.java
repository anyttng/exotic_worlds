package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.ExoticWorlds;

class AppliedMixinsTest {
    @Test
    void candidatesLeaveOurOwnMixinsOutAndNameAnUnmappedMixinByItself() {
        List<AppliedMixins.Applied> applied = List.of(
                new AppliedMixins.Applied("com.exoticworlds.mixin.ExampleMixin", ExoticWorlds.MODID),
                new AppliedMixins.Applied("com.example.mixin.FirstMixin", "example_mod"),
                new AppliedMixins.Applied("com.example.mixin.SecondMixin", "example_mod"),
                new AppliedMixins.Applied("com.loose.mixin.LooseMixin", null));

        assertEquals(List.of("com.loose.mixin.LooseMixin", "example_mod"), AppliedMixins.foreign(applied));
    }
}
