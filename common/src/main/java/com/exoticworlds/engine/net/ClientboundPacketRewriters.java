package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.fold.SeamDelta;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.ClientPosition.BorderCenter;
import com.exoticworlds.engine.seam.MirrorWriter;
import com.exoticworlds.mixin.BlockEntityDataPacketAccessor;
import com.exoticworlds.mixin.ChunkWaypointAccessor;
import com.exoticworlds.mixin.PlayerLookAtPacketAccessor;
import com.exoticworlds.mixin.SectionBlocksUpdatePacketAccessor;
import com.exoticworlds.mixin.TrackedWaypointAccessor;
import com.exoticworlds.mixin.Vec3iWaypointAccessor;
import com.google.common.base.Suppliers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.PositionAndRotation;
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
import net.minecraft.network.protocol.game.ClientboundAddTransientBlockPacket;
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
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
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
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.PositionStep;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;

final class ClientboundPacketRewriters {
    private static final String SOUND_KIND = "sound";

    private static final String PARTICLE_KIND = "particle";

    private static final String FORCED_PARTICLE_KIND = "forced_particle";

    private static final String EXPLOSION_KIND = "explosion";

    private static final Set<Class<?>> POSITIONLESS_VALUES = Set.of(Byte.class, Short.class, Integer.class,
            Long.class, Float.class, Double.class, Boolean.class, Character.class, String.class);

    private static final StreamCodec<FriendlyByteBuf, BorderCenter> BORDER_CENTER_CODEC = StreamCodec.of(
            (buffer, center) -> {
                buffer.writeDouble(center.x());
                buffer.writeDouble(center.z());
            },
            buffer -> new BorderCenter(buffer.readDouble(), buffer.readDouble()));

    private static final int DEFAULT_BUFFER_CAPACITY = 256;

    static ClientboundLevelChunkWithLightPacket levelChunk(ClientboundLevelChunkWithLightPacket packet, TranslationContext context) {
        ChunkPos serverPos = new ChunkPos(packet.x(), packet.z());
        ChunkPos clientPos = context.toClient(serverPos);
        FoldOrientation orientation = orientation(context, serverPos, clientPos);
        return new ClientboundLevelChunkWithLightPacket(
                clientPos.x(), clientPos.z(),
                TurnedChunkData.turned(packet.chunkData(), orientation, () -> buffer(context).registryAccess()),
                TurnedChunkData.turned(packet.lightData(), orientation));
    }

    static ClientboundLightUpdatePacket lightUpdate(ClientboundLightUpdatePacket packet, TranslationContext context) {
        ChunkPos serverPos = new ChunkPos(packet.x(), packet.z());
        ChunkPos clientPos = context.toClient(serverPos);
        return new ClientboundLightUpdatePacket(clientPos.x(), clientPos.z(),
                TurnedChunkData.turned(packet.lightData(), orientation(context, serverPos, clientPos)));
    }

    private static FoldOrientation orientation(TranslationContext context, ChunkPos serverPos, ChunkPos clientPos) {
        return context.fold().deckTransformation(serverPos, clientPos).getOrientation();
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

        if (!clientPosition.isForDimension(context.dimension())) {
            context.rebase().run();
            return packet;
        }

        PositionMoveRotation change = packet.change();
        Vec3 position = change.position();
        Set<Relative> relatives = packet.relatives();
        WorldFold fold = context.fold();

        Vec3 mirror = new Vec3(clientPosition.getX(), 0.0, clientPosition.getZ());
        FoldOrientation standing = clientPosition.orientation(fold);
        Vec3 clientDestination = clientPosition.destinationOf(fold, position, relatives);
        FoldOrientation arriving = relatives.contains(Relative.X) || relatives.contains(Relative.Z)
                ? standing
                : ClientPosition.orientationAt(fold, clientDestination);
        clientPosition.set(clientDestination.x, clientDestination.z, MirrorWriter.POSITION_PACKET);

        Vec3 sentPosition = sentPosition(fold, position, relatives, clientDestination, mirror, standing);
        FoldOrientation ofYaw = relatives.contains(Relative.Y_ROT) ? standing : arriving;
        FoldOrientation ofMovement = isRelativeMovement(relatives) ? standing : arriving;
        return new ClientboundPlayerPositionPacket(
                packet.id(),
                new PositionMoveRotation(
                        sentPosition,
                        ofMovement.applyToDelta(change.deltaMovement()),
                        relatives.contains(Relative.Y_ROT)
                                ? ofYaw.applyToYawChange(change.yRot())
                                : ofYaw.applyToYaw(change.yRot()),
                        change.xRot()),
                movementRelatives(relatives, ofMovement));
    }

    private static boolean isRelativeMovement(Set<Relative> relatives) {
        return relatives.contains(Relative.DELTA_X) || relatives.contains(Relative.DELTA_Z);
    }

    private static Set<Relative> movementRelatives(Set<Relative> relatives, FoldOrientation orientation) {
        boolean isXKept = relatives.contains(Relative.DELTA_X);
        boolean isZKept = relatives.contains(Relative.DELTA_Z);
        if (!orientation.isSwappingAxes() || isXKept == isZKept) {
            return relatives;
        }

        Set<Relative> swapped = EnumSet.copyOf(relatives);
        swapped.remove(isXKept ? Relative.DELTA_X : Relative.DELTA_Z);
        swapped.add(isXKept ? Relative.DELTA_Z : Relative.DELTA_X);
        return swapped;
    }

    static Packet<?> playerRotation(ClientboundPlayerRotationPacket packet, TranslationContext context) {
        ClientPosition clientPosition = context.clientPosition();
        if (!clientPosition.isForDimension(context.dimension())) {
            return packet;
        }

        FoldOrientation standing = clientPosition.orientation(context.fold());
        if (standing.isIdentity()) {
            return packet;
        }

        return new ClientboundPlayerRotationPacket(
                packet.relativeY() ? standing.applyToYawChange(packet.yRot()) : standing.applyToYaw(packet.yRot()),
                packet.relativeY(), packet.xRot(), packet.relativeX());
    }

    private static Vec3 sentPosition(WorldFold fold, Vec3 position, Set<Relative> relatives, Vec3 clientDestination,
            Vec3 mirror, FoldOrientation standing) {
        boolean isRelativeX = relatives.contains(Relative.X);
        boolean isRelativeZ = relatives.contains(Relative.Z);
        return isRelativeX && isRelativeZ
                ? standing.applyToDelta(SeamDelta.fold(fold, position))
                : new Vec3(
                        isRelativeX ? clientDestination.x - mirror.x : clientDestination.x,
                        position.y,
                        isRelativeZ ? clientDestination.z - mirror.z : clientDestination.z);
    }

    static ClientboundAddEntityPacket addEntity(ClientboundAddEntityPacket packet, TranslationContext context) {
        Class<?> turnedEntityClass = EntityPacketTurns.getTurnedEntityClass(context, packet.getId());
        EntitySeat seat = EntitySeat.of(context, EntityPacketTurns.spawnPosition(packet, turnedEntityClass));
        Vec3 clientPos = seat.position();
        FoldOrientation orientation = seat.orientation();
        return new ClientboundAddEntityPacket(
                packet.getId(), packet.getUUID(),
                clientPos.x, clientPos.y, clientPos.z,
                packet.getXRot(), orientation.applyToYaw(packet.getYRot()), packet.getType(),
                EntityPacketTurns.spawnData(packet.getData(), turnedEntityClass, orientation),
                orientation.applyToDelta(packet.getMovement()), orientation.applyToYaw(packet.getYHeadRot()));
    }

    static ClientboundTeleportEntityPacket teleportEntity(ClientboundTeleportEntityPacket packet,
            TranslationContext context) {
        PositionMoveRotation change = packet.change();
        Set<Relative> relatives = packet.relatives();
        boolean isRelativeX = relatives.contains(Relative.X);
        boolean isRelativeZ = relatives.contains(Relative.Z);
        PacketReach reach = context.getTrackedReach();
        Vec3 clientPos = isRelativeX || isRelativeZ
                ? context.toClientRelative(change.position(), isRelativeX, isRelativeZ, reach)
                : context.toClient(change.position(), reach);

        FoldOrientation standing = EntitySeat.orientationOf(context, packet.id());
        FoldOrientation arriving = isRelativeX || isRelativeZ ? standing : EntitySeat.orientationAt(context, clientPos);
        FoldOrientation ofMovement = isRelativeMovement(relatives) ? standing : arriving;
        return new ClientboundTeleportEntityPacket(
                packet.id(),
                new PositionMoveRotation(
                        isRelativeX && isRelativeZ ? standing.applyToDelta(clientPos) : clientPos,
                        ofMovement.applyToDelta(change.deltaMovement()),
                        relatives.contains(Relative.Y_ROT)
                                ? standing.applyToYawChange(change.yRot())
                                : arriving.applyToYaw(change.yRot()),
                        change.xRot()),
                movementRelatives(relatives, ofMovement), packet.onGround());
    }

    static ClientboundEntityPositionSyncPacket entityPositionSync(ClientboundEntityPositionSyncPacket packet,
            TranslationContext context) {
        PositionPath clientPath = toClientPath(context, packet.position());
        return new ClientboundEntityPositionSyncPacket(
                packet.id(), clientPath,
                EntitySeat.orientationAt(context, clientPath.endPosition()).applyToYaw(packet.yRot()), packet.xRot(),
                packet.onGround());
    }

    private static PositionPath toClientPath(TranslationContext context, PositionPath path) {
        PacketReach reach = context.getTrackedReach();
        return switch (path) {
            case PositionPath.Linear(Vec3 end) -> PositionPath.of(context.toClient(end, reach));
            case PositionPath.Stepped(Vec3 _, List<PositionStep> steps) -> PositionPath.stepped(steps.stream()
                    .map(step -> new PositionStep(context.toClient(step.position(), reach), step.tickOffset()))
                    .toList());
        };
    }

    static Packet<?> moveVehicle(ClientboundMoveVehiclePacket packet, TranslationContext context) {
        PositionAndRotation movingTo = packet.movingTo();
        Vec3 clientPos = context.toClient(movingTo.position(), context.getTrackedReach());
        float clientYaw = ClientPosition.orientationAt(context.fold(), clientPos).applyToYaw(movingTo.yRot());
        return new ClientboundMoveVehiclePacket(PositionAndRotation.of(clientPos, clientYaw, movingTo.xRot()));
    }

    static ClientboundBlockUpdatePacket blockUpdate(ClientboundBlockUpdatePacket packet, TranslationContext context) {
        BlockSeat seat = BlockSeat.of(context, packet.getPos());
        return new ClientboundBlockUpdatePacket(seat.pos(), seat.orientation().applyToState(packet.getBlockState()));
    }

    static ClientboundSectionBlocksUpdatePacket sectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, TranslationContext context) {
        ChunkPos serverPos = ((SectionBlocksUpdatePacketAccessor) packet).exotic_worlds$getSectionPos().chunk();
        ChunkPos clientPos = context.toClient(serverPos);
        ClientboundSectionBlocksUpdatePacket seated = rewritePosition(
                (sectionPacket, output) -> ((SectionBlocksUpdatePacketAccessor) sectionPacket).exotic_worlds$write(output),
                SectionBlocksUpdatePacketAccessor::exotic_worlds$create,
                SectionPos.STREAM_CODEC, packet, context,
                section -> SectionPos.of(clientPos, section.y()));
        SectionBlocksUpdatePacketAccessor changes = (SectionBlocksUpdatePacketAccessor) seated;
        TurnedChunkData.turnSectionChanges(changes.exotic_worlds$getPositions(), changes.exotic_worlds$getStates(),
                orientation(context, serverPos, clientPos));
        return seated;
    }

    static ClientboundBlockEntityDataPacket blockEntityData(ClientboundBlockEntityDataPacket packet, TranslationContext context) {
        return BlockEntityDataPacketAccessor.exotic_worlds$create(
                context.toClient(packet.getPos()), packet.getType(), packet.getTag());
    }

    static ClientboundAddTransientBlockPacket addTransientBlock(ClientboundAddTransientBlockPacket packet, TranslationContext context) {
        BlockSeat seat = BlockSeat.of(context, packet.getPos());
        return new ClientboundAddTransientBlockPacket(
                seat.pos(), seat.orientation().applyToState(packet.getBlockState()));
    }

    static ClientboundBlockDestructionPacket blockDestruction(ClientboundBlockDestructionPacket packet, TranslationContext context) {
        return new ClientboundBlockDestructionPacket(
                packet.getId(), context.toClient(packet.getPos()), packet.getProgress());
    }

    static ClientboundLevelEventPacket levelEvent(ClientboundLevelEventPacket packet, TranslationContext context) {
        BlockSeat seat = BlockSeat.of(context, packet.getPos());
        return new ClientboundLevelEventPacket(
                packet.getType(), seat.pos(),
                EventDirections.turnedLevelEventData(packet.getType(), packet.getData(), seat.orientation()),
                packet.isGlobalEvent());
    }

    static ClientboundSetEntityDataPacket setEntityData(ClientboundSetEntityDataPacket packet, TranslationContext context) {
        List<SynchedEntityData.DataValue<?>> items = packet.packedItems();
        Supplier<EntitySeat> seat = Suppliers.memoize(() -> EntitySeat.ofEntity(context, packet.id()));
        FoldedValue.Leaves leaves = FoldedValue.towardLeaves(context, () -> seat.get().position());
        UnaryOperator<Object> fallback = seatedValueFold(context, seat);
        List<SynchedEntityData.DataValue<?>> translated = new ArrayList<>(items.size());
        boolean isChanged = false;
        for (SynchedEntityData.DataValue<?> item : items) {
            SynchedEntityData.DataValue<?> clientItem = toClientData(item, leaves, fallback, context);
            isChanged |= clientItem != item;
            translated.add(clientItem);
        }

        return isChanged ? new ClientboundSetEntityDataPacket(packet.id(), translated) : packet;
    }

    private static SynchedEntityData.DataValue<?> toClientData(SynchedEntityData.DataValue<?> item,
            FoldedValue.Leaves leaves, UnaryOperator<Object> fallback, TranslationContext context) {
        Object value = item.value();
        if (value == null || POSITIONLESS_VALUES.contains(value.getClass())) {
            return item;
        }

        Object clientValue = FoldedValue.walk(context, leaves, value, fallback);
        return clientValue == value ? item : withValue(item, clientValue);
    }

    private static UnaryOperator<Object> seatedValueFold(TranslationContext context, Supplier<EntitySeat> seat) {
        return value -> switch (value) {
            case ParticleOptions particle -> toClientParticle(context, particle, seat.get().position());
            case Direction face -> seat.get().orientation().applyToFace(face);
            default -> value;
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SynchedEntityData.DataValue<?> withValue(SynchedEntityData.DataValue<?> item, Object value) {
        return new SynchedEntityData.DataValue(item.id(), item.serializer(), value);
    }

    static ClientboundBlockEventPacket blockEvent(ClientboundBlockEventPacket packet, TranslationContext context) {
        BlockSeat seat = BlockSeat.of(context, packet.getPos());
        return new ClientboundBlockEventPacket(
                seat.pos(), packet.getBlock(), packet.getB0(),
                EventDirections.turnedBlockEventData(
                        packet.getBlock(), packet.getB0(), packet.getB1(), seat.orientation()));
    }

    static ClientboundOpenSignEditorPacket openSignEditor(ClientboundOpenSignEditorPacket packet, TranslationContext context) {
        return new ClientboundOpenSignEditorPacket(context.toClient(packet.pos()), packet.slot());
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
                new Vec3(packet.x(), packet.y(), packet.z()),
                packet.overrideLimiter() ? FORCED_PARTICLE_KIND : PARTICLE_KIND);
        return new ClientboundLevelParticlesPacket(
                toClientParticle(context, packet.particle(), clientOrigin),
                packet.overrideLimiter(), packet.alwaysShow(),
                clientOrigin.x, clientOrigin.y, clientOrigin.z,
                packet.xDist(), packet.yDist(), packet.zDist(),
                packet.xMaxSpeed(), packet.yMaxSpeed(), packet.zMaxSpeed(),
                packet.count(), packet.randomizationType());
    }

    static ClientboundExplodePacket explode(ClientboundExplodePacket packet, TranslationContext context) {
        Vec3 clientCenter = context.toClientMeasured(packet.center(), EXPLOSION_KIND);
        return new ClientboundExplodePacket(
                clientCenter, packet.radius(), packet.blockCount(), toClientKnockback(context, packet.playerKnockback()),
                toClientParticle(context, packet.explosionParticle(), clientCenter),
                packet.explosionSound(),
                toClientBlockParticles(context, packet.blockParticles(), clientCenter),
                packet.playSound());
    }

    private static Optional<Vec3> toClientKnockback(TranslationContext context, Optional<Vec3> knockback) {
        ClientPosition clientPosition = context.clientPosition();
        if (knockback.isEmpty() || context.fold().isPreservingLocalIndices()
                || !clientPosition.isForDimension(context.dimension())) {
            return knockback;
        }

        return knockback.map(clientPosition.orientation(context.fold())::applyToDelta);
    }

    private static ParticleOptions toClientParticle(TranslationContext context, ParticleOptions particle,
            Vec3 clientOrigin) {
        return switch (particle) {
            case TrailParticleOption trail -> new TrailParticleOption(
                    context.fold().nearestCopy(clientOrigin, trail.target()),
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
                PacketTranslator.ParticleTranslation<ParticleOptions> particleRewriter =
                        PacketTranslator.getParticleRewriter(particle);
                yield particleRewriter == null ? particle : particleRewriter.rewrite(particle, context, clientOrigin);
            }
        };
    }

    private static WeightedList<ExplosionParticleInfo> toClientBlockParticles(TranslationContext context,
            WeightedList<ExplosionParticleInfo> blockParticles, Vec3 clientOrigin) {
        List<Weighted<ExplosionParticleInfo>> entries = blockParticles.unwrap();
        List<Weighted<ExplosionParticleInfo>> translated = new ArrayList<>(entries.size());
        boolean isChanged = false;
        for (Weighted<ExplosionParticleInfo> entry : entries) {
            ExplosionParticleInfo info = entry.value();
            ParticleOptions particle = toClientParticle(context, info.particle(), clientOrigin);
            isChanged |= particle != info.particle();
            translated.add(particle == info.particle() ? entry
                    : new Weighted<>(new ExplosionParticleInfo(particle, info.scaling(), info.speed()), entry.weight()));
        }

        return isChanged ? WeightedList.of(translated) : blockParticles;
    }

    static ClientboundTrackedWaypointPacket trackedWaypoint(ClientboundTrackedWaypointPacket packet, TranslationContext context) {
        if (packet.waypoint() instanceof Vec3iWaypointAccessor waypoint) {
            BlockPos serverPos = BlockPos.ZERO.offset(waypoint.exotic_worlds$getVector());
            BlockPos clientPos = context.nearestCopy(serverPos);
            return clientPos.equals(serverPos) ? packet : withWaypoint(packet, context,
                    copy -> ((Vec3iWaypointAccessor) copy).exotic_worlds$setVector(clientPos));
        }

        if (packet.waypoint() instanceof ChunkWaypointAccessor waypoint) {
            ChunkPos serverPos = waypoint.exotic_worlds$getChunkPos();
            ChunkPos clientPos = context.nearestCopy(serverPos);
            return clientPos.equals(serverPos) ? packet : withWaypoint(packet, context,
                    copy -> ((ChunkWaypointAccessor) copy).exotic_worlds$setChunkPos(clientPos));
        }

        return packet;
    }

    private static ClientboundTrackedWaypointPacket withWaypoint(ClientboundTrackedWaypointPacket packet,
            TranslationContext context, Consumer<TrackedWaypoint> seat) {
        RegistryFriendlyByteBuf buffer = buffer(context);
        packet.waypoint().write(buffer);
        TrackedWaypoint copy = TrackedWaypointAccessor.exotic_worlds$read(buffer);
        seat.accept(copy);
        return new ClientboundTrackedWaypointPacket(packet.operation(), copy);
    }

    static Packet<?> setDefaultSpawnPosition(ClientboundSetDefaultSpawnPositionPacket packet, TranslationContext context) {
        LevelData.RespawnData respawnData = packet.respawnData();
        if (!respawnData.dimension().equals(context.dimension())) {
            return packet;
        }

        BlockPos clientPos = context.nearestCopy(respawnData.pos());
        context.clientPosition().setHeldSpawn(clientPos);
        return new ClientboundSetDefaultSpawnPositionPacket(new LevelData.RespawnData(
                GlobalPos.of(respawnData.dimension(), clientPos),
                respawnData.yaw(), respawnData.pitch()));
    }

    static <T extends Packet<?>> Packet<?> borderCenter(T packet, TranslationContext context,
            BiConsumer<T, RegistryFriendlyByteBuf> writer, Function<FriendlyByteBuf, T> reader) {
        if (!context.clientPosition().isForDimension(context.dimension())) {
            return packet;
        }

        return rewritePosition(writer, reader, BORDER_CENTER_CODEC, packet, context,
                center -> toClientBorderCenter(context, center));
    }

    private static BorderCenter toClientBorderCenter(TranslationContext context, BorderCenter center) {
        ClientPosition clientPosition = context.clientPosition();
        BorderCenter clientCenter = nearestCopyCenter(context.fold(), clientPosition, center);
        clientPosition.setHeldBorderCenter(clientCenter);
        return clientCenter;
    }

    static BorderCenter nearestCopyCenter(WorldFold fold, ClientPosition clientPosition,
            BorderCenter center) {
        Vec3 nearest = fold.nearestCopy(clientPosition.anchor(0.0), new Vec3(center.x(), 0.0, center.z()));
        return new BorderCenter(nearest.x, nearest.z);
    }

    static ClientboundPlayerLookAtPacket playerLookAt(ClientboundPlayerLookAtPacket packet, TranslationContext context) {
        PlayerLookAtPacketAccessor accessor = (PlayerLookAtPacketAccessor) packet;
        Vec3 serverPos = new Vec3(accessor.exotic_worlds$getX(), 0.0, accessor.exotic_worlds$getZ());
        Vec3 near = context.nearestCopy(serverPos);
        if (near.equals(serverPos)) {
            return packet;
        }

        RegistryFriendlyByteBuf buffer = buffer(context);
        accessor.exotic_worlds$write(buffer);
        ClientboundPlayerLookAtPacket copy = PlayerLookAtPacketAccessor.exotic_worlds$create(buffer);
        PlayerLookAtPacketAccessor copyAccessor = (PlayerLookAtPacketAccessor) copy;
        copyAccessor.exotic_worlds$setX(near.x);
        copyAccessor.exotic_worlds$setZ(near.z);
        return copy;
    }

    static Packet<?> damageEvent(ClientboundDamageEventPacket packet, TranslationContext context) {
        if (packet.sourcePosition().isEmpty()) {
            return packet;
        }

        PacketReach reach = context.getTrackedReach();
        return new ClientboundDamageEventPacket(
                packet.entityId(), packet.sourceType(), packet.sourceCauseId(), packet.sourceDirectId(),
                packet.sourcePosition().map(position -> context.toClient(position, reach)));
    }

    static ClientboundMoveMinecartPacket moveMinecart(ClientboundMoveMinecartPacket packet, TranslationContext context) {
        PacketReach reach = context.getTrackedReach();
        List<NewMinecartBehavior.MinecartStep> translated = new ArrayList<>(packet.lerpSteps().size());
        for (NewMinecartBehavior.MinecartStep step : packet.lerpSteps()) {
            Vec3 clientPos = context.toClient(step.position(), reach);
            FoldOrientation orientation = EntitySeat.orientationAt(context, clientPos);
            translated.add(new NewMinecartBehavior.MinecartStep(
                    clientPos, orientation.applyToDelta(step.movement()), orientation.applyToYaw(step.yRot()),
                    step.xRot(), step.weight()));
        }

        return new ClientboundMoveMinecartPacket(packet.entityId(), translated);
    }

    static ClientboundChunksBiomesPacket chunkBiomes(ClientboundChunksBiomesPacket packet, TranslationContext context) {
        return new ClientboundChunksBiomesPacket(packet.chunkBiomeData().stream()
                .map(data -> chunkBiomeData(data, context))
                .toList());
    }

    private static ClientboundChunksBiomesPacket.ChunkBiomeData chunkBiomeData(
            ClientboundChunksBiomesPacket.ChunkBiomeData data, TranslationContext context) {
        ChunkPos clientPos = context.toClient(data.pos());
        return new ClientboundChunksBiomesPacket.ChunkBiomeData(clientPos, TurnedChunkData.turnedBiomes(
                data.buffer(), orientation(context, data.pos(), clientPos), () -> buffer(context).registryAccess()));
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
