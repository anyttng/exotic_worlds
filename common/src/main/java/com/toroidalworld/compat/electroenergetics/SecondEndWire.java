package com.toroidalworld.compat.electroenergetics;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNodeConnection;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeHelper;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;

import net.createmod.catnip.data.Pair;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SecondEndWire extends Pair<InWorldNodeConnection, WireData> {
    private SecondEndWire(InWorldNodeConnection connection, WireData data) {
        super(connection, data);
    }

    public static List<Pair<InWorldNodeConnection, WireData>> withSecondEnds(Level level,
            List<Pair<InWorldNodeConnection, WireData>> wires) {
        WorldFold fold = WorldLoopAttachments.transformerOfReader(level);
        List<Pair<InWorldNodeConnection, WireData>> passes = null;
        for (Pair<InWorldNodeConnection, WireData> wire : wires) {
            if (parted(fold, level, wire.getFirst())) {
                passes = passes == null ? new ArrayList<>(wires) : passes;
                passes.add(new SecondEndWire(wire.getFirst(), wire.getSecond()));
            }
        }

        return passes == null ? wires : passes;
    }

    public static int end(Pair<?, ?> wire) {
        return wire instanceof SecondEndWire ? WireCopies.SECOND_END : WireCopies.FIRST_END;
    }

    private static boolean parted(WorldFold fold, Level level, InWorldNodeConnection connection) {
        Vec3 pos1 = position(level, connection.node1());
        Vec3 pos2 = position(level, connection.node2());
        return pos1 != null && pos2 != null && WireCopies.parted(fold, pos1, pos2);
    }

    private static @Nullable Vec3 position(Level level, InWorldNode node) {
        Vec3 pos = node.getPosition(level);
        return pos != null || DetachedNodeHelper.isDetached(node) ? pos : node.sourcePos().getCenter();
    }
}
