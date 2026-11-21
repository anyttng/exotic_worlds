package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FoldOrientation;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;

class EventDirectionsTest {
    private static final FoldOrientation CLOCKWISE = FoldOrientation.QUARTER_TURN_CLOCKWISE;

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int NORTH = 2;
    private static final int SOUTH = 3;
    private static final int WEST = 4;
    private static final int EAST = 5;

    private static final int PISTON_EXTEND = 0;
    private static final int PISTON_RETRACT = 1;
    private static final int BELL_RING = 1;
    private static final int BELL_OTHER = 0;
    private static final int CHEST_LID = 1;
    private static final int OPEN_COUNT = 5;

    private static final int AXIS_X = 0;
    private static final int AXIS_Y = 1;
    private static final int AXIS_Z = 2;
    private static final int NO_AXIS = -1;

    private static final int CHARGE = 5 << 6;
    private static final int BLOCK_STATE_ID = 77;

    @Test
    void aPistonEventCarriesItsDirectionTurned() {
        assertEquals(SOUTH, EventDirections.turnedBlockEventData(Blocks.PISTON, PISTON_EXTEND, EAST, CLOCKWISE));
        assertEquals(NORTH,
                EventDirections.turnedBlockEventData(Blocks.STICKY_PISTON, PISTON_RETRACT, WEST, CLOCKWISE));
    }

    @Test
    void aPistonPointingAlongYKeepsItsDirection() {
        assertEquals(UP, EventDirections.turnedBlockEventData(Blocks.PISTON, PISTON_EXTEND, UP, CLOCKWISE));
        assertEquals(DOWN, EventDirections.turnedBlockEventData(Blocks.PISTON, PISTON_EXTEND, DOWN, CLOCKWISE));
    }

    @Test
    void aBellRingCarriesItsDirectionTurnedAndItsOtherEventsStay() {
        assertEquals(EAST, EventDirections.turnedBlockEventData(Blocks.BELL, BELL_RING, NORTH, CLOCKWISE));
        assertEquals(NORTH, EventDirections.turnedBlockEventData(Blocks.BELL, BELL_OTHER, NORTH, CLOCKWISE));
    }

    @Test
    void aBlockEventWithoutADirectionIsUnchanged() {
        assertEquals(OPEN_COUNT,
                EventDirections.turnedBlockEventData(Blocks.CHEST, CHEST_LID, OPEN_COUNT, CLOCKWISE));
    }

    @Test
    void theSmokeEventsCarryTheirDirectionTurned() {
        for (int type : new int[] {LevelEvent.PARTICLES_SHOOT_SMOKE, LevelEvent.PARTICLES_SHOOT_WHITE_SMOKE}) {
            assertEquals(NORTH, EventDirections.turnedLevelEventData(type, WEST, CLOCKWISE), "event " + type);
            assertEquals(UP, EventDirections.turnedLevelEventData(type, UP, CLOCKWISE), "event " + type);
        }
    }

    @Test
    void theSparkEventSwapsItsHorizontalAxisAndKeepsTheRest() {
        int type = LevelEvent.PARTICLES_ELECTRIC_SPARK;
        assertEquals(AXIS_Z, EventDirections.turnedLevelEventData(type, AXIS_X, CLOCKWISE));
        assertEquals(AXIS_X, EventDirections.turnedLevelEventData(type, AXIS_Z, CLOCKWISE));
        assertEquals(AXIS_Y, EventDirections.turnedLevelEventData(type, AXIS_Y, CLOCKWISE));
        assertEquals(NO_AXIS, EventDirections.turnedLevelEventData(type, NO_AXIS, CLOCKWISE));
        assertEquals(AXIS_X, EventDirections.turnedLevelEventData(type, AXIS_X, FoldOrientation.HALF_TURN));
    }

    @Test
    void theSculkChargeEventTurnsItsFaceSetAndKeepsTheCharge() {
        int sent = CHARGE | (1 << NORTH) | (1 << UP);
        int turned = CHARGE | (1 << EAST) | (1 << UP);
        assertEquals(turned,
                EventDirections.turnedLevelEventData(LevelEvent.PARTICLES_SCULK_CHARGE, sent, CLOCKWISE));
    }

    @Test
    void aLevelEventWithoutADirectionIsUnchanged() {
        assertEquals(BLOCK_STATE_ID,
                EventDirections.turnedLevelEventData(LevelEvent.PARTICLES_DESTROY_BLOCK, BLOCK_STATE_ID, CLOCKWISE));
    }

    @Test
    void anUprightSeatKeepsEveryEvent() {
        assertEquals(EAST,
                EventDirections.turnedBlockEventData(Blocks.PISTON, PISTON_EXTEND, EAST, FoldOrientation.IDENTITY));
        assertEquals(WEST, EventDirections.turnedLevelEventData(
                LevelEvent.PARTICLES_SHOOT_SMOKE, WEST, FoldOrientation.IDENTITY));
    }
}
