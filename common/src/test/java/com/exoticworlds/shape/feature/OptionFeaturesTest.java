package com.exoticworlds.shape.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;

class OptionFeaturesTest {

    @BeforeAll
    static void registerFeatures() {
        StartupRegistrations.options();
    }

    @Test
    void aNewWorldTakesEachFeaturesOwnNewWorldValue() {
        assertEquals(ClimateScale.OFF, OptionFeatures.newWorldOptions().get(CompactBiomes.FEATURE.getOption()));
        assertFalse(OptionFeatures.newWorldOptions().get(GuaranteedLand.FEATURE.getOption()));
    }

    @Test
    void aWorldThatNeverStatedTheChoiceStillReadsTheOptionsOwnDefault() {
        assertEquals(ClimateScale.AUTO, CompactBiomes.FEATURE.getOption().defaultValue());
    }
}
