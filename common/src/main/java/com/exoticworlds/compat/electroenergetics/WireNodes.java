package com.exoticworlds.compat.electroenergetics;

import java.util.function.UnaryOperator;

import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNodeConnection;
import com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeHelper;

import net.minecraft.core.BlockPos;

public final class WireNodes {
    public static InWorldNode moved(InWorldNode node, UnaryOperator<BlockPos> move) {
        if (DetachedNodeHelper.isDetached(node)) {
            return node;
        }

        BlockPos pos = move.apply(node.sourcePos());
        return pos.equals(node.sourcePos()) ? node : new InWorldNode(node.id(), pos);
    }

    public static InWorldNodeConnection moved(InWorldNodeConnection connection, UnaryOperator<BlockPos> move) {
        InWorldNode node1 = moved(connection.node1(), move);
        InWorldNode node2 = moved(connection.node2(), move);
        return node1 == connection.node1() && node2 == connection.node2()
                ? connection
                : new InWorldNodeConnection(node1, node2);
    }

    private WireNodes() {
    }
}
