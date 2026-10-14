package com.exoticworlds.engine.net;

import com.exoticworlds.core.CoordinateConstants;

public record PacketReach(String kind, double blocks, double slackBlocks) {
    private static final double MIRROR_SLACK = CoordinateConstants.CHUNK_WIDTH;

    private static final double TRACKED_SLACK = MIRROR_SLACK + CoordinateConstants.CHUNK_WIDTH;

    private static final String TRACKED_ENTITY_KIND = "tracked_entity";

    public static PacketReach measured(String kind, double blocks) {
        return new PacketReach(kind, blocks, MIRROR_SLACK);
    }

    public static PacketReach tracked(int viewDistance) {
        return new PacketReach(
                TRACKED_ENTITY_KIND, (double) viewDistance * CoordinateConstants.CHUNK_WIDTH, TRACKED_SLACK);
    }
}
