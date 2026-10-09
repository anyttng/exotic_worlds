package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.exoticworlds.accessors.ChunkPacketPosition;
import com.exoticworlds.api.v1.net.ParticleRewriter;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.SeamDelta;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.ClientPosition.BorderCenter;
import com.exoticworlds.engine.seam.MirrorWriter;
import com.exoticworlds.mixin.BlockEntityDataPacketAccessor;
import com.exoticworlds.mixin.ChunkWaypointAccessor;
import com.exoticworlds.mixin.PlayerLookAtPacketAccessor;
import com.exoticworlds.mixin.SectionBlocksUpdatePacketAccessor;
import com.exoticworlds.mixin.Vec3iWaypointAccessor;
import com.google.common.base.Suppliers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
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
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

final class ClientboundPacketRewriters {
    private static final String SOUND_KIND = "sound";

    private static final String PARTICLE_KIND = "particle";

    private static final String FORCED_PARTICLE_KIND = "forced_particle";

    private static final String EXPLOSION_KIND = "explosion";

    private static final StreamCodec<FriendlyByteBuf, BorderCenter> BORDER_CENTER_CODEC = StreamCodec.of(
            (buffer, center) -> {
                buffer.writeDouble(center.x());
                buffer.writeDouble(center.z());
            },
            buffer -> new BorderCenter(buffer.readDouble(), buffer.readDouble()));

    private static final int DEFAULT_BUFFER_CAPACITY = 256;

    static Packet<?> chunkPosition(Packet<?> packet, int x, int z, TranslationContext context) {
        ChunkPos clientPos = context.toClient(new ChunkPos(x, z));

        ChunkPacketPosition accessor = (ChunkPacketPosition) packet;
        accessor.toroidal$setX(clientPos.x());
        accessor.toroidal$setZ(clientPos.z());
        return packet;
    }

    static Packet<?> forgetChunk(ClientboundForgetLevelChunkPacket packet, TranslationContext context) {
        List<ChunkPos> candidates = context.forgetCandidates(packet.pos());
        if (candidates.size() == 1) {
            return new ClientboundForgetLevelChunkPacket(candidates.getFirst());
        }

        List<Packet<? super ClientGamePacketListener>> forgets = new ArrayList<>(candidates.size());
        for (ChunkPos candidate : candidates) {
            forgets.add(new ClientboundForgetLevelChunkPacket(candidate));
        }

        return new ClientboundBundlePacket(forgets);
    }

    static ClientboundSetChunkCacheCenterPacket chunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet, TranslationContext context) {
        ChunkPos clientPos = context.toClientCacheCenter(new ChunkPos(packet.getX(), packet.getZ()));
        return new ClientboundSetChunkCacheCenterPacket(clientPos.x(), clientPos.z());
    }

    static ClientboundPlayerPositionPacket playerPosition(ClientboundPlayerPositionPacket packet, TranslationContext context) {
        ClientPosition clientPosition = context.clientPosition();

        if (!clientPosition.describes(context.dimension())) {
            context.rebase().run();
            return packet;
        }

        PositionMoveRotation change = packet.change();
        Vec3 position = change.position();

        boolean relativeX = packet.relatives().contains(Relative.X);
        boolean relativeZ = packet.relatives().contains(Relative.Z);
        double mirrorX = clientPosition.x();
        double mirrorZ = clientPosition.z();
        Vec3 clientDestination = clientPosition.destinationOf(context.transformer(), position, packet.relatives());
        clientPosition.set(clientDestination.x, clientDestination.z, MirrorWriter.POSITION_PACKET);

        Vec3 sentPosition = relativeX && relativeZ
                ? SeamDelta.fold(context.transformer(), position)
                : new Vec3(
                        relativeX ? clientDestination.x - mirrorX : clientDestination.x,
                        position.y,
                        relativeZ ? clientDestination.z - mirrorZ : clientDestination.z);
        return new ClientboundPlayerPositionPacket(
                packet.id(),
                new PositionMoveRotation(sentPosition, change.deltaMovement(), change.yRot(), change.xRot()),
                packet.relatives());
    }

    static ClientboundAddEntityPacket addEntity(ClientboundAddEntityPacket packet, TranslationContext context) {
        Vec3 clientPos = context.toClient(
                new Vec3(packet.getX(), packet.getY(), packet.getZ()), context.trackedReach());
        return new ClientboundAddEntityPacket(
                packet.getId(), packet.getUUID(),
                clientPos.x, clientPos.y, clientPos.z,
                packet.getXRot(), packet.getYRot(), packet.getType(), packet.getData(),
                packet.getMovement(), packet.getYHeadRot());
    }

    static Packet<?> teleportEntity(ClientboundTeleportEntityPacket packet, TranslationContext context) {
        if (context.ownVehicle().test(packet.id())) {
            return null;
        }

        return new ClientboundTeleportEntityPacket(
                packet.id(), toClientChange(context, packet.change(), packet.relatives()), packet.relatives(),
                packet.onGround());
    }

    static Packet<?> entityPositionSync(ClientboundEntityPositionSyncPacket packet, TranslationContext context) {
        if (context.ownVehicle().test(packet.id())) {
            return null;
        }

        return new ClientboundEntityPositionSyncPacket(
                packet.id(), toClientChange(context, packet.values(), Set.of()), packet.onGround());
    }

    static Packet<?> moveVehicle(ClientboundMoveVehiclePacket packet, TranslationContext context) {
        Vec3 clientPos = context.toClient(packet.position(), context.trackedReach());
        return new ClientboundMoveVehiclePacket(clientPos, packet.yRot(), packet.xRot());
    }

    private static PositionMoveRotation toClientChange(TranslationContext context, PositionMoveRotation change, Set<Relative> relatives) {
        PacketReach reach = context.trackedReach();
        Vec3 position = change.position();
        Vec3 clientPos = relatives.contains(Relative.X) || relatives.contains(Relative.Z)
                ? context.toClientRelative(position, relatives.contains(Relative.X), relatives.contains(Relative.Z), reach)
                : context.toClient(position, reach);
        return new PositionMoveRotation(clientPos, change.deltaMovement(), change.yRot(), change.xRot());
    }

    static ClientboundBlockUpdatePacket blockUpdate(ClientboundBlockUpdatePacket packet, TranslationContext context) {
        return new ClientboundBlockUpdatePacket(context.toClient(packet.getPos()), packet.getBlockState());
    }

    static ClientboundSectionBlocksUpdatePacket sectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, TranslationContext context) {
        return rewritePosition(
                (sectionPacket, output) -> ((SectionBlocksUpdatePacketAccessor) sectionPacket).toroidal$write(output),
                SectionBlocksUpdatePacketAccessor::toroidal$create,
                SectionPos.STREAM_CODEC, packet, context,
                section -> SectionPos.of(context.toClient(section.chunk()), section.y()));
    }

    static ClientboundBlockEntityDataPacket blockEntityData(ClientboundBlockEntityDataPacket packet, TranslationContext context) {
        return BlockEntityDataPacketAccessor.toroidal$create(
                context.toClient(packet.getPos()), packet.getType(), packet.getTag());
    }

    static ClientboundBlockDestructionPacket blockDestruction(ClientboundBlockDestructionPacket packet, TranslationContext context) {
        return new ClientboundBlockDestructionPacket(
                packet.getId(), context.toClient(packet.getPos()), packet.getProgress());
    }

    static ClientboundLevelEventPacket levelEvent(ClientboundLevelEventPacket packet, TranslationContext context) {
        return new ClientboundLevelEventPacket(
                packet.getType(), context.toClient(packet.getPos()), packet.getData(), packet.isGlobalEvent());
    }

    static ClientboundSetEntityDataPacket setEntityData(ClientboundSetEntityDataPacket packet, TranslationContext context) {
        List<SynchedEntityData.DataValue<?>> items = packet.packedItems();
        Supplier<Vec3> anchor = Suppliers.memoize(() -> resolveEntityAnchor(packet.id(), context));
        List<SynchedEntityData.DataValue<?>> translated = new ArrayList<>(items.size());
        boolean changed = false;
        for (SynchedEntityData.DataValue<?> item : items) {
            SynchedEntityData.DataValue<?> clientItem = toClientData(item, anchor, context);
            changed |= clientItem != item;
            translated.add(clientItem);
        }

        return changed ? new ClientboundSetEntityDataPacket(packet.id(), translated) : packet;
    }

    private static Vec3 resolveEntityAnchor(int entityId, TranslationContext context) {
        Vec3 serverPosition = context.entityPosition().apply(entityId);
        if (serverPosition == null) {
            ClientPosition mirror = context.clientPosition();
            return new Vec3(mirror.x(), 0.0, mirror.z());
        }

        return context.toClient(serverPosition, context.trackedReach());
    }

    private static SynchedEntityData.DataValue<?> toClientData(SynchedEntityData.DataValue<?> item,
            Supplier<Vec3> anchor, TranslationContext context) {
        Object value = item.value();
        Object clientValue = FoldedValue.toward(context, anchor, value, particleFold(context, anchor));
        return clientValue == value ? item : withValue(item, clientValue);
    }

    private static UnaryOperator<Object> particleFold(TranslationContext context, Supplier<Vec3> anchor) {
        return value -> value instanceof ParticleOptions particle
                ? toClientParticle(context, particle, anchor.get())
                : value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SynchedEntityData.DataValue<?> withValue(SynchedEntityData.DataValue<?> item, Object value) {
        return new SynchedEntityData.DataValue(item.id(), item.serializer(), value);
    }

    static ClientboundBlockEventPacket blockEvent(ClientboundBlockEventPacket packet, TranslationContext context) {
        return new ClientboundBlockEventPacket(
                context.toClient(packet.getPos()), packet.getBlock(), packet.getB0(), packet.getB1());
    }

    static ClientboundOpenSignEditorPacket openSignEditor(ClientboundOpenSignEditorPacket packet, TranslationContext context) {
        return new ClientboundOpenSignEditorPacket(context.toClient(packet.getPos()), packet.isFrontText());
    }

    static ClientboundSoundPacket sound(ClientboundSoundPacket packet, TranslationContext context) {
        Vec3 clientPos = context.toClientMeasured(
                new Vec3(packet.getX(), packet.getY(), packet.getZ()), SOUND_KIND);
        return new ClientboundSoundPacket(
                packet.getSound(), packet.getSource(),
                clientPos.x, clientPos.y, clientPos.z,
                packet.getVolume(), packet.getPitch(), packet.getSeed());
    }

    static ClientboundLevelParticlesPacket levelParticles(ClientboundLevelParticlesPacket packet, TranslationContext context) {
        Vec3 clientOrigin = context.toClientMeasured(
                new Vec3(packet.getX(), packet.getY(), packet.getZ()),
                packet.isOverrideLimiter() ? FORCED_PARTICLE_KIND : PARTICLE_KIND);
        return new ClientboundLevelParticlesPacket(
                toClientParticle(context, packet.getParticle(), clientOrigin),
                packet.isOverrideLimiter(), packet.alwaysShow(),
                clientOrigin.x, clientOrigin.y, clientOrigin.z,
                packet.getXDist(), packet.getYDist(), packet.getZDist(), packet.getMaxSpeed(), packet.getCount());
    }

    static ClientboundExplodePacket explode(ClientboundExplodePacket packet, TranslationContext context) {
        Vec3 clientCenter = context.toClientMeasured(packet.center(), EXPLOSION_KIND);
        return new ClientboundExplodePacket(
                clientCenter, packet.radius(), packet.blockCount(), packet.playerKnockback(),
                toClientParticle(context, packet.explosionParticle(), clientCenter),
                packet.explosionSound(),
                toClientBlockParticles(context, packet.blockParticles(), clientCenter));
    }

    private static ParticleOptions toClientParticle(TranslationContext context, ParticleOptions particle,
            Vec3 clientOrigin) {
        return switch (particle) {
            case TrailParticleOption trail -> new TrailParticleOption(
                    context.transformer().nearestCopy(clientOrigin, trail.target()),
                    trail.color(), trail.duration());
            case VibrationParticleOption vibration -> {
                if (!(vibration.getDestination() instanceof BlockPositionSource destination)) {
                    yield particle;
                }

                BlockPos clientDestination = FoldedValue.nearestCopy(context, clientOrigin, destination.pos());
                yield new VibrationParticleOption(
                        new BlockPositionSource(clientDestination), vibration.getArrivalInTicks());
            }
            default -> {
                ParticleRewriter<ParticleOptions> particleRewriter = PacketTranslator.particleRewriterFor(particle);
                yield particleRewriter == null ? particle : particleRewriter.rewrite(particle, context, clientOrigin);
            }
        };
    }

    private static WeightedList<ExplosionParticleInfo> toClientBlockParticles(TranslationContext context,
            WeightedList<ExplosionParticleInfo> blockParticles, Vec3 clientOrigin) {
        List<Weighted<ExplosionParticleInfo>> entries = blockParticles.unwrap();
        List<Weighted<ExplosionParticleInfo>> translated = new ArrayList<>(entries.size());
        boolean changed = false;
        for (Weighted<ExplosionParticleInfo> entry : entries) {
            ExplosionParticleInfo info = entry.value();
            ParticleOptions particle = toClientParticle(context, info.particle(), clientOrigin);
            changed |= particle != info.particle();
            translated.add(particle == info.particle() ? entry
                    : new Weighted<>(new ExplosionParticleInfo(particle, info.scaling(), info.speed()), entry.weight()));
        }

        return changed ? WeightedList.of(translated) : blockParticles;
    }

    static ClientboundTrackedWaypointPacket trackedWaypoint(ClientboundTrackedWaypointPacket packet, TranslationContext context) {
        if (packet.waypoint() instanceof Vec3iWaypointAccessor waypoint) {
            waypoint.toroidal$setVector(nearestCopyBlock(context, new BlockPos(waypoint.toroidal$getVector())));
        } else if (packet.waypoint() instanceof ChunkWaypointAccessor waypoint) {
            waypoint.toroidal$setChunkPos(context.nearestCopy(waypoint.toroidal$getChunkPos()));
        }

        return packet;
    }

    static Packet<?> setDefaultSpawnPosition(ClientboundSetDefaultSpawnPositionPacket packet, TranslationContext context) {
        LevelData.RespawnData respawnData = packet.respawnData();
        if (!respawnData.dimension().equals(context.dimension())) {
            return packet;
        }

        BlockPos clientPos = nearestCopyBlock(context, respawnData.pos());
        context.clientPosition().setHeldSpawn(clientPos);
        return new ClientboundSetDefaultSpawnPositionPacket(new LevelData.RespawnData(
                GlobalPos.of(respawnData.dimension(), clientPos),
                respawnData.yaw(), respawnData.pitch()));
    }

    static <T extends Packet<?>> Packet<?> borderCenter(T packet, TranslationContext context,
            BiConsumer<T, RegistryFriendlyByteBuf> writer, Function<FriendlyByteBuf, T> reader) {
        if (!context.clientPosition().describes(context.dimension())) {
            return packet;
        }

        return rewritePosition(writer, reader, BORDER_CENTER_CODEC, packet, context,
                center -> toClientBorderCenter(context, center));
    }

    private static BorderCenter toClientBorderCenter(TranslationContext context, BorderCenter center) {
        ClientPosition clientPosition = context.clientPosition();
        BorderCenter clientCenter = nearestCopyCenter(context.transformer(), clientPosition, center);
        clientPosition.setHeldBorderCenter(clientCenter);
        return clientCenter;
    }

    static BorderCenter nearestCopyCenter(WorldFold transformer, ClientPosition clientPosition,
            BorderCenter center) {
        Vec3 nearest = transformer.nearestCopy(
                new Vec3(clientPosition.x(), 0.0, clientPosition.z()), new Vec3(center.x(), 0.0, center.z()));
        return new BorderCenter(nearest.x, nearest.z);
    }

    static ClientboundPlayerLookAtPacket playerLookAt(ClientboundPlayerLookAtPacket packet, TranslationContext context) {
        PlayerLookAtPacketAccessor accessor = (PlayerLookAtPacketAccessor) packet;
        Vec3 near = context.nearestCopy(new Vec3(accessor.toroidal$getX(), 0.0, accessor.toroidal$getZ()));
        accessor.toroidal$setX(near.x);
        accessor.toroidal$setZ(near.z);
        return packet;
    }

    static Packet<?> damageEvent(ClientboundDamageEventPacket packet, TranslationContext context) {
        if (packet.sourcePosition().isEmpty()) {
            return packet;
        }

        PacketReach reach = context.trackedReach();
        return new ClientboundDamageEventPacket(
                packet.entityId(), packet.sourceType(), packet.sourceCauseId(), packet.sourceDirectId(),
                packet.sourcePosition().map(position -> context.toClient(position, reach)));
    }

    static ClientboundMoveMinecartPacket moveMinecart(ClientboundMoveMinecartPacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        List<NewMinecartBehavior.MinecartStep> translated = new ArrayList<>(packet.lerpSteps().size());
        for (NewMinecartBehavior.MinecartStep step : packet.lerpSteps()) {
            translated.add(new NewMinecartBehavior.MinecartStep(
                    context.toClient(step.position(), reach), step.movement(), step.yRot(), step.xRot(), step.weight()));
        }

        return new ClientboundMoveMinecartPacket(packet.entityId(), translated);
    }

    static ClientboundChunksBiomesPacket chunkBiomes(ClientboundChunksBiomesPacket packet, TranslationContext context) {
        return new ClientboundChunksBiomesPacket(packet.chunkBiomeData().stream()
                .map(data -> new ClientboundChunksBiomesPacket.ChunkBiomeData(
                        context.toClient(data.pos()), data.buffer()))
                .toList());
    }

    private static BlockPos nearestCopyBlock(TranslationContext context, BlockPos pos) {
        return nearestCopyBlock(context.transformer(), context.clientPosition().chunk(), pos);
    }

    static BlockPos nearestCopyBlock(WorldFold transformer, ChunkPos anchor, BlockPos pos) {
        return transformer.reseat(pos, transformer.nearestCopy(anchor, ChunkPos.containing(pos)));
    }

    private static <T, P> T rewritePosition(BiConsumer<T, RegistryFriendlyByteBuf> writer,
            Function<FriendlyByteBuf, T> reader,
            StreamCodec<? super RegistryFriendlyByteBuf, P> positionCodec, T packet, TranslationContext context,
            UnaryOperator<P> toClient) {
        RegistryFriendlyByteBuf source = buffer(context);
        writer.accept(packet, source);
        P serverPosition = positionCodec.decode(source);

        RegistryFriendlyByteBuf target = buffer(context, source.readerIndex() + source.readableBytes());
        positionCodec.encode(target, toClient.apply(serverPosition));
        target.writeBytes(source);
        return reader.apply(target);
    }

    private static RegistryFriendlyByteBuf buffer(TranslationContext context) {
        return context.bufferFactory().apply(DEFAULT_BUFFER_CAPACITY);
    }

    private static RegistryFriendlyByteBuf buffer(TranslationContext context, int capacity) {
        return context.bufferFactory().apply(capacity);
    }

    private ClientboundPacketRewriters() {
    }
}
