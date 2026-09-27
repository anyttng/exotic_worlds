package com.toroidalworld.engine.net;

import com.toroidalworld.platform.Platforms;

import net.minecraft.server.level.ServerPlayer;

public final class PositionRowsSync {
    private static volatile PositionRows current = PositionRows.EMPTY;

    static void apply(PositionRows rows) {
        current = rows;
        SpawnBufferFold.declare(rows.entities());
        RecordPayloadFold.deny(rows.deny());
        ComponentPositions.declare(rows.components());
    }

    public static void sendTo(ServerPlayer player) {
        PositionRows rows = current;
        Platforms.get().sendPositionRows(player, new PositionRowsPayload(rows.blockEntities(), rows.components()));
    }

    private PositionRowsSync() {
    }
}
