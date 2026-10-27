package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.mixin.InitializeBorderPacketAccessor;
import com.exoticworlds.mixin.SetBorderCenterPacketAccessor;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
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
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;

public final class PacketTranslator {
    private static final StreamCodec<FriendlyByteBuf, Unit> NO_PREFIX_CODEC = StreamCodec.unit(Unit.INSTANCE);

    private static final PayloadRoute<ClientboundCustomPayloadPacket> CLIENTBOUND_PAYLOADS = new PayloadRoute<>(
            PacketRewriters::clientboundPayloadFor, RecordPayloadFold::toClient, ClientboundCustomPayloadPacket::new);

    private static final PayloadRoute<ServerboundCustomPayloadPacket> SERVERBOUND_PAYLOADS = new PayloadRoute<>(
            PacketRewriters::serverboundPayloadFor, RecordPayloadFold::toServer, ServerboundCustomPayloadPacket::new);

    private static final Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> TO_CLIENT = Map.ofEntries(
            Map.entry(ClientboundLevelChunkWithLightPacket.class, perCopy(
                    (ClientboundLevelChunkWithLightPacket packet, TranslationContext context) ->
                            ClientboundPacketRewriters.chunkPosition(packet, packet.getX(), packet.getZ(),
                                    ChunkTraffic.CHUNK_DATA, context))),
            Map.entry(ClientboundLightUpdatePacket.class, perCopy(
                    (ClientboundLightUpdatePacket packet, TranslationContext context) ->
                            ClientboundPacketRewriters.chunkPosition(packet, packet.getX(), packet.getZ(),
                                    ChunkTraffic.LIGHT_UPDATE, context))),
            Map.entry(ClientboundForgetLevelChunkPacket.class, rewriter(ClientboundPacketRewriters::forgetChunk)),
            Map.entry(ClientboundSetChunkCacheCenterPacket.class, rewriter(ClientboundPacketRewriters::chunkCacheCenter)),
            Map.entry(ClientboundChunksBiomesPacket.class, perCopy(ClientboundPacketRewriters::chunkBiomes)),
            Map.entry(ClientboundPlayerPositionPacket.class, rewriter(ClientboundPacketRewriters::playerPosition)),
            Map.entry(ClientboundBlockUpdatePacket.class, perCopy(ClientboundPacketRewriters::blockUpdate)),
            Map.entry(ClientboundSectionBlocksUpdatePacket.class, perCopy(ClientboundPacketRewriters::sectionBlocksUpdate)),
            Map.entry(ClientboundBlockEntityDataPacket.class, perCopy(ClientboundPacketRewriters::blockEntityData)),
            Map.entry(ClientboundBlockDestructionPacket.class, rewriter(ClientboundPacketRewriters::blockDestruction)),
            Map.entry(ClientboundSetEntityDataPacket.class, rewriter(ClientboundPacketRewriters::setEntityData)),
            Map.entry(ClientboundAddEntityPacket.class, rewriter(ClientboundPacketRewriters::addEntity)),
            Map.entry(ClientboundTeleportEntityPacket.class, rewriter(ClientboundPacketRewriters::teleportEntity)),
            Map.entry(ClientboundMoveVehiclePacket.class, rewriter(ClientboundPacketRewriters::moveVehicle)),
            Map.entry(ClientboundBlockEventPacket.class, perCopy(ClientboundPacketRewriters::blockEvent)),
            Map.entry(ClientboundOpenSignEditorPacket.class, rewriter(ClientboundPacketRewriters::openSignEditor)),
            Map.entry(ClientboundLevelEventPacket.class, rewriter(ClientboundPacketRewriters::levelEvent)),
            Map.entry(ClientboundSoundPacket.class, rewriter(ClientboundPacketRewriters::sound)),
            Map.entry(ClientboundLevelParticlesPacket.class, rewriter(ClientboundPacketRewriters::levelParticles)),
            Map.entry(ClientboundExplodePacket.class, rewriter(ClientboundPacketRewriters::explode)),
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
            Map.entry(ClientboundCustomPayloadPacket.class, rewriter(PacketTranslator::clientboundPayload)));

    private static final PacketRewriters PRODUCTION = new PacketRewriters();

    static PacketRewriters production() {
        return PRODUCTION;
    }

    public static <P extends CustomPacketPayload> void registerClientboundPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        PRODUCTION.registerClientboundPayload(payloadType, payloadRewriter);
    }

    public static <P extends CustomPacketPayload> void registerClientboundChunkPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        PRODUCTION.registerClientboundChunkPayload(payloadType, payloadRewriter);
    }

    private static Packet<?> clientboundPayload(ClientboundCustomPayloadPacket packet, TranslationContext context) {
        BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> chunkRewriter =
                context.rewriters().clientboundChunkPayloadFor(packet.payload());
        return chunkRewriter == null
                ? CLIENTBOUND_PAYLOADS.rewrite(packet, packet.payload(), context)
                : perCopy((sent, seated) -> CLIENTBOUND_PAYLOADS.rewrite(packet, packet.payload(), seated, chunkRewriter),
                        packet, context);
    }

    public static <P extends CustomPacketPayload> void registerServerboundPayloadRewriter(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        PRODUCTION.registerServerboundPayload(payloadType, payloadRewriter);
    }

    public static <P extends ParticleOptions> void registerParticleRewriter(Class<P> particleType,
            PacketRewriters.ParticleRewriter<P> particleRewriter) {
        PRODUCTION.registerParticle(particleType, particleRewriter);
    }

    public static <T> void registerEntityDataRewriter(EntityDataSerializer<T> serializer,
            PacketRewriters.EntityDataRewriter<T> dataRewriter) {
        PRODUCTION.registerEntityData(serializer, dataRewriter);
    }

    public static <T> void registerEntityDataRewriter(Class<? extends Entity> owner,
            Supplier<EntityDataAccessor<T>> accessor, PacketRewriters.EntityDataRewriter<T> dataRewriter) {
        PRODUCTION.registerEntityData(owner, accessor, dataRewriter);
    }

    private static final Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> TO_SERVER = Map.ofEntries(
            Map.entry(ServerboundUseItemOnPacket.class, rewriter(ServerboundPacketRewriters::useItemOn)),
            Map.entry(ServerboundPlayerActionPacket.class, rewriter(ServerboundPacketRewriters::playerAction)),
            Map.entry(ServerboundSignUpdatePacket.class, rewriter(ServerboundPacketRewriters::signUpdate)),
            Map.entry(ServerboundBlockEntityTagQueryPacket.class, rewriter(ServerboundPacketRewriters::blockEntityTagQuery)),
            Map.entry(ServerboundJigsawGeneratePacket.class, rewriter(ServerboundPacketRewriters::jigsawGenerate)),
            Map.entry(ServerboundSetCommandBlockPacket.class, rewriter(ServerboundPacketRewriters::setCommandBlock)),
            Map.entry(ServerboundSetJigsawBlockPacket.class, rewriter(ServerboundPacketRewriters::setJigsawBlock)),
            Map.entry(ServerboundSetStructureBlockPacket.class, rewriter(ServerboundPacketRewriters::setStructureBlock)),
            Map.entry(ServerboundSetCreativeModeSlotPacket.class, rewriter(ServerboundPacketRewriters::setCreativeModeSlot)),
            Map.entry(ServerboundCustomPayloadPacket.class, rewriter(
                    (ServerboundCustomPayloadPacket packet, TranslationContext context) ->
                            SERVERBOUND_PAYLOADS.rewrite(packet, packet.payload(), context))));

    private static final PacketRoute CLIENTBOUND = new PacketRoute(TO_CLIENT,
            PacketRewriters::clientboundPacketFor, RecordPayloadFold::toClient);

    private static final PacketRoute SERVERBOUND = new PacketRoute(TO_SERVER,
            PacketRewriters::serverboundPacketFor, RecordPayloadFold::toServer);

    public static <P extends Packet<?>> void registerClientboundPacketRewriter(Class<P> packetType,
            BiFunction<P, TranslationContext, Packet<?>> packetRewriter) {
        PRODUCTION.registerClientboundPacket(packetType, packetRewriter);
    }

    public static <P extends Packet<?>> void registerServerboundPacketRewriter(Class<P> packetType,
            BiFunction<P, TranslationContext, Packet<?>> packetRewriter) {
        PRODUCTION.registerServerboundPacket(packetType, packetRewriter);
    }

    public static <T extends net.minecraft.network.PacketListener> @Nullable Packet<T> toClient(Packet<T> packet,
            ServerPlayer player) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(player.level());
        if (fold == null) {
            return packet;
        }

        if (packet instanceof ClientboundBundlePacket bundle) {
            return castPacket(toClientBundle(bundle, PRODUCTION, () -> TranslationContext.of(player, fold)));
        }

        return dispatch(CLIENTBOUND, packet, player, fold);
    }

    static <T extends net.minecraft.network.PacketListener> @Nullable Packet<T> toClient(Packet<T> packet,
            TranslationContext context) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            return castPacket(toClientBundle(bundle, context.rewriters(), () -> context));
        }

        return castPacket(rewrite(CLIENTBOUND, packet, context));
    }

    @SuppressWarnings("unchecked")
    private static Packet<?> toClientBundle(ClientboundBundlePacket bundle, PacketRewriters rewriters,
            Supplier<TranslationContext> contextOf) {
        TranslationContext context = null;
        List<Packet<? super ClientGamePacketListener>> translated = new ArrayList<>();
        boolean changed = false;
        for (Packet<? super ClientGamePacketListener> sub : bundle.subPackets()) {
            BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = CLIENTBOUND.rewriterOf(sub, rewriters);
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
            if (translatedSub instanceof ClientboundBundlePacket copies) {
                copies.subPackets().forEach(translated::add);
            } else if (translatedSub != null) {
                translated.add(translatedSub);
            }
        }

        return changed ? new ClientboundBundlePacket(translated) : bundle;
    }

    public static <T extends net.minecraft.network.PacketListener> Packet<T> toServer(Packet<T> packet, ServerPlayer player) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(player.level());
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
        BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = route.rewriterOf(packet, PRODUCTION);
        return rewriter == null ? packet
                : castPacket(rewriter.apply(packet, TranslationContext.of(player, fold)));
    }

    private static Packet<?> rewrite(PacketRoute route, Packet<?> packet, TranslationContext context) {
        BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = route.rewriterOf(packet, context.rewriters());
        return rewriter == null ? packet : rewriter.apply(packet, context);
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.network.PacketListener> Packet<T> castPacket(Packet<?> packet) {
        return (Packet<T>) packet;
    }

    static <T, P> T rewritePosition(BiConsumer<T, RegistryFriendlyByteBuf> writer,
            Function<FriendlyByteBuf, T> reader,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            UnaryOperator<P> toClient) {
        return rewritePosition(writer, reader, NO_PREFIX_CODEC, positionCodec, packet, context,
                (noPrefix, serverPosition) -> toClient.apply(serverPosition));
    }

    // The same swap where the position is not first on the wire — an entity id stands in front of it, or an id and an
    // action. The prefix is decoded so its width is known and then copied across byte for byte: re-encoding it would
    // have to reproduce it exactly, and a copy owes nothing to that. Its decoded value is handed to the mapper because
    // for a serverbound interact those leading bytes are the only place the entity id lives — the packet exposes no
    // getter for it, and the hit location has to fold around the entity it names.
    //
    // Every position codec that comes through here re-encodes to the width the server one vacated — a packed long,
    // three doubles, three floats — so the target holds exactly the source packet and the buffer never grows.
    //
    // The wire pair is vanilla's own write/read behind the packet's STREAM_CODEC, reached through invokers rather
    // than the codec field: loaders and mods swap wrappers into those fields whose transforms assume the network
    // pipeline around them — Fabric's PacketContext is a scoped value bound only inside the encoder, and this runs
    // on the server thread — and the pipeline still encodes the finished packet, so a wrapper run here would also
    // run twice.
    static <T, R, P> T rewritePosition(BiConsumer<T, RegistryFriendlyByteBuf> writer,
            Function<FriendlyByteBuf, T> reader,
            StreamCodec<? super RegistryFriendlyByteBuf, R> prefixCodec,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            BiFunction<R, P, P> toClient) {
        RegistryFriendlyByteBuf source = buffer(context);
        writer.accept(packet, source);
        R prefix = prefixCodec.decode(source);
        int prefixLength = source.readerIndex();
        P serverPosition = positionCodec.decode(source);

        RegistryFriendlyByteBuf target = buffer(context, source.readerIndex() + source.readableBytes());
        target.writeBytes(source, 0, prefixLength);
        positionCodec.encode(target, toClient.apply(prefix, serverPosition));
        target.writeBytes(source);
        return reader.apply(target);
    }

    private static final int DEFAULT_BUFFER_CAPACITY = 256;

    private static RegistryFriendlyByteBuf buffer(TranslationContext context) {
        return context.bufferFactory().apply(DEFAULT_BUFFER_CAPACITY);
    }

    private static RegistryFriendlyByteBuf buffer(TranslationContext context, int capacity) {
        return context.bufferFactory().apply(capacity);
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
        CameraSide cameraSide = context.cameraSide();
        cameraSide.reset();
        Packet<?> held = once.apply(packet, context);
        if (!cameraSide.secondCopy()) {
            return held;
        }

        Packet<?> camera;
        cameraSide.answerCamera();
        try {
            camera = once.apply(packet, context);
        } finally {
            cameraSide.reset();
        }

        return new ClientboundBundlePacket(List.of(
                (Packet<? super ClientGamePacketListener>) held, (Packet<? super ClientGamePacketListener>) camera));
    }

    private record PacketRoute(Map<Class<?>, BiFunction<Packet<?>, TranslationContext, Packet<?>>> table,
            BiFunction<PacketRewriters, Packet<?>,
                    @Nullable BiFunction<Packet<?>, TranslationContext, Packet<?>>> rewriterFor,
            BiFunction<Packet<?>, TranslationContext, Packet<?>> recordFold) {
        @Nullable BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriterOf(Packet<?> packet,
                PacketRewriters rewriters) {
            BiFunction<Packet<?>, TranslationContext, Packet<?>> rewriter = this.table.get(packet.getClass());
            if (rewriter == null) {
                rewriter = this.rewriterFor.apply(rewriters, packet);
            }

            if (rewriter == null && isModPacket(packet)
                    && RecordPayloadFold.formOf(packet.getClass()).carriesPositions()) {
                rewriter = this.recordFold;
            }

            return rewriter;
        }

        private static boolean isModPacket(Packet<?> packet) {
            return !packet.type().id().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE);
        }
    }

    private record PayloadRoute<P extends Packet<?>>(
            BiFunction<PacketRewriters, CustomPacketPayload,
                    @Nullable BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>> rewriterFor,
            BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> recordFold,
            Function<CustomPacketPayload, P> wrap) {
        Packet<?> rewrite(P packet, CustomPacketPayload payload, TranslationContext context) {
            BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> payloadRewriter =
                    this.rewriterFor.apply(context.rewriters(), payload);
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
