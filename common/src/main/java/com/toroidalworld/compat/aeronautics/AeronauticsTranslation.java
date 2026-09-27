package com.toroidalworld.compat.aeronautics;

import java.util.Map;

import org.joml.Vector3d;

import com.toroidalworld.client.engine.SyncedTagFold;
import com.toroidalworld.compat.aeronautics.mixin.MultiMiningSyncAccessor;
import com.toroidalworld.compat.aeronautics.mixin.PhysicsStaffBeamPacketAccessor;
import com.toroidalworld.engine.fold.FoldedCopies;
import com.toroidalworld.core.JomlVectors;
import com.toroidalworld.engine.net.ComponentPositions;
import com.toroidalworld.engine.net.PacketTranslator;
import com.toroidalworld.engine.net.SpawnBufferFold;
import com.toroidalworld.engine.net.TagPositions;
import com.toroidalworld.engine.net.TranslationContext;

import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.index.SimEntityDataSerializers;
import dev.simulated_team.simulated.network.packets.lodestone_compass.UpdateClientLodestonePositionPacket;
import dev.ryanhcode.offroad.handlers.server.MultiMiningServerManager;
import dev.ryanhcode.offroad.network.borehead_bearing.ClientboundMultiMiningSync;
import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffBeamPacket;
import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffDragSessionsPacket;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

// Nothing on a CompoundTag key or a payload component says it holds a world position, so every list below is
// enumerated from the read code of the Aeronautics 1.3.2 bundle and a bump of aeronautics_version means reading
// it again.
public final class AeronauticsTranslation {
    private static final String SPRING_GOAL_KEY = "Goal";
    private static final String MERGING_GLUE_PARTNER_KEY = "PartnerPosition";
    private static final String DOCKING_OTHER_CONNECTOR_KEY = "OtherConnector";
    private static final String NAMEPLATE_CONTROLLER_KEY = "ControllerPos";
    private static final String SWIVEL_PLATE_KEY = "SwivelPlate";
    private static final String SWIVEL_PARENT_KEY = "ParentPos";
    private static final String NAV_TARGET_KEY = "CurrentTarget";
    private static final String LASER_HIT_KEY = "HitPos";
    private static final String HONEY_GLUE_POS_KEY = "Pos";
    private static final ResourceLocation ROPE_FIRST_CONNECTION_ID =
            ResourceLocation.fromNamespaceAndPath("simulated", "rope_first_connection");

    public static void register() {
        if (SimulatedMod.present()) {
            registerSimulated();
        }

        if (OffroadMod.present()) {
            registerOffroad();
        }
    }

    private static void registerOffroad() {
        // Not a record, so the record fold cannot rebuild it.
        PacketTranslator.registerClientboundPayloadRewriter(ClientboundMultiMiningSync.class, (payload, context) -> {
            ClientboundMultiMiningSync seated = ClientboundMultiMiningSync.serverOutboundData(
                    ((MultiMiningSyncAccessor) (Object) payload).toroidal$breakingId());
            for (Map.Entry<BlockPos, MultiMiningServerManager.BlockBreakingData> entry : payload.inData.entrySet()) {
                seated.inData.put(seat(context, entry.getKey()), entry.getValue());
            }

            return seated;
        });
    }

    private static void registerSimulated() {
        registerSimulatedSyncedTags();
        ComponentPositions.register(ROPE_FIRST_CONNECTION_ID);
        SpawnBufferFold.register(HoneyGlueEntity.class, TagPositions.PositionShape.VEC3_LIST, HONEY_GLUE_POS_KEY);

        // A Pair component is out of the record fold's reach.
        PacketTranslator.registerClientboundPayloadRewriter(PhysicsStaffDragSessionsPacket.class, (payload, context) ->
                new PhysicsStaffDragSessionsPacket(payload.dimension(),
                        FoldedCopies.of(payload.sessions(), session ->
                                Pair.of(session.getFirst(), seat(context, session.getSecond())))));

        // A compass target may name any point, so it takes the plain door, not the record fold's guarded one.
        PacketTranslator.registerClientboundPayloadRewriter(UpdateClientLodestonePositionPacket.class, (payload, context) ->
                new UpdateClientLodestonePositionPacket(payload.id(), seat(context, payload.sentPosition())));

        // Not a record, so the record fold cannot rebuild it.
        PacketTranslator.registerClientboundPayloadRewriter(PhysicsStaffBeamPacket.class, (payload, context) -> {
            PhysicsStaffBeamPacketAccessor beam = (PhysicsStaffBeamPacketAccessor) payload;
            return new PhysicsStaffBeamPacket(beam.toroidal$uuid(), seat(context, beam.toroidal$start()),
                    seat(context, beam.toroidal$end()));
        });

        PacketTranslator.registerEntityDataRewriter(SimEntityDataSerializers.VEC3,
                (position, context, anchor) -> context.transformer().nearestCopy(anchor, position));
    }

    private static void registerSimulatedSyncedTags() {
        SyncedTagFold.register(SpringBlockEntity.class, TagPositions.PositionShape.PACKED_LONG, SPRING_GOAL_KEY);
        SyncedTagFold.register(MergingGlueBlockEntity.class, TagPositions.PositionShape.PACKED_LONG,
                MERGING_GLUE_PARTNER_KEY);
        SyncedTagFold.register(DockingConnectorBlockEntity.class, TagPositions.PositionShape.BLOCK_POS,
                DOCKING_OTHER_CONNECTOR_KEY);
        SyncedTagFold.register(NameplateBlockEntity.class, TagPositions.PositionShape.BLOCK_POS,
                NAMEPLATE_CONTROLLER_KEY);
        SyncedTagFold.register(SwivelBearingBlockEntity.class, TagPositions.PositionShape.BLOCK_POS,
                SWIVEL_PLATE_KEY);
        SyncedTagFold.register(SwivelBearingPlateBlockEntity.class, TagPositions.PositionShape.BLOCK_POS,
                SWIVEL_PARENT_KEY);
        SyncedTagFold.register(NavTableBlockEntity.class, TagPositions.PositionShape.VEC3_LIST, NAV_TARGET_KEY);
        SyncedTagFold.register(LaserPointerBlockEntity.class, TagPositions.PositionShape.VEC3_LIST, LASER_HIT_KEY);
    }

    private static BlockPos seat(TranslationContext context, BlockPos pos) {
        return context.nearestCopy(pos);
    }

    private static Vector3d seat(TranslationContext context, Vector3d point) {
        Vec3 raw = JomlVectors.read(point);
        return JomlVectors.seated(point, raw, context.nearestCopy(raw));
    }

    private AeronauticsTranslation() {
    }
}
