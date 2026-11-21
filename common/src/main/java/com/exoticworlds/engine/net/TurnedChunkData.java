package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.FoldOrientation;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityInfo;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.block.state.BlockState;
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

    static ClientboundLevelChunkPacketData turned(ClientboundLevelChunkPacketData data, FoldOrientation orientation,
            Supplier<RegistryAccess> registries) {
        if (orientation.isIdentity()) {
            return data;
        }

        PalettedContainerFactory containers = PalettedContainerFactory.create(registries.get());
        List<LevelChunkSection> sections = read(data.buffer, containers);

        Map<Heightmap.Types, long[]> heightmaps = new EnumMap<>(Heightmap.Types.class);
        data.getHeightmaps().forEach(
                (type, cells) -> heightmaps.put(type, heightmap(cells, orientation, sections.size())));

        List<BlockEntityInfo> blockEntities = new ArrayList<>(data.blockEntitiesData.size());
        for (BlockEntityInfo blockEntity : data.blockEntitiesData) {
            blockEntities.add(new BlockEntityInfo(packedXZ(blockEntity.packedXZ(), orientation), blockEntity.y(),
                    blockEntity.type(), blockEntity.tag()));
        }

        return new ClientboundLevelChunkPacketData(
                heightmaps, write(turnedSections(sections, orientation, containers)), blockEntities);
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

    private TurnedChunkData() {
    }
}
