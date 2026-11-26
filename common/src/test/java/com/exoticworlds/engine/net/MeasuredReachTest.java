package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeasuredReachTest {
    private static final double PARTICLE_RADIUS_BLOCKS = 32.0;

    private static final double SOUND_RADIUS_BLOCKS = 16.0;

    @Test
    void nothingIsMeasuredOutsideASend() {
        assertEquals(MeasuredReach.UNMEASURED, MeasuredReach.getBlocks());
    }

    @Test
    void theRadiusStandsForTheLengthOfTheSend() {
        try (MeasuredReach _ = MeasuredReach.measuring(PARTICLE_RADIUS_BLOCKS)) {
            assertEquals(PARTICLE_RADIUS_BLOCKS, MeasuredReach.getBlocks());
        }

        assertEquals(MeasuredReach.UNMEASURED, MeasuredReach.getBlocks());
    }

    @Test
    void anInnerSendGivesTheOuterRadiusBack() {
        try (MeasuredReach _ = MeasuredReach.measuring(PARTICLE_RADIUS_BLOCKS)) {
            try (MeasuredReach _ = MeasuredReach.measuring(SOUND_RADIUS_BLOCKS)) {
                assertEquals(SOUND_RADIUS_BLOCKS, MeasuredReach.getBlocks());
            }

            assertEquals(PARTICLE_RADIUS_BLOCKS, MeasuredReach.getBlocks());
        }
    }
}
