package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.registry.StartupRegistry;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.mixin.InitializeBorderPacketAccessor;
import com.exoticworlds.mixin.SetBorderCenterPacketAccessor;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class PacketTranslator {
    @FunctionalInterface
    public interface ParticleTranslation<P extends ParticleOptions> {
        ParticleOptions rewrite(P particle, TranslationContext context, Vec3 clientOrigin);
    }

    private static final int SINGLE_SEAT = 1;

    private static final PayloadRoute<ClientboundCustomPayloadPacket> CLIENTBOUND_PAYLOADS = new PayloadRoute<>(
            new StartupRegistry<>("Clientbound payload rewriters"), RecordPayloadFold::toClient,
            ClientboundCustomPayloadPacket::new);

    private static final PayloadRoute<ServerboundCustomPayloadPacket> SERVERBOUND_PAYLOADS = new PayloadRoute<>(
            new StartupRegistry<>("Serverbound payload rewriters"), RecordPayloadFold::toServer,
            ServerboundCustomPayloadPacket::new);

    private static final StartupRegistry<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>>
            CHUNK_PAYLOAD_REWRITERS = new StartupRegistry<>("Clientbound chunk payload rewriters");

    private static final Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> TO_CLIENT = Map.ofEntries(
            Map.entry(ClientboundLevelChunkWithLightPacket.class, perCopy(ClientboundPacketRewriters::levelChunk)),
            Map.entry(ClientboundLightUpdatePacket.class, perCopy(ClientboundPacketRewriters::lightUpdate)),
            Map.entry(ClientboundForgetLevelChunkPacket.class, rewriter(ClientboundPacketRewriters::forgetChunk)),
            Map.entry(ClientboundSetChunkCacheCenterPacket.class, rewriter(ClientboundPacketRewriters::chunkCacheCenter)),
            Map.entry(ClientboundChunksBiomesPacket.class, perCopy(ClientboundPacketRewriters::chunkBiomes)),
            Map.entry(ClientboundPlayerPositionPacket.class, rewriter(ClientboundPacketRewriters::playerPosition)),
            Map.entry(ClientboundPlayerRotationPacket.class, rewriter(ClientboundPacketRewriters::playerRotation)),
            Map.entry(ClientboundBlockUpdatePacket.class, perCopy(ClientboundPacketRewriters::blockUpdate)),
            Map.entry(ClientboundSectionBlocksUpdatePacket.class, perCopy(ClientboundPacketRewriters::sectionBlocksUpdate)),
            Map.entry(ClientboundBlockEntityDataPacket.class, perCopy(ClientboundPacketRewriters::blockEntityData)),
            Map.entry(ClientboundBlockDestructionPacket.class, rewriter(ClientboundPacketRewriters::blockDestruction)),
            Map.entry(ClientboundSetEntityDataPacket.class, rewriter(ClientboundPacketRewriters::setEntityData)),
            Map.entry(ClientboundAddEntityPacket.class, rewriter(ClientboundPacketRewriters::addEntity)),
            Map.entry(ClientboundTeleportEntityPacket.class, rewriter(ClientboundPacketRewriters::teleportEntity)),
            Map.entry(ClientboundEntityPositionSyncPacket.class, rewriter(ClientboundPacketRewriters::entityPositionSync)),
            Map.entry(ClientboundMoveVehiclePacket.class, rewriter(ClientboundPacketRewriters::moveVehicle)),
            Map.entry(ClientboundBlockEventPacket.class, perCopy(ClientboundPacketRewriters::blockEvent)),
            Map.entry(ClientboundOpenSignEditorPacket.class, rewriter(ClientboundPacketRewriters::openSignEditor)),
            Map.entry(ClientboundLevelEventPacket.class, rewriter(ClientboundPacketRewriters::levelEvent)),
            Map.entry(ClientboundSoundPacket.class, rewriter(ClientboundPacketRewriters::sound)),
            Map.entry(ClientboundLevelParticlesPacket.class, rewriter(ClientboundPacketRewriters::levelParticles)),
            Map.entry(ClientboundExplodePacket.class, rewriter(ClientboundPacketRewriters::explode)),
            Map.entry(ClientboundTrackedWaypointPacket.class, rewriter(ClientboundPacketRewriters::trackedWaypoint)),
            Map.entry(ClientboundSetDefaultSpawnPositionPacket.class, rewriter(ClientboundPacketRewriters::setDefaultSpawnPosition)),
            Map.entry(ClientboundInitializeBorderPacket.class, rewriter(
                    (ClientboundInitializeBorderPacket packet, TranslationContext context) ->
                            ClientboundPacketRewriters.borderCenter(packet, context,
                                    (borderPacket, output) -> ((InitializeBorderPacketAccessor) borderPacket).exotic_worlds$write(output),
                                    InitializeBorderPacketAccessor::exotic_worlds$create))),
            Map.entry(ClientboundSetBorderCenterPacket.class, rewriter(
                    (ClientboundSetBorderCenterPacket packet, TranslationContext context) ->
                            ClientboundPacketRewriters.borderCenter(packet, context,
                                    (borderPacket, output) -> ((SetBorderCenterPacketAccessor) borderPacket).exotic_worlds$write(output),
                                    SetBorderCenterPacketAccessor::exotic_worlds$create))),
            Map.entry(ClientboundPlayerLookAtPacket.class, rewriter(ClientboundPacketRewriters::playerLookAt)),
            Map.entry(ClientboundDamageEventPacket.class, rewriter(ClientboundPacketRewriters::damageEvent)),
            Map.entry(ClientboundMoveMinecartPacket.class, rewriter(ClientboundPacketRewriters::moveMinecart)),
            Map.entry(ClientboundCustomPayloadPacket.class, rewriter(PacketTranslator::clientboundPayload)));

    private static final Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> TO_TURNED_CLIENT = Map.of(
            ClientboundMoveEntityPacket.Pos.class, rewriter(EntityPacketTurns::moveEntity),
            ClientboundMoveEntityPacket.PosRot.class, rewriter(EntityPacketTurns::moveEntity),
            ClientboundMoveEntityPacket.Rot.class, rewriter(EntityPacketTurns::moveEntity),
            ClientboundRotateHeadPacket.class, rewriter(EntityPacketTurns::rotateHead),
            ClientboundSetEntityMotionPacket.class, rewriter(EntityPacketTurns::entityMotion));

    public static <P extends CustomPacketPayload> void registerClientboundPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        CLIENTBOUND_PAYLOADS.rewriters().register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    public static <P extends CustomPacketPayload> void registerClientboundChunkPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        CHUNK_PAYLOAD_REWRITERS.register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    private static Packet<?> clientboundPayload(ClientboundCustomPayloadPacket packet, TranslationContext context) {
        BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> chunkRewriter =
                CHUNK_PAYLOAD_REWRITERS.getEntries().get(packet.payload().getClass());
        return chunkRewriter == null
                ? CLIENTBOUND_PAYLOADS.rewrite(packet, packet.payload(), context)
                : perCopy((sent, seated) -> CLIENTBOUND_PAYLOADS.rewrite(packet, packet.payload(), seated, chunkRewriter),
                        packet, context);
    }

    public static <P extends CustomPacketPayload> void registerServerboundPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        SERVERBOUND_PAYLOADS.rewriters().register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    private static <P extends CustomPacketPayload> BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>
            castingRewriter(Class<P> payloadType, BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        return (payload, context) -> payloadRewriter.apply(payloadType.cast(payload), context);
    }

    private static final StartupRegistry<Class<?>, ParticleTranslation<ParticleOptions>> PARTICLE_REWRITERS =
            new StartupRegistry<>("Particle rewriters");

    public static <P extends ParticleOptions> void registerParticleRewriter(Class<P> particleType,
            ParticleTranslation<P> particleRewriter) {
        PARTICLE_REWRITERS.register(particleType,
                (particle, context, clientOrigin) -> particleRewriter.rewrite(particleType.cast(particle), context, clientOrigin));
    }

    static @Nullable ParticleTranslation<ParticleOptions> getParticleRewriter(ParticleOptions particle) {
        return PARTICLE_REWRITERS.getEntries().get(particle.getClass());
    }

    private static final Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> TO_SERVER = Map.ofEntries(
            Map.entry(ServerboundUseItemOnPacket.class, rewriter(ServerboundPacketRewriters::useItemOn)),
            Map.entry(ServerboundPlayerActionPacket.class, rewriter(ServerboundPacketRewriters::playerAction)),
            Map.entry(ServerboundPickItemFromBlockPacket.class, rewriter(ServerboundPacketRewriters::pickItemFromBlock)),
            Map.entry(ServerboundSignUpdatePacket.class, rewriter(ServerboundPacketRewriters::signUpdate)),
            Map.entry(ServerboundBlockEntityTagQueryPacket.class, rewriter(ServerboundPacketRewriters::blockEntityTagQuery)),
            Map.entry(ServerboundJigsawGeneratePacket.class, rewriter(ServerboundPacketRewriters::jigsawGenerate)),
            Map.entry(ServerboundSetCommandBlockPacket.class, rewriter(ServerboundPacketRewriters::setCommandBlock)),
            Map.entry(ServerboundSetJigsawBlockPacket.class, rewriter(ServerboundPacketRewriters::setJigsawBlock)),
            Map.entry(ServerboundSetStructureBlockPacket.class, rewriter(ServerboundPacketRewriters::setStructureBlock)),
            Map.entry(ServerboundSetTestBlockPacket.class, rewriter(ServerboundPacketRewriters::setTestBlock)),
            Map.entry(ServerboundTestInstanceBlockActionPacket.class, rewriter(ServerboundPacketRewriters::testInstanceBlockAction)),
            Map.entry(ServerboundSetCreativeModeSlotPacket.class, rewriter(ServerboundPacketRewriters::setCreativeModeSlot)),
            Map.entry(ServerboundCustomPayloadPacket.class, rewriter(
                    (ServerboundCustomPayloadPacket packet, TranslationContext context) ->
                            SERVERBOUND_PAYLOADS.rewrite(packet, packet.payload(), context))));

    private static final PacketRoute CLIENTBOUND = new PacketRoute(TO_CLIENT, TO_TURNED_CLIENT,
            new StartupRegistry<>("Clientbound packet rewriters"), RecordPayloadFold::toClient,
            TranslationContext::followingCamera);

    private static final PacketRoute SERVERBOUND = new PacketRoute(TO_SERVER, Map.of(),
            new StartupRegistry<>("Serverbound packet rewriters"), RecordPayloadFold::toServer,
            TranslationContext::of);

    public static <P extends Packet<?>> void registerClientboundPacketRewriter(Class<P> packetType,
            BiFunction<P, TranslationContext, Packet<?>> packetRewriter) {
        CLIENTBOUND.rewriters().register(packetType, rewriter(packetRewriter));
    }

    public static <P extends Packet<?>> void registerServerboundPacketRewriter(Class<P> packetType,
            BiFunction<P, TranslationContext, Packet<?>> packetRewriter) {
        SERVERBOUND.rewriters().register(packetType, rewriter(packetRewriter));
    }

    public static <T extends net.minecraft.network.PacketListener> Packet<T> toClient(Packet<T> packet,
            ServerPlayer player) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(player.level());
        if (fold == null) {
            return packet;
        }

        if (packet instanceof ClientboundBundlePacket bundle) {
            return castPacket(toClientBundle(bundle, fold, () -> CLIENTBOUND.contextOf().apply(player, fold)));
        }

        return dispatch(CLIENTBOUND, packet, player, fold);
    }

    static <T extends net.minecraft.network.PacketListener> Packet<T> toClient(Packet<T> packet,
            TranslationContext context) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            return castPacket(toClientBundle(bundle, context.fold(), () -> context));
        }

        return castPacket(rewrite(CLIENTBOUND, packet, context));
    }

    @SuppressWarnings("unchecked")
    private static Packet<?> toClientBundle(ClientboundBundlePacket bundle, WorldFold fold,
            Supplier<TranslationContext> contextOf) {
        TranslationContext context = null;
        List<Packet<? super ClientGamePacketListener>> translated = new ArrayList<>();
        boolean isChanged = false;
        for (Packet<? super ClientGamePacketListener> sub : bundle.subPackets()) {
            BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = CLIENTBOUND.getRewriter(sub, fold);
            if (rewriter == null) {
                translated.add(sub);
                continue;
            }

            if (context == null) {
                context = contextOf.get();
            }

            Packet<? super ClientGamePacketListener> translatedSub =
                    (Packet<? super ClientGamePacketListener>) rewriter.apply(sub, context);
            isChanged |= translatedSub != sub;
            if (translatedSub instanceof ClientboundBundlePacket copies) {
                copies.subPackets().forEach(translated::add);
            } else {
                translated.add(translatedSub);
            }
        }

        return isChanged ? new ClientboundBundlePacket(translated) : bundle;
    }

    public static <T extends net.minecraft.network.PacketListener> Packet<T> toServer(Packet<T> packet, ServerPlayer player) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(player.level());
        if (fold == null) {
            return packet;
        }

        return dispatch(SERVERBOUND, packet, player, fold);
    }

    static <T extends net.minecraft.network.PacketListener> Packet<T> toServer(Packet<T> packet, TranslationContext context) {
        return castPacket(rewrite(SERVERBOUND, packet, context));
    }

    private static <T extends net.minecraft.network.PacketListener> Packet<T> dispatch(PacketRoute route,
            Packet<T> packet, ServerPlayer player, WorldFold fold) {
        BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = route.getRewriter(packet, fold);
        return rewriter == null ? packet
                : castPacket(rewriter.apply(packet, route.contextOf().apply(player, fold)));
    }

    private static Packet<?> rewrite(PacketRoute route, Packet<?> packet, TranslationContext context) {
        BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = route.getRewriter(packet, context.fold());
        return rewriter == null ? packet : rewriter.apply(packet, context);
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.network.PacketListener> Packet<T> castPacket(Packet<?> packet) {
        return (Packet<T>) packet;
    }

    @SuppressWarnings("unchecked")
    private static <P extends Packet<?>> BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter(BiFunction<P, TranslationContext, Packet<?>> typed) {
        return (packet, player) -> typed.apply((P) packet, player);
    }

    private static <P extends Packet<?>> BiFunction<Packet<?>, TranslationContext, Packet<?>> perCopy(
            BiFunction<P, TranslationContext, Packet<?>> typed) {
        BiFunction<Packet<?>, TranslationContext, Packet<?>> once = rewriter(typed);
        return (packet, context) -> perCopy(once, packet, context);
    }

    @SuppressWarnings("unchecked")
    private static Packet<?> perCopy(BiFunction<Packet<?>, TranslationContext, Packet<?>> once, Packet<?> packet,
            TranslationContext context) {
        SeatPass seatPass = context.seatPass();
        seatPass.reset();
        Packet<?> held = once.apply(packet, context);
        int seats = seatPass.getSeats();
        if (seats == SINGLE_SEAT) {
            return held;
        }

        List<Packet<? super ClientGamePacketListener>> copies = new ArrayList<>(seats);
        copies.add((Packet<? super ClientGamePacketListener>) held);
        try {
            for (int seat = SINGLE_SEAT; seat < seats; seat++) {
                seatPass.answer(seat);
                copies.add((Packet<? super ClientGamePacketListener>) once.apply(packet, context));
            }
        } finally {
            seatPass.reset();
        }

        return new ClientboundBundlePacket(copies);
    }

    private record PacketRoute(Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> table,
            Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> turnedTable,
            StartupRegistry<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> rewriters,
            BiFunction<Packet<?>, TranslationContext, Packet<?>> recordFold,
            BiFunction<ServerPlayer, WorldFold, TranslationContext> contextOf) {
        @Nullable BiFunction<Packet<?>, TranslationContext, Packet<?>> getRewriter(Packet<?> packet, WorldFold fold) {
            BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = this.table.get(packet.getClass());
            if (rewriter == null && !fold.isPreservingLocalIndices()) {
                rewriter = this.turnedTable.get(packet.getClass());
            }

            if (rewriter == null) {
                rewriter = this.rewriters.getEntries().get(packet.getClass());
            }

            if (rewriter == null && isModPacket(packet)
                    && RecordPayloadFold.getForm(packet.getClass()).hasPositions()) {
                rewriter = this.recordFold;
            }

            return rewriter;
        }

        private static boolean isModPacket(Packet<?> packet) {
            return !packet.type().id().getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
        }
    }

    private record PayloadRoute<P extends Packet<?>>(
            StartupRegistry<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>> rewriters,
            BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> recordFold,
            Function<CustomPacketPayload, P> wrap) {
        Packet<?> rewrite(P packet, CustomPacketPayload payload, TranslationContext context) {
            BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> payloadRewriter =
                    this.rewriters.getEntries().get(payload.getClass());
            return rewrite(packet, payload, context, payloadRewriter == null ? this.recordFold : payloadRewriter);
        }

        Packet<?> rewrite(P packet, CustomPacketPayload payload, TranslationContext context,
                BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> payloadRewriter) {
            CustomPacketPayload rewritten = payloadRewriter.apply(payload, context);
            return rewritten == payload ? packet : this.wrap.apply(rewritten);
        }
    }

    private PacketTranslator() {
    }
}
