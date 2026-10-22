package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.net.ParticleRewriter;
import com.exoticworlds.core.StartupRegistry;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
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
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
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

public final class PacketTranslator {
    private static final PayloadRoute<ClientboundCustomPayloadPacket> CLIENTBOUND_PAYLOADS = new PayloadRoute<>(
            new StartupRegistry<>("Clientbound payload rewriters"), RecordPayloadFold::toClient,
            ClientboundCustomPayloadPacket::new);

    private static final PayloadRoute<ServerboundCustomPayloadPacket> SERVERBOUND_PAYLOADS = new PayloadRoute<>(
            new StartupRegistry<>("Serverbound payload rewriters"), RecordPayloadFold::toServer,
            ServerboundCustomPayloadPacket::new);

    private static final Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> TO_CLIENT = Map.ofEntries(
            Map.entry(ClientboundLevelChunkWithLightPacket.class, rewriter(
                    (ClientboundLevelChunkWithLightPacket packet, TranslationContext context) ->
                            ClientboundPacketRewriters.chunkPosition(packet, packet.getX(), packet.getZ(), context))),
            Map.entry(ClientboundLightUpdatePacket.class, rewriter(
                    (ClientboundLightUpdatePacket packet, TranslationContext context) ->
                            ClientboundPacketRewriters.chunkPosition(packet, packet.getX(), packet.getZ(), context))),
            Map.entry(ClientboundForgetLevelChunkPacket.class, rewriter(ClientboundPacketRewriters::forgetChunk)),
            Map.entry(ClientboundSetChunkCacheCenterPacket.class, rewriter(ClientboundPacketRewriters::chunkCacheCenter)),
            Map.entry(ClientboundChunksBiomesPacket.class, rewriter(ClientboundPacketRewriters::chunkBiomes)),
            Map.entry(ClientboundPlayerPositionPacket.class, rewriter(ClientboundPacketRewriters::playerPosition)),
            Map.entry(ClientboundBlockUpdatePacket.class, rewriter(ClientboundPacketRewriters::blockUpdate)),
            Map.entry(ClientboundSectionBlocksUpdatePacket.class, rewriter(ClientboundPacketRewriters::sectionBlocksUpdate)),
            Map.entry(ClientboundBlockEntityDataPacket.class, rewriter(ClientboundPacketRewriters::blockEntityData)),
            Map.entry(ClientboundBlockDestructionPacket.class, rewriter(ClientboundPacketRewriters::blockDestruction)),
            Map.entry(ClientboundSetEntityDataPacket.class, rewriter(ClientboundPacketRewriters::setEntityData)),
            Map.entry(ClientboundAddEntityPacket.class, rewriter(ClientboundPacketRewriters::addEntity)),
            Map.entry(ClientboundTeleportEntityPacket.class, rewriter(ClientboundPacketRewriters::teleportEntity)),
            Map.entry(ClientboundEntityPositionSyncPacket.class, rewriter(ClientboundPacketRewriters::entityPositionSync)),
            Map.entry(ClientboundMoveVehiclePacket.class, rewriter(ClientboundPacketRewriters::moveVehicle)),
            Map.entry(ClientboundBlockEventPacket.class, rewriter(ClientboundPacketRewriters::blockEvent)),
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
            Map.entry(ClientboundCustomPayloadPacket.class, rewriter(
                    (ClientboundCustomPayloadPacket packet, TranslationContext context) ->
                            CLIENTBOUND_PAYLOADS.rewrite(packet, packet.payload(), context))));

    public static <P extends CustomPacketPayload> void registerClientboundPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        CLIENTBOUND_PAYLOADS.rewriters().register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    public static <P extends CustomPacketPayload> void registerServerboundPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        SERVERBOUND_PAYLOADS.rewriters().register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    private static <P extends CustomPacketPayload> BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>
            castingRewriter(Class<P> payloadType, BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        return (payload, context) -> payloadRewriter.apply(payloadType.cast(payload), context);
    }

    private static final StartupRegistry<Class<?>, ParticleRewriter<ParticleOptions>> PARTICLE_REWRITERS =
            new StartupRegistry<>("Particle rewriters");

    public static <P extends ParticleOptions> void registerParticleRewriter(Class<P> particleType,
            ParticleRewriter<P> particleRewriter) {
        PARTICLE_REWRITERS.register(particleType,
                (particle, context, clientOrigin) -> particleRewriter.rewrite(particleType.cast(particle), context, clientOrigin));
    }

    static @Nullable ParticleRewriter<ParticleOptions> particleRewriterFor(ParticleOptions particle) {
        return PARTICLE_REWRITERS.entries().get(particle.getClass());
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

    private static final PacketRoute CLIENTBOUND = new PacketRoute(TO_CLIENT,
            new StartupRegistry<>("Clientbound packet rewriters"), RecordPayloadFold::toClient);

    private static final PacketRoute SERVERBOUND = new PacketRoute(TO_SERVER,
            new StartupRegistry<>("Serverbound packet rewriters"), RecordPayloadFold::toServer);

    public static <P extends Packet<?>> void registerClientboundPacketRewriter(Class<P> packetType,
            BiFunction<P, TranslationContext, Packet<?>> packetRewriter) {
        CLIENTBOUND.rewriters().register(packetType, rewriter(packetRewriter));
    }

    public static <P extends Packet<?>> void registerServerboundPacketRewriter(Class<P> packetType,
            BiFunction<P, TranslationContext, Packet<?>> packetRewriter) {
        SERVERBOUND.rewriters().register(packetType, rewriter(packetRewriter));
    }

    public static <T extends net.minecraft.network.PacketListener> @Nullable Packet<T> toClient(Packet<T> packet,
            ServerPlayer player) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return packet;
        }

        if (packet instanceof ClientboundBundlePacket bundle) {
            return castPacket(toClientBundle(bundle, () -> TranslationContext.of(player, transformer)));
        }

        return dispatch(CLIENTBOUND, packet, player, transformer);
    }

    static <T extends net.minecraft.network.PacketListener> @Nullable Packet<T> toClient(Packet<T> packet,
            TranslationContext context) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            return castPacket(toClientBundle(bundle, () -> context));
        }

        return castPacket(rewrite(CLIENTBOUND, packet, context));
    }

    @SuppressWarnings("unchecked")
    private static Packet<?> toClientBundle(ClientboundBundlePacket bundle, Supplier<TranslationContext> contextOf) {
        TranslationContext context = null;
        List<Packet<? super ClientGamePacketListener>> translated = new ArrayList<>();
        boolean changed = false;
        for (Packet<? super ClientGamePacketListener> sub : bundle.subPackets()) {
            BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = CLIENTBOUND.rewriterOf(sub);
            if (rewriter == null) {
                translated.add(sub);
                continue;
            }

            if (context == null) {
                context = contextOf.get();
            }

            Packet<? super ClientGamePacketListener> translatedSub =
                    (Packet<? super ClientGamePacketListener>) rewriter.apply(sub, context);
            changed |= translatedSub != sub;
            if (translatedSub != null) {
                translated.add(translatedSub);
            }
        }

        return changed ? new ClientboundBundlePacket(translated) : bundle;
    }

    public static <T extends net.minecraft.network.PacketListener> Packet<T> toServer(Packet<T> packet, ServerPlayer player) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return packet;
        }

        return dispatch(SERVERBOUND, packet, player, transformer);
    }

    static <T extends net.minecraft.network.PacketListener> Packet<T> toServer(Packet<T> packet, TranslationContext context) {
        return castPacket(rewrite(SERVERBOUND, packet, context));
    }

    private static <T extends net.minecraft.network.PacketListener> Packet<T> dispatch(PacketRoute route,
            Packet<T> packet, ServerPlayer player, WorldFold transformer) {
        BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = route.rewriterOf(packet);
        return rewriter == null ? packet
                : castPacket(rewriter.apply(packet, TranslationContext.of(player, transformer)));
    }

    private static Packet<?> rewrite(PacketRoute route, Packet<?> packet, TranslationContext context) {
        BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = route.rewriterOf(packet);
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

    private record PacketRoute(Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> table,
            StartupRegistry<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> rewriters,
            BiFunction<Packet<?>, TranslationContext, Packet<?>> recordFold) {
        @Nullable BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriterOf(Packet<?> packet) {
            BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = this.table.get(packet.getClass());
            if (rewriter == null) {
                rewriter = this.rewriters.entries().get(packet.getClass());
            }

            if (rewriter == null && isModPacket(packet)
                    && RecordPayloadFold.formOf(packet.getClass()).carriesPositions()) {
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
                    this.rewriters.entries().get(payload.getClass());
            CustomPacketPayload rewritten =
                    (payloadRewriter == null ? this.recordFold : payloadRewriter).apply(payload, context);
            return rewritten == payload ? packet : this.wrap.apply(rewritten);
        }
    }

    private PacketTranslator() {
    }
}
