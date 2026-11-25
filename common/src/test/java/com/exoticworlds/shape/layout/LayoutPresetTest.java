package com.exoticworlds.shape.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.NetherScales;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.torus.GuaranteedLand;

import net.minecraft.core.Direction;

class LayoutPresetTest {
    private static final ShapeOptions PLAIN = DimensionLayout.DEFAULT_OPTIONS;

    private static final ShapeOptions LAND = PLAIN.with(GuaranteedLand.FEATURE.getOption(), true);

    @Test
    void aPresetOnATorusSetsItsSizeAndPutsTheNetherAndTheEndOnTheOverworld() {
        WorldLayout skewed = withOverworld(DimensionLayout.looped(ShapeFixtures.latticeTorus(48, 64, 4), LAND));

        WorldLayout large = LayoutPreset.apply(skewed, WorldLoopPresets.LARGE, 1.0);

        assertEquals(DimensionLayout.looped(ShapeFixtures.torus(256), LAND), large.overworld());
        assertEquals(ShapeKind.FROM_OVERWORLD, large.nether().kind());
        assertEquals(ShapeKind.FROM_OVERWORLD, large.end().kind());
        assertEquals(WorldLoopPresets.LARGE.getNetherScale(), large.netherScale());
    }

    @Test
    void aPresetOnACylinderKeepsItsLoopingAxis() {
        WorldLayout cylinder =
                withOverworld(DimensionLayout.looped(ShapeFixtures.cylinder(Direction.Axis.Z, 48), PLAIN));

        WorldLayout tiny = LayoutPreset.apply(cylinder, WorldLoopPresets.TINY, 1.0);

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.Z, WorldLoopPresets.TINY.getWidthChunks()),
                tiny.overworld().spans());
        assertTrue(LayoutPreset.isMatching(tiny, WorldLoopPresets.TINY));
    }

    @Test
    void aPresetOnANormalOverworldMakesItATorus() {
        WorldLayout medium = LayoutPreset.apply(WorldLayout.DEFAULT, WorldLoopPresets.MEDIUM, 1.0);

        assertEquals(ShapeFixtures.torus(WorldLoopPresets.MEDIUM.getWidthChunks()), medium.overworld().spans());
        assertFalse(LayoutPreset.isMatching(WorldLayout.DEFAULT, WorldLoopPresets.MEDIUM));
    }

    @Test
    void onlyThePresetJustAppliedMatches() {
        WorldLayout small = LayoutPreset.apply(WorldLayout.DEFAULT, WorldLoopPresets.SMALL, 1.0);

        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertEquals(preset == WorldLoopPresets.SMALL, LayoutPreset.isMatching(small, preset), preset.getId());
        }
    }

    @Test
    void aPresetStopsMatchingOnceTheSizeOrTheShiftChanges() {
        WorldLayout small = LayoutPreset.apply(WorldLayout.DEFAULT, WorldLoopPresets.SMALL, 1.0);
        int widthChunks = WorldLoopPresets.SMALL.getWidthChunks();

        assertFalse(LayoutPreset.isMatching(
                small.withOverworld(DimensionLayout.looped(ShapeFixtures.torus(widthChunks, widthChunks * 2), PLAIN)),
                WorldLoopPresets.SMALL));
        assertFalse(LayoutPreset.isMatching(
                small.withOverworld(
                        DimensionLayout.looped(ShapeFixtures.latticeTorus(widthChunks, widthChunks, 2), PLAIN)),
                WorldLoopPresets.SMALL));
    }

    private static WorldLayout withOverworld(DimensionLayout overworld) {
        return new WorldLayout(overworld, DimensionLayout.normal(PLAIN), DimensionLayout.normal(PLAIN), Map.of(),
                NetherScales.DEFAULT);
    }
}
