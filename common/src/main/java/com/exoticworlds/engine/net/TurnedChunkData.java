package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.FoldOrientation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.levelgen.Heightmap;

final class TurnedChunkData {
    private static final int BLOCK_WIDTH = CoordinateConstants.CHUNK_WIDTH;

    private static final int BIOME_WIDTH = 4;

    private static final int LOCAL_MASK = BLOCK_WIDTH - 1;

    private static final int PACKED_X_SHIFT = 4;

    private static final int SECTION_X_SHIFT = 8;

    private static final int SECTION_Z_SHIFT = 4;

    private static final StreamCodec<ByteBuf, byte[]> LIGHT_LAYER_CODEC = ByteBufCodecs.byteArray(DataLayer.SIZE);

    static final StreamCodec<ByteBuf, Map<Heightmap.Types, long[]>> HEIGHTMAPS_CODEC = ByteBufCodecs.map(
            size -> new EnumMap<>(Heightmap.Types.class), Heightmap.Types.STREAM_CODEC, ByteBufCodecs.LONG_ARRAY);

    static final StreamCodec<RegistryFriendlyByteBuf, BlockEntityType<?>> BLOCK_ENTITY_TYPE_CODEC =
            ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE);

    static ClientboundLevelChunkPacketData turned(ClientboundLevelChunkPacketData data, FoldOrientation orientation,
            Supplier<RegistryAccess> registries) {
        if (orientation.isIdentity()) {
            return data;
        }

        RegistryAccess access = registries.get();
        PalettedContainerFactory containers = PalettedContainerFactory.create(access);
        RegistryFriendlyByteBuf sent = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        data.write(sent);
        Map<Heightmap.Types, long[]> sentHeightmaps = HEIGHTMAPS_CODEC.decode(sent);
        byte[] sentSections = new byte[sent.readVarInt()];
        sent.readBytes(sentSections);
        List<LevelChunkSection> sections = read(sentSections, containers);

        Map<Heightmap.Types, long[]> heightmaps = new EnumMap<>(Heightmap.Types.class);
        sentHeightmaps.forEach((type, cells) -> heightmaps.put(type, heightmap(cells, orientation, sections.size())));

        RegistryFriendlyByteBuf turned = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        HEIGHTMAPS_CODEC.encode(turned, heightmaps);
        byte[] turnedSections = write(turnedSections(sections, orientation, containers));
        turned.writeVarInt(turnedSections.length);
        turned.writeBytes(turnedSections);
        copyTurnedBlockEntities(sent, turned, orientation);
        return new ClientboundLevelChunkPacketData(turned, 0, 0);
    }

    private static void copyTurnedBlockEntities(RegistryFriendlyByteBuf sent, RegistryFriendlyByteBuf turned,
            FoldOrientation orientation) {
        int count = ByteBufCodecs.readCount(sent, Integer.MAX_VALUE);
        ByteBufCodecs.writeCount(turned, count, Integer.MAX_VALUE);
        for (int i = 0; i < count; i++) {
            turned.writeByte(packedXZ(sent.readByte(), orientation));
            turned.writeShort(sent.readShort());
            BLOCK_ENTITY_TYPE_CODEC.encode(turned, BLOCK_ENTITY_TYPE_CODEC.decode(sent));
            turned.writeNbt(sent.readNbt());
        }
    }

    static List<LevelChunkSection> read(byte[] buffer, PalettedContainerFactory containers) {
        FriendlyByteBuf input = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer));
        List<LevelChunkSection> sections = new ArrayList<>();
        while (input.isReadable()) {
            LevelChunkSection section = new LevelChunkSection(containers);
            section.read(input);
            sections.add(section);
        }

        return sections;
    }

    static byte[] write(List<LevelChunkSection> sections) {
        int size = 0;
        for (LevelChunkSection section : sections) {
            size += section.getSerializedSize();
        }

        byte[] buffer = new byte[size];
        FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer).writerIndex(0));
        for (LevelChunkSection section : sections) {
            section.write(output);
        }

        return buffer;
    }

    static List<LevelChunkSection> turnedSections(List<LevelChunkSection> sections, FoldOrientation orientation,
            PalettedContainerFactory containers) {
        int[] blockSeats = orientation.localIndexTable(BLOCK_WIDTH);
        int[] biomeSeats = orientation.localIndexTable(BIOME_WIDTH);
        Map<BlockState, BlockState> turnedStates = new Reference2ReferenceOpenHashMap<>();
        UnaryOperator<BlockState> turn = state -> turnedStates.computeIfAbsent(state, orientation::applyToState);

        List<LevelChunkSection> turned = new ArrayList<>(sections.size());
        for (LevelChunkSection section : sections) {
            turned.add(new LevelChunkSection(
                    seated(section.getStates(), containers.createForBlockStates(), blockSeats, BLOCK_WIDTH, turn),
                    seated(section.getBiomes(), containers.createForBiomes(), biomeSeats, BIOME_WIDTH,
                            UnaryOperator.identity())));
        }

        return turned;
    }

    private static <T> PalettedContainer<T> seated(PalettedContainerRO<T> source, PalettedContainer<T> target,
            int[] seats, int width, UnaryOperator<T> turn) {
        for (int y = 0; y < width; y++) {
            for (int cell = 0; cell < seats.length; cell++) {
                int seat = seats[cell];
                target.getAndSetUnchecked(seat % width, y, seat / width,
                        turn.apply(source.get(cell % width, y, cell / width)));
            }
        }

        return target;
    }

    static long[] heightmap(long[] cells, FoldOrientation orientation, int sectionCount) {
        int bits = Mth.ceillog2(sectionCount * BLOCK_WIDTH + 1);
        int[] seats = orientation.localIndexTable(BLOCK_WIDTH);
        SimpleBitStorage source = new SimpleBitStorage(bits, seats.length, cells);
        SimpleBitStorage target = new SimpleBitStorage(bits, seats.length);
        for (int cell = 0; cell < seats.length; cell++) {
            target.set(seats[cell], source.get(cell));
        }

        return target.getRaw();
    }

    static byte packedXZ(byte packedXZ, FoldOrientation orientation) {
        int localX = packedXZ >> PACKED_X_SHIFT & LOCAL_MASK;
        int localZ = packedXZ & LOCAL_MASK;
        return (byte) (orientation.applyToLocalX(localX, localZ, BLOCK_WIDTH) << PACKED_X_SHIFT
                | orientation.applyToLocalZ(localX, localZ, BLOCK_WIDTH));
    }

    static ClientboundLightUpdatePacketData turned(ClientboundLightUpdatePacketData data,
            FoldOrientation orientation) {
        if (orientation.isIdentity()) {
            return data;
        }

        int[] seats = orientation.localIndexTable(BLOCK_WIDTH);
        FriendlyByteBuf turned = new FriendlyByteBuf(Unpooled.buffer());
        turned.writeBitSet(data.getSkyYMask());
        turned.writeBitSet(data.getBlockYMask());
        turned.writeBitSet(data.getEmptySkyYMask());
        turned.writeBitSet(data.getEmptyBlockYMask());
        turned.writeCollection(lightLayers(data.getSkyUpdates(), seats), LIGHT_LAYER_CODEC);
        turned.writeCollection(lightLayers(data.getBlockUpdates(), seats), LIGHT_LAYER_CODEC);
        return new ClientboundLightUpdatePacketData(turned, 0, 0);
    }

    private static List<byte[]> lightLayers(List<byte[]> layers, int[] seats) {
        List<byte[]> turned = new ArrayList<>(layers.size());
        for (byte[] layer : layers) {
            turned.add(lightLayer(layer, seats));
        }

        return turned;
    }

    static byte[] lightLayer(byte[] layer, int[] seats) {
        DataLayer source = new DataLayer(layer);
        DataLayer target = new DataLayer();
        for (int y = 0; y < BLOCK_WIDTH; y++) {
            for (int cell = 0; cell < seats.length; cell++) {
                int seat = seats[cell];
                target.set(seat % BLOCK_WIDTH, y, seat / BLOCK_WIDTH,
                        source.get(cell % BLOCK_WIDTH, y, cell / BLOCK_WIDTH));
            }
        }

        return target.getData();
    }

    static byte[] turnedBiomes(byte[] buffer, FoldOrientation orientation, Supplier<RegistryAccess> registries) {
        if (orientation.isIdentity()) {
            return buffer;
        }

        PalettedContainerFactory containers = PalettedContainerFactory.create(registries.get());
        int[] seats = orientation.localIndexTable(BIOME_WIDTH);
        FriendlyByteBuf input = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer));
        List<PalettedContainer<Holder<Biome>>> turned = new ArrayList<>();
        int size = 0;
        while (input.isReadable()) {
            PalettedContainer<Holder<Biome>> sent = containers.createForBiomes();
            sent.read(input);
            PalettedContainer<Holder<Biome>> seated =
                    seated(sent, containers.createForBiomes(), seats, BIOME_WIDTH, UnaryOperator.identity());
            size += seated.getSerializedSize();
            turned.add(seated);
        }

        byte[] written = new byte[size];
        FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.wrappedBuffer(written).writerIndex(0));
        for (PalettedContainer<Holder<Biome>> biomes : turned) {
            biomes.write(output);
        }

        return written;
    }

    static void turnSectionChanges(short[] positions, BlockState[] states, FoldOrientation orientation) {
        if (orientation.isIdentity()) {
            return;
        }

        for (int change = 0; change < positions.length; change++) {
            short packed = positions[change];
            int localX = SectionPos.sectionRelativeX(packed);
            int localZ = SectionPos.sectionRelativeZ(packed);
            positions[change] = (short) (orientation.applyToLocalX(localX, localZ, BLOCK_WIDTH) << SECTION_X_SHIFT
                    | orientation.applyToLocalZ(localX, localZ, BLOCK_WIDTH) << SECTION_Z_SHIFT
                    | SectionPos.sectionRelativeY(packed));
            states[change] = orientation.applyToState(states[change]);
        }
    }

    private TurnedChunkData() {
    }
}
