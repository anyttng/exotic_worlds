package com.exoticworlds.engine.seam;

public enum MirrorWriter {
    PLAYER_MOVE("player_move", true),
    VEHICLE_MOVE("vehicle_move", true),
    PASSENGER("passenger", true),
    POSITION_PACKET("position_packet", false);

    private final String key;
    private final boolean needsSeating;

    MirrorWriter(String key, boolean needsSeating) {
        this.key = key;
        this.needsSeating = needsSeating;
    }

    public String key() {
        return key;
    }

    public boolean needsSeating() {
        return needsSeating;
    }
}
