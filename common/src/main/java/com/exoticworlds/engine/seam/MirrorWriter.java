package com.exoticworlds.engine.seam;

public enum MirrorWriter {
    PLAYER_MOVE("player_move", true),
    VEHICLE_MOVE("vehicle_move", true),
    PASSENGER("passenger", true),
    POSITION_PACKET("position_packet", false);

    private final String key;
    private final boolean isSeatingNeeded;

    MirrorWriter(String key, boolean isSeatingNeeded) {
        this.key = key;
        this.isSeatingNeeded = isSeatingNeeded;
    }

    public String getKey() {
        return key;
    }

    public boolean isSeatingNeeded() {
        return isSeatingNeeded;
    }
}
