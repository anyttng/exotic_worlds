package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.exoticworlds.accessors.ChunkPacketPosition;
import com.exoticworlds.engine.fold.SeamDelta;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.mixin.BlockEntityDataPacketAccessor;
import com.exoticworlds.mixin.BlockPositionSourceAccessor;
import com.exoticworlds.mixin.MoveVehiclePacketAccessor;
import com.exoticworlds.mixin.PlayerLookAtPacketAccessor;
import com.exoticworlds.mixin.SectionBlocksUpdatePacketAccessor;
import com.exoticworlds.mixin.TeleportEntityPacketAccessor;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.ClientPosition.BorderCenter;
import com.exoticworlds.engine.seam.MirrorWriter;
import com.google.common.base.Suppliers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
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
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
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

    // A section has no stream codec of its own on this version, but it has always travelled as the packed long its
    // own accessors read back, so the codec the swap below needs is two lines rather than a reason to rebuild the packet.
    private static final StreamCodec<FriendlyByteBuf, SectionPos> SECTION_POS_CODEC = StreamCodec.of(
            (buffer, section) -> buffer.writeLong(section.asLong()),
            buffer -> SectionPos.of(buffer.readLong()));

    private static final StreamCodec<FriendlyByteBuf, Vec3> POSITION_CODEC = StreamCodec.of(
            (buffer, position) -> {
                buffer.writeDouble(position.x);
                buffer.writeDouble(position.y);
                buffer.writeDouble(position.z);
            },
            buffer -> new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));

    static Packet<?> chunkPosition(Packet<?> packet, int x, int z, ChunkTraffic traffic,
            TranslationContext context) {
        ChunkPos clientPos = context.toClient(new ChunkPos(x, z), traffic);

        ChunkPacketPosition accessor = (ChunkPacketPosition) packet;
        accessor.toroidal$setX(clientPos.x);
        accessor.toroidal$setZ(clientPos.z);
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
        return new ClientboundSetChunkCacheCenterPacket(clientPos.x, clientPos.z);
    }

    static ClientboundPlayerPositionPacket playerPosition(ClientboundPlayerPositionPacket packet, TranslationContext context) {
        ClientPosition clientPosition = context.clientPosition();

        if (!clientPosition.describes(context.dimension())) {
            context.rebase().run();
            return packet;
        }

        Set<RelativeMovement> relatives = packet.getRelativeArguments();
        boolean relativeX = relatives.contains(RelativeMovement.X);
        boolean relativeZ = relatives.contains(RelativeMovement.Z);
        Vec3 clientDestination = clientPosition.destinationOf(
                context.transformer(), new Vec3(packet.getX(), packet.getY(), packet.getZ()), relatives);
        clientPosition.set(clientDestination, MirrorWriter.POSITION_PACKET);

        double sentX = relativeX
                ? SeamDelta.foldX(context.transformer(), packet.getX())
                : clientDestination.x;
        double sentZ = relativeZ
                ? SeamDelta.foldZ(context.transformer(), packet.getZ())
                : clientDestination.z;
        return new ClientboundPlayerPositionPacket(
                sentX,
                packet.getY(),
                sentZ,
                packet.getYRot(), packet.getXRot(), relatives, packet.getId());
    }

    static ClientboundAddEntityPacket addEntity(ClientboundAddEntityPacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        double clientX = context.toClientX(packet.getX(), reach);
        double clientZ = context.toClientZ(packet.getZ(), reach);
        return new ClientboundAddEntityPacket(
                packet.getId(), packet.getUUID(),
                clientX,
                packet.getY(),
                clientZ,
                packet.getXRot(), packet.getYRot(), packet.getType(), packet.getData(),
                new Vec3(packet.getXa(), packet.getYa(), packet.getZa()), packet.getYHeadRot());
    }

    // An entity teleport carries no relative flags on this version — it is always an absolute position — so there is no
    // axis to leave alone. The packet also offers no constructor to rebuild from values, so the position is swapped on
    // the wire, behind the entity id that opens it.
    static Packet<?> teleportEntity(ClientboundTeleportEntityPacket packet, TranslationContext context) {
        if (context.ownVehicle().test(packet.getId())) {
            return null;
        }

        PacketReach reach = context.trackedReach();
        return PacketTranslator.rewritePosition(
                (teleportPacket, output) -> ((TeleportEntityPacketAccessor) teleportPacket).toroidal$write(output),
                TeleportEntityPacketAccessor::toroidal$create,
                ByteBufCodecs.VAR_INT, POSITION_CODEC,
                packet, context, (entityId, position) -> context.toClient(position, reach));
    }

    static Packet<?> moveVehicle(ClientboundMoveVehiclePacket packet, TranslationContext context) {
        PacketReach reach = context.trackedReach();
        return PacketTranslator.rewritePosition(
                (vehiclePacket, output) -> ((MoveVehiclePacketAccessor) vehiclePacket).toroidal$write(output),
                MoveVehiclePacketAccessor::toroidal$create,
                POSITION_CODEC, packet, context,
                position -> context.toClient(position, reach));
    }

    static ClientboundBlockUpdatePacket blockUpdate(ClientboundBlockUpdatePacket packet, TranslationContext context) {
        return new ClientboundBlockUpdatePacket(
                toClientBlock(context, packet.getPos(), ChunkTraffic.BLOCK_UPDATE), packet.getBlockState());
    }

    static ClientboundSectionBlocksUpdatePacket sectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, TranslationContext context) {
        return PacketTranslator.rewritePosition(
                (sectionPacket, output) -> ((SectionBlocksUpdatePacketAccessor) sectionPacket).toroidal$write(output),
                SectionBlocksUpdatePacketAccessor::toroidal$create,
                SECTION_POS_CODEC, packet, context,
                section -> SectionPos.of(context.toClient(section.chunk(), ChunkTraffic.SECTION_BLOCKS), section.y()));
    }

    static ClientboundBlockEntityDataPacket blockEntityData(ClientboundBlockEntityDataPacket packet, TranslationContext context) {
        return BlockEntityDataPacketAccessor.toroidal$create(
                toClientBlock(context, packet.getPos(), ChunkTraffic.BLOCK_ENTITY), packet.getType(), packet.getTag());
    }

    static ClientboundBlockDestructionPacket blockDestruction(ClientboundBlockDestructionPacket packet, TranslationContext context) {
        return new ClientboundBlockDestructionPacket(
                packet.getId(), toClientBlock(context, packet.getPos(), ChunkTraffic.BLOCK_DESTRUCTION),
                packet.getProgress());
    }

    static ClientboundLevelEventPacket levelEvent(ClientboundLevelEventPacket packet, TranslationContext context) {
        BlockPos clientPos = packet.isGlobalEvent()
                ? nearestCopyBlock(context, packet.getPos())
                : toClientBlock(context, packet.getPos(), ChunkTraffic.LEVEL_EVENT);
        return new ClientboundLevelEventPacket(
                packet.getType(), clientPos, packet.getData(), packet.isGlobalEvent());
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
        PacketRewriters.EntityDataRewriter<Object> dataRewriter = context.rewriters().entityDataFor(item);
        if (dataRewriter != null) {
            Object rewritten = dataRewriter.rewrite(item.value(), context, anchor.get());
            return rewritten == item.value() ? item : withValue(item, rewritten);
        }

        Object value = item.value();
        if (!foldsAsEntityData(value)) {
            return item;
        }

        Object clientValue = FoldedValue.toward(context, anchor, value, particleFold(context, anchor));
        return clientValue == value ? item : withValue(item, clientValue);
    }

    // Past a registered serializer, only the position types this mod owns fold; a foreign value stays as sent.
    private static boolean foldsAsEntityData(Object value) {
        return switch (value) {
            case BlockPos pos -> true;
            case GlobalPos globalPos -> true;
            case ParticleOptions particle -> true;
            case Optional<?> held -> held.isPresent() && foldsAsEntityData(held.get());
            case List<?> values -> isParticleList(values);
            default -> false;
        };
    }

    private static boolean isParticleList(List<?> values) {
        if (values.isEmpty()) {
            return false;
        }

        for (Object value : values) {
            if (!(value instanceof ParticleOptions)) {
                return false;
            }
        }

        return true;
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
                toClientBlock(context, packet.getPos(), ChunkTraffic.BLOCK_EVENT),
                packet.getBlock(), packet.getB0(), packet.getB1());
    }

    static ClientboundOpenSignEditorPacket openSignEditor(ClientboundOpenSignEditorPacket packet, TranslationContext context) {
        return new ClientboundOpenSignEditorPacket(
                toClientBlock(context, packet.getPos(), ChunkTraffic.SIGN_EDITOR), packet.isFrontText());
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
                packet.isOverrideLimiter(),
                clientOrigin.x, clientOrigin.y, clientOrigin.z,
                packet.getXDist(), packet.getYDist(), packet.getZDist(), packet.getMaxSpeed(), packet.getCount());
    }

    static ClientboundExplodePacket explode(ClientboundExplodePacket packet, TranslationContext context) {
        Vec3 serverCenter = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        Vec3 clientCenter = context.toClientMeasured(serverCenter, EXPLOSION_KIND);
        return new ClientboundExplodePacket(
                clientCenter.x, clientCenter.y, clientCenter.z, packet.getPower(),
                toClientBlown(packet.getToBlow(), serverCenter, clientCenter),
                new Vec3(packet.getKnockbackX(), packet.getKnockbackY(), packet.getKnockbackZ()),
                packet.getBlockInteraction(),
                toClientParticle(context, packet.getSmallExplosionParticles(), clientCenter),
                toClientParticle(context, packet.getLargeExplosionParticles(), clientCenter),
                packet.getExplosionSound());
    }

    private static List<BlockPos> toClientBlown(List<BlockPos> blown, Vec3 serverCenter, Vec3 clientCenter) {
        int shiftX = Mth.floor(clientCenter.x) - Mth.floor(serverCenter.x);
        int shiftZ = Mth.floor(clientCenter.z) - Mth.floor(serverCenter.z);
        if (blown.isEmpty() || (shiftX == 0 && shiftZ == 0)) {
            return blown;
        }

        List<BlockPos> translated = new ArrayList<>(blown.size());
        for (BlockPos pos : blown) {
            translated.add(pos.offset(shiftX, 0, shiftZ));
        }

        return translated;
    }

    private static ParticleOptions toClientParticle(TranslationContext context, ParticleOptions particle,
            Vec3 clientOrigin) {
        switch (particle) {
            case VibrationParticleOption vibration -> {
                if (!(vibration.getDestination() instanceof BlockPositionSource destination)) {
                    return particle;
                }

                BlockPos serverDestination = ((BlockPositionSourceAccessor) destination).toroidal$getPos();
                BlockPos clientDestination = FoldedValue.nearestCopy(context, clientOrigin, serverDestination);
                return new VibrationParticleOption(
                        new BlockPositionSource(clientDestination), vibration.getArrivalInTicks());
            }
            default -> {
                PacketRewriters.ParticleRewriter<ParticleOptions> particleRewriter = context.rewriters().particleFor(particle);
                return particleRewriter == null ? particle : particleRewriter.rewrite(particle, context, clientOrigin);
            }
        }
    }

    static Packet<?> setDefaultSpawnPosition(ClientboundSetDefaultSpawnPositionPacket packet, TranslationContext context) {
        if (!Level.OVERWORLD.equals(context.dimension())) {
            return packet;
        }

        BlockPos serverPos = packet.getPos();
        BlockPos clientPos = nearestCopyBlock(context, serverPos);
        context.clientPosition().setHeldSpawn(clientPos);
        return new ClientboundSetDefaultSpawnPositionPacket(clientPos, packet.getAngle());
    }

    static <T extends Packet<?>> Packet<?> borderCenter(T packet, TranslationContext context,
            BiConsumer<T, RegistryFriendlyByteBuf> writer, Function<FriendlyByteBuf, T> reader) {
        if (!context.clientPosition().describes(context.dimension())) {
            return packet;
        }

        return PacketTranslator.rewritePosition(writer, reader, BORDER_CENTER_CODEC, packet, context,
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

    static ClientboundChunksBiomesPacket chunkBiomes(ClientboundChunksBiomesPacket packet, TranslationContext context) {
        return new ClientboundChunksBiomesPacket(packet.chunkBiomeData().stream()
                .map(data -> new ClientboundChunksBiomesPacket.ChunkBiomeData(
                        context.toClient(data.pos(), ChunkTraffic.CHUNK_BIOMES), data.buffer()))
                .toList());
    }

    static BlockPos toClientBlock(TranslationContext context, BlockPos pos, ChunkTraffic traffic) {
        return context.transformer().reseat(pos, context.toClient(new ChunkPos(pos), traffic));
    }

    private static BlockPos nearestCopyBlock(TranslationContext context, BlockPos pos) {
        return context.nearestCopy(pos);
    }

    static BlockPos nearestCopyBlock(WorldFold transformer, ChunkPos anchor, BlockPos pos) {
        return transformer.reseat(pos, transformer.nearestCopy(anchor, new ChunkPos(pos)));
    }

    private ClientboundPacketRewriters() {
    }
}
