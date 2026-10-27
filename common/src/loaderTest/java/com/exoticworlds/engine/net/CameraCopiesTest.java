package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.net.PacketTranslatorFixture.BUFFERS;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.MIRROR_X;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.MIRROR_Z;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.REGISTRIES;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.FOLD;
import static com.exoticworlds.engine.net.PacketTranslatorFixture.VIEW_DISTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.engine.seam.ClientPosition;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

class CameraCopiesTest {
    private static final ChunkPos HELD_CENTER = new ChunkPos(36, -44);
    private static final Vec3 CAMERA = new Vec3(1040.0, 64.0, -700.0);
    private static final Vec3 CAMERA_IN_THE_HELD_CENTER = new Vec3(MIRROR_X, 64.0, MIRROR_Z);

    private static final ChunkPos OVERLAP_CHUNK = new ChunkPos(18, -44);
    private static final ChunkPos OVERLAP_CAMERA_CHUNK = new ChunkPos(82, -44);
    private static final BlockPos OVERLAP_BLOCK = new BlockPos(293, 64, -700);
    private static final BlockPos OVERLAP_CAMERA_BLOCK = new BlockPos(1317, 64, -700);

    private static TranslationContext contextWithCamera(Vec3 camera) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, FOLD);
        mirror.setHeldCacheCenter(HELD_CENTER);
        mirror.followCamera(camera, Level.OVERWORLD);
        return new TranslationContext(FOLD, mirror, BUFFERS, Level.OVERWORLD,
                VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, entityId -> null, () -> {});
    }

    private static ClientboundLevelChunkWithLightPacket levelChunk(ChunkPos pos) {
        RegistryFriendlyByteBuf buf = buffer();
        buf.writeInt(pos.x());
        buf.writeInt(pos.z());
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        writeEmptyLightData(buf);
        return ClientboundLevelChunkWithLightPacket.STREAM_CODEC.decode(buf);
    }

    private static ClientboundLightUpdatePacket lightUpdate(ChunkPos pos) {
        RegistryFriendlyByteBuf buf = buffer();
        buf.writeVarInt(pos.x());
        buf.writeVarInt(pos.z());
        writeEmptyLightData(buf);
        return ClientboundLightUpdatePacket.STREAM_CODEC.decode(buf);
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    }

    private static void writeEmptyLightData(FriendlyByteBuf buf) {
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeVarInt(0);
        buf.writeVarInt(0);
    }

    private static List<Object> sent(Packet<?> packet) {
        List<Object> positions = new ArrayList<>();
        List<Packet<?>> packets = new ArrayList<>();
        if (packet instanceof ClientboundBundlePacket bundle) {
            bundle.subPackets().forEach(packets::add);
        } else {
            packets.add(packet);
        }

        for (Packet<?> sub : packets) {
            positions.add(switch (sub) {
                case ClientboundLevelChunkWithLightPacket chunk -> "chunk " + new ChunkPos(chunk.getX(), chunk.getZ());
                case ClientboundLightUpdatePacket light -> "light " + new ChunkPos(light.getX(), light.getZ());
                case ClientboundBlockUpdatePacket block -> "block " + block.getPos().toShortString();
                case ClientboundOpenSignEditorPacket sign -> "sign " + sign.getPos().toShortString();
                case ClientboundForgetLevelChunkPacket forget -> "forget " + forget.pos();
                default -> sub;
            });
        }

        return positions;
    }

    @Test
    void aChunkInsideBothViewsGoesOutOnBothCopies() {
        Packet<?> translated = PacketTranslator.toClient(levelChunk(OVERLAP_CHUNK), contextWithCamera(CAMERA));

        assertEquals(List.of("chunk " + OVERLAP_CHUNK, "chunk " + OVERLAP_CAMERA_CHUNK), sent(translated));
    }

    @Test
    void aBlockUpdateInsideBothViewsGoesOutOnBothCopies() {
        Packet<?> translated = PacketTranslator.toClient(
                new ClientboundBlockUpdatePacket(OVERLAP_BLOCK, Blocks.STONE.defaultBlockState()),
                contextWithCamera(CAMERA));

        assertEquals(List.of("block " + OVERLAP_BLOCK.toShortString(), "block " + OVERLAP_CAMERA_BLOCK.toShortString()),
                sent(translated));
    }

    @Test
    void theCopiesJoinTheBundleTheyArrivedIn() {
        List<Packet<? super ClientGamePacketListener>> arriving =
                List.of(levelChunk(OVERLAP_CHUNK), lightUpdate(OVERLAP_CHUNK));

        Packet<?> translated = PacketTranslator.toClient(
                new ClientboundBundlePacket(arriving), contextWithCamera(CAMERA));

        for (Packet<?> sub : ((ClientboundBundlePacket) translated).subPackets()) {
            assertFalse(sub instanceof ClientboundBundlePacket, "a bundle nested inside the arriving bundle");
        }
        assertEquals(List.of("chunk " + OVERLAP_CHUNK, "chunk " + OVERLAP_CAMERA_CHUNK,
                "light " + OVERLAP_CHUNK, "light " + OVERLAP_CAMERA_CHUNK), sent(translated));
    }

    @Test
    void aForgetInsideBothViewsReachesBothCopies() {
        Packet<?> translated = PacketTranslator.toClient(
                new ClientboundForgetLevelChunkPacket(OVERLAP_CHUNK), contextWithCamera(CAMERA));

        assertEquals(Set.of("forget " + OVERLAP_CHUNK, "forget " + OVERLAP_CAMERA_CHUNK), Set.copyOf(sent(translated)));
    }

    @Test
    void aSignEditorInsideBothViewsOpensOnce() {
        Packet<?> translated = PacketTranslator.toClient(
                new ClientboundOpenSignEditorPacket(OVERLAP_BLOCK, true), contextWithCamera(CAMERA));

        assertEquals(List.of("sign " + OVERLAP_BLOCK.toShortString()), sent(translated));
    }

    @Test
    void aCameraInTheHeldCenterChunkSendsOneCopy() {
        Packet<?> translated = PacketTranslator.toClient(
                levelChunk(OVERLAP_CHUNK), contextWithCamera(CAMERA_IN_THE_HELD_CENTER));

        assertEquals(List.of("chunk " + OVERLAP_CHUNK), sent(translated));
    }
}
