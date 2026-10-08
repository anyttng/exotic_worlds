package com.exoticworlds.compat.electroenergetics;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryConnection;
import com.george_vi.electroenergetics.content.railway_electrification.catenary.ClearCatenaryPacket;
import com.george_vi.electroenergetics.content.railway_electrification.catenary.SendCatenaryPacket;
import com.george_vi.electroenergetics.content.wire.ClearWireConnectionsPacket;
import com.george_vi.electroenergetics.content.wire.SendWireConnectionsPacket;
import com.george_vi.electroenergetics.content.wire.SendWireParticlesPacket;
import com.george_vi.electroenergetics.content.wire.interaction.InteractWirePacket;
import com.george_vi.electroenergetics.content.wire_spool.ChangeLengthWirePacket;
import com.george_vi.electroenergetics.foundation.nodes.DirectionalInWorldNodeConnection;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNodeConnection;
import com.george_vi.electroenergetics.foundation.nodes.NodeConnectionPoint;
import com.george_vi.electroenergetics.simulation.RequestVoltageDataPacket;
import com.george_vi.electroenergetics.simulation.SendVoltageDataPacket;
import com.george_vi.electroenergetics.simulation.infrastructure.SendNodeDataPacket;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.exoticworlds.compat.electroenergetics.mixin.SendVoltageDataPacketAccessor;
import com.exoticworlds.engine.fold.FoldedCopies;
import com.exoticworlds.engine.net.ComponentPositions;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

// Every payload of Create: Electro Energetics 1.21.1-1.1.1 that carries a node position, enumerated from its read
// code; a bump of electroenergetics_version means reading it again.
public final class ElectroEnergeticsTranslation {
    private static final String NAMESPACE = "electroenergetics";
    private static final ResourceLocation SELECTED_NODE_ID =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "selected_node");
    private static final ResourceLocation NODE_CONNECTION_ID =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "node_connection");

    private static final ComponentPositions.Mover NODE_MOVER = (stored, seat) -> {
        UnaryOperator<BlockPos> move = pos -> (BlockPos) seat.apply(pos);
        return switch (stored) {
            case InWorldNode node -> WireNodes.moved(node, move);
            case InWorldNodeConnection connection -> WireNodes.moved(connection, move);
            default -> stored;
        };
    };

    public static void register() {
        if (!ElectroEnergeticsMod.present()) {
            return;
        }

        registerWires();
        registerCatenary();
        registerNodes();
        registerRequests();
        registerComponents();
    }

    private static void registerComponents() {
        ComponentPositions.register(SELECTED_NODE_ID, NODE_MOVER);
        ComponentPositions.register(NODE_CONNECTION_ID, NODE_MOVER);
    }

    private static void registerWires() {
        PacketTranslator.registerClientboundPayloadRewriter(SendWireConnectionsPacket.class, (payload, context) -> {
            List<Pair<DirectionalInWorldNodeConnection, WireData>> seated =
                    FoldedCopies.of(payload.connections(), wire -> seatWire(context, wire));
            return seated == payload.connections() ? payload : new SendWireConnectionsPacket(seated);
        });

        PacketTranslator.registerClientboundPayloadRewriter(ClearWireConnectionsPacket.class, (payload, context) -> {
            List<Pair<InWorldNode, InWorldNode>> seated =
                    FoldedCopies.of(payload.connections(), ends -> seatEnds(context, ends));
            return seated == payload.connections() ? payload : new ClearWireConnectionsPacket(seated, payload.all());
        });

        PacketTranslator.registerClientboundPayloadRewriter(SendWireParticlesPacket.class, (payload, context) -> {
            InWorldNode node1 = seat(context, payload.node1());
            InWorldNode node2 = seat(context, payload.node2());
            return node1 == payload.node1() && node2 == payload.node2()
                    ? payload
                    : new SendWireParticlesPacket(node1, node2, payload.options(), payload.sag(), payload.chance());
        });
    }

    private static void registerCatenary() {
        PacketTranslator.registerClientboundPayloadRewriter(SendCatenaryPacket.class, (payload, context) -> {
            List<CatenaryConnection> seated = FoldedCopies.of(payload.connections(), line -> seat(context, line));
            return seated == payload.connections() ? payload : new SendCatenaryPacket(seated);
        });

        PacketTranslator.registerClientboundPayloadRewriter(ClearCatenaryPacket.class, (payload, context) -> {
            List<CatenaryConnection> seated = FoldedCopies.of(payload.connections(), line -> seat(context, line));
            return seated == payload.connections() ? payload : new ClearCatenaryPacket(seated, payload.all());
        });
    }

    private static void registerNodes() {
        PacketTranslator.registerClientboundPayloadRewriter(SendNodeDataPacket.class, (payload, context) -> {
            InWorldNode node = seat(context, payload.node());
            Optional<Vec3> detachedPosition = payload.detachedPosition().map(position -> context.toClient(position));
            return node == payload.node() && detachedPosition.equals(payload.detachedPosition())
                    ? payload
                    : new SendNodeDataPacket(node, payload.label(), detachedPosition, payload.remove());
        });

        PacketTranslator.registerClientboundPayloadRewriter(SendVoltageDataPacket.class, (payload, context) -> {
            SendVoltageDataPacketAccessor voltages = (SendVoltageDataPacketAccessor) (Object) payload;
            InWorldNode[] nodes = voltages.toroidal$nodes();
            InWorldNode[] seated = null;
            for (int i = 0; i < nodes.length; i++) {
                InWorldNode node = seat(context, nodes[i]);
                if (node != nodes[i]) {
                    seated = seated == null ? nodes.clone() : seated;
                    seated[i] = node;
                }
            }

            return seated == null ? payload : withNodes(voltages, seated);
        });
    }

    private static void registerRequests() {
        PacketTranslator.registerServerboundPayloadRewriter(InteractWirePacket.class, (payload, context) -> {
            NodeConnectionPoint point = fold(context, payload.point());
            return point == payload.point() ? payload : new InteractWirePacket(point);
        });

        PacketTranslator.registerServerboundPayloadRewriter(ChangeLengthWirePacket.class, (payload, context) -> {
            NodeConnectionPoint point = fold(context, payload.point());
            return point == payload.point() ? payload : new ChangeLengthWirePacket(point, payload.delta());
        });

        PacketTranslator.registerServerboundPayloadRewriter(RequestVoltageDataPacket.class, (payload, context) -> {
            InWorldNode node = fold(context, payload.node());
            return node == payload.node() ? payload : new RequestVoltageDataPacket(node);
        });
    }

    private static SendVoltageDataPacket withNodes(SendVoltageDataPacketAccessor source, InWorldNode[] nodes) {
        SendVoltageDataPacket copy = new SendVoltageDataPacket();
        SendVoltageDataPacketAccessor target = (SendVoltageDataPacketAccessor) (Object) copy;
        target.toroidal$setNodes(nodes);
        target.toroidal$setVoltages(source.toroidal$voltages());
        target.toroidal$setMicroTicks(source.toroidal$microTicks());
        target.toroidal$setFrequencies(source.toroidal$frequencies());
        return copy;
    }

    private static Pair<DirectionalInWorldNodeConnection, WireData> seatWire(TranslationContext context,
            Pair<DirectionalInWorldNodeConnection, WireData> wire) {
        DirectionalInWorldNodeConnection connection = wire.getFirst();
        InWorldNode node1 = seat(context, connection.node1());
        InWorldNode node2 = seat(context, connection.node2());
        return node1 == connection.node1() && node2 == connection.node2()
                ? wire
                : Pair.of(new DirectionalInWorldNodeConnection(node1, node2), wire.getSecond());
    }

    private static Pair<InWorldNode, InWorldNode> seatEnds(TranslationContext context,
            Pair<InWorldNode, InWorldNode> ends) {
        InWorldNode first = seat(context, ends.getFirst());
        InWorldNode second = seat(context, ends.getSecond());
        return first == ends.getFirst() && second == ends.getSecond() ? ends : Pair.of(first, second);
    }

    private static CatenaryConnection seat(TranslationContext context, CatenaryConnection line) {
        BlockPos pos1 = context.nearestCopy(line.pos1());
        BlockPos pos2 = context.nearestCopy(line.pos2());
        return pos1.equals(line.pos1()) && pos2.equals(line.pos2()) ? line : new CatenaryConnection(pos1, pos2);
    }

    private static NodeConnectionPoint fold(TranslationContext context, NodeConnectionPoint point) {
        InWorldNode node1 = fold(context, point.node1());
        InWorldNode node2 = fold(context, point.node2());
        return node1 == point.node1() && node2 == point.node2()
                ? point
                : new NodeConnectionPoint(node1, node2, point.point());
    }

    private static InWorldNode seat(TranslationContext context, InWorldNode node) {
        return WireNodes.moved(node, pos -> context.nearestCopy(pos));
    }

    private static InWorldNode fold(TranslationContext context, InWorldNode node) {
        return WireNodes.moved(node, pos -> context.toServer(pos));
    }

    private ElectroEnergeticsTranslation() {
    }
}
