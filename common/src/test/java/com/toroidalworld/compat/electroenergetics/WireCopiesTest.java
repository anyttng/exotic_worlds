package com.toroidalworld.compat.electroenergetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

class WireCopiesTest {
    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(64)));
    private static final double LAP = 1024.0;
    private static final Vec3 INLAND_1 = new Vec3(100.0, 70.0, 5.0);
    private static final Vec3 INLAND_2 = new Vec3(160.0, 70.0, 5.0);
    private static final Vec3 FAR_EAST_END = new Vec3(480.0, 70.0, 5.0);
    private static final Vec3 FAR_WEST_END = new Vec3(-500.0, 70.0, 5.0);
    private static final Vec3 WEST_END_BESIDE_EAST = new Vec3(-500.0 + LAP, 70.0, 5.0);
    private static final Vec3 EAST_END_BESIDE_WEST = new Vec3(480.0 - LAP, 70.0, 5.0);
    private static final float NEAR_FIRST_END = 0.25F;
    private static final BlockPos INLAND_HOLDER_1 = BlockPos.containing(INLAND_1);
    private static final BlockPos INLAND_HOLDER_2 = BlockPos.containing(INLAND_2);
    private static final BlockPos FAR_EAST_HOLDER = BlockPos.containing(FAR_EAST_END);
    private static final BlockPos FAR_WEST_HOLDER = BlockPos.containing(FAR_WEST_END);

    @Test
    void aLineInOnePieceHasOneCopy() {
        assertFalse(WireCopies.parted(TORUS, INLAND_HOLDER_1, INLAND_HOLDER_2));
        assertEquals(new WireCopies.BlockSpan(INLAND_HOLDER_1, INLAND_HOLDER_2),
                WireCopies.from(TORUS, INLAND_HOLDER_1, INLAND_HOLDER_2, WireCopies.FIRST_END));
        assertEquals(new WireCopies.BlockSpan(INLAND_HOLDER_1, INLAND_HOLDER_2),
                WireCopies.from(TORUS, INLAND_HOLDER_1, INLAND_HOLDER_2, WireCopies.SECOND_END));
    }

    @Test
    void eachHolderCarriesItsCopyAndSeatsTheOtherBesideIt() {
        assertTrue(WireCopies.parted(TORUS, FAR_EAST_HOLDER, FAR_WEST_HOLDER));
        assertEquals(new WireCopies.BlockSpan(FAR_EAST_HOLDER, BlockPos.containing(WEST_END_BESIDE_EAST)),
                WireCopies.from(TORUS, FAR_EAST_HOLDER, FAR_WEST_HOLDER, WireCopies.FIRST_END));
        assertEquals(new WireCopies.BlockSpan(BlockPos.containing(EAST_END_BESIDE_WEST), FAR_WEST_HOLDER),
                WireCopies.from(TORUS, FAR_EAST_HOLDER, FAR_WEST_HOLDER, WireCopies.SECOND_END));
    }

    @Test
    void aWireInOnePieceHasOneCopy() {
        assertFalse(WireCopies.parted(TORUS, INLAND_1, INLAND_2));
        assertEquals(new WireCopies.Span(INLAND_1, INLAND_2),
                WireCopies.from(TORUS, INLAND_1, INLAND_2, WireCopies.FIRST_END));
        assertEquals(new WireCopies.Span(INLAND_1, INLAND_2),
                WireCopies.from(TORUS, INLAND_1, INLAND_2, WireCopies.SECOND_END));
    }

    @Test
    void endsOnOppositeSidesOfTheViewPart() {
        assertTrue(WireCopies.parted(TORUS, FAR_EAST_END, FAR_WEST_END));
    }

    @Test
    void eachEndCarriesItsCopyAndSeatsTheOtherBesideIt() {
        assertEquals(new WireCopies.Span(FAR_EAST_END, WEST_END_BESIDE_EAST),
                WireCopies.from(TORUS, FAR_EAST_END, FAR_WEST_END, WireCopies.FIRST_END));
        assertEquals(new WireCopies.Span(EAST_END_BESIDE_WEST, FAR_WEST_END),
                WireCopies.from(TORUS, FAR_EAST_END, FAR_WEST_END, WireCopies.SECOND_END));
    }

    @Test
    void aPointIsReadOnTheCopyNearestTheViewer() {
        assertEquals(new WireCopies.Span(FAR_EAST_END, WEST_END_BESIDE_EAST),
                WireCopies.around(TORUS, FAR_EAST_END, FAR_WEST_END, NEAR_FIRST_END, new Vec3(470.0, 70.0, 5.0)));
        assertEquals(new WireCopies.Span(EAST_END_BESIDE_WEST, FAR_WEST_END),
                WireCopies.around(TORUS, FAR_EAST_END, FAR_WEST_END, NEAR_FIRST_END, new Vec3(-490.0, 70.0, 5.0)));
    }
}
