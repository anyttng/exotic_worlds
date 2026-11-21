package com.exoticworlds.shape.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.CuboidShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.torus.GuaranteedLand;

import net.minecraft.core.Direction;

class LayoutPresetTest {
    private static final GenerationOptions PLAIN = DimensionLayout.DEFAULT_OPTIONS;

    private static final GenerationOptions LAND = PLAIN.with(GuaranteedLand.OPTION, true);

    @Test
    void aPresetOnATorusSetsItsSizeAndPutsTheNetherAndTheEndOnTheOverworld() {
        WorldLayout skewed = withOverworld(DimensionLayout.looped(LoopSpans.ofWidths(48, 64).withSkew(4), LAND));

        WorldLayout large = LayoutPreset.apply(skewed, WorldLoopPresets.LARGE, 1.0);

        assertEquals(DimensionLayout.looped(LoopSpans.ofWidth(256), LAND), large.overworld());
        assertEquals(ShapeKind.FROM_OVERWORLD, large.nether().kind());
        assertEquals(ShapeKind.FROM_OVERWORLD, large.end().kind());
        assertEquals(WorldLoopPresets.LARGE.netherScale(), large.netherScale());
    }

    @Test
    void aPresetOnACylinderKeepsItsLoopingAxis() {
        WorldLayout cylinder = withOverworld(DimensionLayout.looped(LoopSpans.ofWidth(Direction.Axis.Z, 48), PLAIN));

        WorldLayout tiny = LayoutPreset.apply(cylinder, WorldLoopPresets.TINY, 1.0);

        assertEquals(LoopSpans.ofWidth(Direction.Axis.Z, WorldLoopPresets.TINY.chunkWidth()), tiny.overworld().spans());
        assertTrue(LayoutPreset.matches(tiny, WorldLoopPresets.TINY));
    }

    @Test
    void aPresetOnANormalOverworldMakesItATorus() {
        WorldLayout medium = LayoutPreset.apply(WorldLayout.DEFAULT, WorldLoopPresets.MEDIUM, 1.0);

        assertEquals(LoopSpans.ofWidth(WorldLoopPresets.MEDIUM.chunkWidth()), medium.overworld().spans());
        assertFalse(LayoutPreset.matches(WorldLayout.DEFAULT, WorldLoopPresets.MEDIUM));
    }

    @Test
    void onlyThePresetJustAppliedMatches() {
        WorldLayout small = LayoutPreset.apply(WorldLayout.DEFAULT, WorldLoopPresets.SMALL, 1.0);

        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertEquals(preset == WorldLoopPresets.SMALL, LayoutPreset.matches(small, preset), preset.id());
        }
    }

    @Test
    void aPresetStopsMatchingOnceTheSizeOrTheShiftChanges() {
        WorldLayout small = LayoutPreset.apply(WorldLayout.DEFAULT, WorldLoopPresets.SMALL, 1.0);
        int chunkWidth = WorldLoopPresets.SMALL.chunkWidth();

        assertFalse(LayoutPreset.matches(
                small.withOverworld(DimensionLayout.looped(LoopSpans.ofWidths(chunkWidth, chunkWidth * 2), PLAIN)),
                WorldLoopPresets.SMALL));
        assertFalse(LayoutPreset.matches(
                small.withOverworld(DimensionLayout.looped(LoopSpans.ofWidth(chunkWidth).withSkew(2), PLAIN)),
                WorldLoopPresets.SMALL));
    }

    @Test
    void thePresetRowIsNotOfferedUnderACuboidOverworld() {
        assertTrue(LayoutPreset.offered(WorldLayout.DEFAULT));
        assertFalse(LayoutPreset.offered(withOverworld(DimensionLayout.cuboid(new CuboidShape(16, 24, 40)))));
    }

    private static WorldLayout withOverworld(DimensionLayout overworld) {
        return new WorldLayout(overworld, DimensionLayout.normal(PLAIN), DimensionLayout.normal(PLAIN), Map.of(),
                NetherScales.DEFAULT);
    }
}
