package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.FoldOrientation;
import com.mojang.serialization.Lifecycle;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.levelgen.Heightmap;

class TurnedChunkDataTest {
    private static final int BLOCK_WIDTH = 16;
    private static final int BIOME_WIDTH = 4;
    private static final int HEIGHTMAP_CELLS = BLOCK_WIDTH * BLOCK_WIDTH;
    private static final int LIGHT_LEVELS = 16;
    private static final int BLOCK_LIGHT_SHIFT = 5;

    private static final int OVERWORLD_SECTIONS = 24;
    private static final int OVERWORLD_HEIGHT_BITS = 9;
    private static final int TALL_SECTIONS = 128;
    private static final int TALL_HEIGHT_BITS = 12;
    private static final int TALL_HEIGHT_BASE = 2048;

    private static final List<ResourceKey<Biome>> BIOME_KEYS = List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.OCEAN);

    private static final HolderLookup.RegistryLookup<Biome> VANILLA_BIOMES =
            VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME);

    private static final MappedRegistry<Biome> BIOMES = biomeRegistry();

    private static final RegistryAccess REGISTRIES =
            new RegistryAccess.ImmutableRegistryAccess(List.of(BIOMES, BuiltInRegistries.BLOCK_ENTITY_TYPE));

    private static final PalettedContainerFactory CONTAINERS = PalettedContainerFactory.create(REGISTRIES);

    private static final int SENT_SECTIONS = 2;
    private static final int SENT_HEIGHT_BITS = 6;
    private static final int SENT_HEIGHT = 21;
    private static final int SENT_X = 3;
    private static final short SENT_Y = 5;
    private static final int SENT_Z = 7;

    private static final List<BlockState> STATES = List.of(
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
            Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
            Blocks.AIR.defaultBlockState(),
            Blocks.WATER.defaultBlockState());

    private static MappedRegistry<Biome> biomeRegistry() {
        MappedRegistry<Biome> registry = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        for (ResourceKey<Biome> key : BIOME_KEYS) {
            registry.register(key, VANILLA_BIOMES.getOrThrow(key).value(), RegistrationInfo.BUILT_IN);
        }

        registry.freeze();
        return registry;
    }

    private static BlockState stateAt(int x, int y, int z) {
        return STATES.get((x * 7 + z * 13 + y * 3) % STATES.size());
    }

    private static Holder<Biome> biomeAt(int x, int y, int z) {
        return BIOMES.getOrThrow(BIOME_KEYS.get((x + z * 2 + y) % BIOME_KEYS.size()));
    }

    private static LevelChunkSection mixedSection() {
        LevelChunkSection section = new LevelChunkSection(CONTAINERS);
        for (int y = 0; y < BLOCK_WIDTH; y++) {
            for (int z = 0; z < BLOCK_WIDTH; z++) {
                for (int x = 0; x < BLOCK_WIDTH; x++) {
                    section.setBlockState(x, y, z, stateAt(x, y, z), false);
                }
            }
        }

        var biomes = CONTAINERS.createForBiomes();
        for (int y = 0; y < BIOME_WIDTH; y++) {
            for (int z = 0; z < BIOME_WIDTH; z++) {
                for (int x = 0; x < BIOME_WIDTH; x++) {
                    biomes.set(x, y, z, biomeAt(x, y, z));
                }
            }
        }

        return new LevelChunkSection(section.getStates(), biomes);
    }

    private static byte[] vanillaBuffer(List<LevelChunkSection> sections) {
        int size = sections.stream().mapToInt(LevelChunkSection::getSerializedSize).sum();
        byte[] buffer = new byte[size];
        FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer).writerIndex(0));
        sections.forEach(section -> section.write(output));
        return buffer;
    }

    private static List<LevelChunkSection> reencoded(FoldOrientation orientation) {
        byte[] sent = vanillaBuffer(List.of(mixedSection(), new LevelChunkSection(CONTAINERS)));
        List<LevelChunkSection> turned =
                TurnedChunkData.turnedSections(TurnedChunkData.read(sent, CONTAINERS), orientation, CONTAINERS);
        return TurnedChunkData.read(TurnedChunkData.write(turned), CONTAINERS);
    }

    private static ClientboundLevelChunkPacketData sentData() {
        SimpleBitStorage heights = new SimpleBitStorage(SENT_HEIGHT_BITS, HEIGHTMAP_CELLS);
        heights.set(SENT_Z * BLOCK_WIDTH + SENT_X, SENT_HEIGHT);
        Map<Heightmap.Types, long[]> heightmaps = new EnumMap<>(Heightmap.Types.class);
        heightmaps.put(Heightmap.Types.WORLD_SURFACE, heights.getRaw());
        byte[] sections = vanillaBuffer(List.of(mixedSection(), new LevelChunkSection(CONTAINERS)));

        RegistryFriendlyByteBuf sent = new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
        TurnedChunkData.HEIGHTMAPS_CODEC.encode(sent, heightmaps);
        sent.writeVarInt(sections.length);
        sent.writeBytes(sections);
        sent.writeVarInt(1);
        sent.writeByte(SENT_X << 4 | SENT_Z);
        sent.writeShort(SENT_Y);
        TurnedChunkData.BLOCK_ENTITY_TYPE_CODEC.encode(sent, BlockEntityTypes.CHEST);
        sent.writeNbt(null);
        return new ClientboundLevelChunkPacketData(sent, 0, 0);
    }

    private static byte[] sectionBytes(ClientboundLevelChunkPacketData data) {
        FriendlyByteBuf buffer = data.getReadBuffer();
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    @Test
    void anUprightSeatGetsTheSentDataItself() {
        ClientboundLevelChunkPacketData sent = sentData();
        assertSame(sent, TurnedChunkData.turned(sent, FoldOrientation.IDENTITY, () -> {
            throw new AssertionError("an upright seat asked for the registries");
        }));
    }

    @Test
    void aTurnedSeatGetsEveryPartInItsOwnFrame() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            if (orientation.isIdentity()) {
                continue;
            }

            assertTurnedParts(orientation, TurnedChunkData.turned(sentData(), orientation, () -> REGISTRIES));
        }
    }

    private static void assertTurnedParts(FoldOrientation orientation, ClientboundLevelChunkPacketData turned) {
        int seatX = orientation.applyToLocalX(SENT_X, SENT_Z, BLOCK_WIDTH);
        int seatZ = orientation.applyToLocalZ(SENT_X, SENT_Z, BLOCK_WIDTH);

        List<BlockPos> blockEntities = new ArrayList<>();
        turned.getBlockEntitiesTagsConsumer(0, 0).accept((pos, type, tag) -> blockEntities.add(pos.immutable()));
        assertEquals(List.of(new BlockPos(seatX, SENT_Y, seatZ)), blockEntities,
                orientation + " misplaced the block entity");

        SimpleBitStorage heights = new SimpleBitStorage(SENT_HEIGHT_BITS, HEIGHTMAP_CELLS,
                turned.getHeightmaps().get(Heightmap.Types.WORLD_SURFACE));
        assertEquals(SENT_HEIGHT, heights.get(seatZ * BLOCK_WIDTH + seatX), orientation + " misplaced the height");

        List<LevelChunkSection> sections = TurnedChunkData.read(sectionBytes(turned), CONTAINERS);
        assertSame(orientation.applyToState(stateAt(SENT_X, SENT_Y, SENT_Z)),
                sections.getFirst().getBlockState(seatX, SENT_Y, seatZ), orientation + " misplaced the block");
    }

    @Test
    void aReencodedChunkKeepsItsSectionCount() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            assertEquals(SENT_SECTIONS, reencoded(orientation).size(), orientation + " changed the section count");
        }
    }

    @Test
    void eachBlockLandsAtItsPermutedCellWithItsStateTurned() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            LevelChunkSection section = reencoded(orientation).getFirst();
            forEachCell(BLOCK_WIDTH, (x, y, z) -> assertSame(
                    orientation.applyToState(stateAt(x, y, z)),
                    section.getBlockState(
                            orientation.applyToLocalX(x, z, BLOCK_WIDTH), y, orientation.applyToLocalZ(x, z, BLOCK_WIDTH)),
                    orientation + " misplaced the block of (" + x + ", " + y + ", " + z + ")"));
        }
    }

    @Test
    void anEmptySectionStaysEmpty() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            assertTrue(reencoded(orientation).getLast().hasOnlyAir(), orientation + " filled an empty section");
        }
    }

    @Test
    void eachBiomeLandsAtItsPermutedQuart() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            LevelChunkSection section = reencoded(orientation).getFirst();
            forEachCell(BIOME_WIDTH, (x, y, z) -> assertSame(
                    biomeAt(x, y, z),
                    section.getNoiseBiome(
                            orientation.applyToLocalX(x, z, BIOME_WIDTH), y, orientation.applyToLocalZ(x, z, BIOME_WIDTH)),
                    orientation + " misplaced the biome of (" + x + ", " + y + ", " + z + ")"));
        }
    }

    @Test
    void eachHeightLandsAtItsPermutedColumn() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            assertHeightsFollow(orientation, OVERWORLD_SECTIONS, OVERWORLD_HEIGHT_BITS, 0);
        }
    }

    @Test
    void aTallWorldsHeightsAreReadAtTheirOwnCellWidth() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            assertHeightsFollow(orientation, TALL_SECTIONS, TALL_HEIGHT_BITS, TALL_HEIGHT_BASE);
        }
    }

    private static void assertHeightsFollow(FoldOrientation orientation, int sectionCount, int bits, int base) {
        SimpleBitStorage sent = new SimpleBitStorage(bits, HEIGHTMAP_CELLS);
        for (int cell = 0; cell < HEIGHTMAP_CELLS; cell++) {
            sent.set(cell, base + cell);
        }

        SimpleBitStorage turned = new SimpleBitStorage(bits, HEIGHTMAP_CELLS,
                TurnedChunkData.heightmap(sent.getRaw(), orientation, sectionCount));
        for (int z = 0; z < BLOCK_WIDTH; z++) {
            for (int x = 0; x < BLOCK_WIDTH; x++) {
                int landed = orientation.applyToLocalZ(x, z, BLOCK_WIDTH) * BLOCK_WIDTH
                        + orientation.applyToLocalX(x, z, BLOCK_WIDTH);
                assertEquals(base + z * BLOCK_WIDTH + x, turned.get(landed),
                        orientation + " misplaced the height of (" + x + ", " + z + ")");
            }
        }
    }

    @Test
    void aBlockEntityMovesWithItsColumn() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            for (int z = 0; z < BLOCK_WIDTH; z++) {
                for (int x = 0; x < BLOCK_WIDTH; x++) {
                    int landed = orientation.applyToLocalX(x, z, BLOCK_WIDTH) << 4
                            | orientation.applyToLocalZ(x, z, BLOCK_WIDTH);
                    assertEquals((byte) landed, TurnedChunkData.packedXZ((byte) (x << 4 | z), orientation),
                            orientation + " misplaced the block entity of (" + x + ", " + z + ")");
                }
            }
        }
    }

    private static int lightAt(int x, int y, int z) {
        return (x * 3 + z * 5 + y * 7) % LIGHT_LEVELS;
    }

    private static byte[] lightLayer(int shift) {
        DataLayer layer = new DataLayer();
        forEachCell(BLOCK_WIDTH, (x, y, z) -> layer.set(x, y, z, (lightAt(x, y, z) + shift) % LIGHT_LEVELS));
        return layer.getData();
    }

    private static BitSet sections(int... indexes) {
        BitSet mask = new BitSet();
        for (int index : indexes) {
            mask.set(index);
        }

        return mask;
    }

    private static ClientboundLightUpdatePacketData sentLight() {
        FriendlyByteBuf sent = new FriendlyByteBuf(Unpooled.buffer());
        sent.writeBitSet(sections(1));
        sent.writeBitSet(sections(2));
        sent.writeBitSet(sections(3));
        sent.writeBitSet(sections(4));
        sent.writeCollection(List.of(lightLayer(0)), ByteBufCodecs.byteArray(DataLayer.SIZE));
        sent.writeCollection(List.of(lightLayer(BLOCK_LIGHT_SHIFT)), ByteBufCodecs.byteArray(DataLayer.SIZE));
        return new ClientboundLightUpdatePacketData(sent, 0, 0);
    }

    @Test
    void eachLightNibbleLandsAtItsPermutedCell() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            DataLayer turned = new DataLayer(
                    TurnedChunkData.lightLayer(lightLayer(0), orientation.localIndexTable(BLOCK_WIDTH)));
            forEachCell(BLOCK_WIDTH, (x, y, z) -> assertEquals(
                    lightAt(x, y, z),
                    turned.get(orientation.applyToLocalX(x, z, BLOCK_WIDTH), y,
                            orientation.applyToLocalZ(x, z, BLOCK_WIDTH)),
                    orientation + " misplaced the light of (" + x + ", " + y + ", " + z + ")"));
        }
    }

    @Test
    void aTurnedSeatGetsBothLightLayersUnderTheSentMasks() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            if (orientation.isIdentity()) {
                continue;
            }

            ClientboundLightUpdatePacketData sent = sentLight();
            ClientboundLightUpdatePacketData turned = TurnedChunkData.turned(sent, orientation);
            int[] seats = orientation.localIndexTable(BLOCK_WIDTH);

            assertEquals(sent.getSkyYMask(), turned.getSkyYMask(), orientation + " changed the sky mask");
            assertEquals(sent.getBlockYMask(), turned.getBlockYMask(), orientation + " changed the block mask");
            assertEquals(sent.getEmptySkyYMask(), turned.getEmptySkyYMask(),
                    orientation + " changed the empty sky mask");
            assertEquals(sent.getEmptyBlockYMask(), turned.getEmptyBlockYMask(),
                    orientation + " changed the empty block mask");
            assertEquals(1, turned.getSkyUpdates().size(), orientation + " changed the sky layer count");
            assertEquals(1, turned.getBlockUpdates().size(), orientation + " changed the block layer count");
            assertArrayEquals(TurnedChunkData.lightLayer(lightLayer(0), seats), turned.getSkyUpdates().getFirst(),
                    orientation + " misplaced the sky light");
            assertArrayEquals(TurnedChunkData.lightLayer(lightLayer(BLOCK_LIGHT_SHIFT), seats),
                    turned.getBlockUpdates().getFirst(), orientation + " misplaced the block light");
        }
    }

    @Test
    void anUprightSeatGetsTheSentLightItself() {
        ClientboundLightUpdatePacketData sent = sentLight();
        assertSame(sent, TurnedChunkData.turned(sent, FoldOrientation.IDENTITY));
    }

    private static byte[] biomeBuffer() {
        List<PalettedContainerRO<Holder<Biome>>> sent =
                List.of(mixedSection().getBiomes(), CONTAINERS.createForBiomes());
        byte[] buffer = new byte[sent.stream().mapToInt(PalettedContainerRO::getSerializedSize).sum()];
        FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer).writerIndex(0));
        sent.forEach(biomes -> biomes.write(output));
        return buffer;
    }

    private static List<PalettedContainer<Holder<Biome>>> readBiomes(byte[] buffer) {
        FriendlyByteBuf input = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer));
        List<PalettedContainer<Holder<Biome>>> read = new ArrayList<>();
        while (input.isReadable()) {
            PalettedContainer<Holder<Biome>> biomes = CONTAINERS.createForBiomes();
            biomes.read(input);
            read.add(biomes);
        }

        return read;
    }

    @Test
    void eachBiomeOfABiomeBufferLandsAtItsPermutedQuart() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            List<PalettedContainer<Holder<Biome>>> turned =
                    readBiomes(TurnedChunkData.turnedBiomes(biomeBuffer(), orientation, () -> REGISTRIES));
            assertEquals(SENT_SECTIONS, turned.size(), orientation + " changed the section count");
            forEachCell(BIOME_WIDTH, (x, y, z) -> assertSame(
                    biomeAt(x, y, z),
                    turned.getFirst().get(orientation.applyToLocalX(x, z, BIOME_WIDTH), y,
                            orientation.applyToLocalZ(x, z, BIOME_WIDTH)),
                    orientation + " misplaced the biome of (" + x + ", " + y + ", " + z + ")"));
        }
    }

    @Test
    void anUprightSeatGetsTheSentBiomeBufferItself() {
        byte[] sent = biomeBuffer();
        assertSame(sent, TurnedChunkData.turnedBiomes(sent, FoldOrientation.IDENTITY, () -> {
            throw new AssertionError("an upright seat asked for the registries");
        }));
    }

    @Test
    void eachSectionChangeLandsAtItsPermutedCellWithItsStateTurned() {
        for (FoldOrientation orientation : FoldOrientation.values()) {
            short[] positions = new short[BLOCK_WIDTH * BLOCK_WIDTH * BLOCK_WIDTH];
            BlockState[] states = new BlockState[positions.length];
            List<BlockPos> sent = new ArrayList<>();
            forEachCell(BLOCK_WIDTH, (x, y, z) -> {
                positions[sent.size()] = SectionPos.sectionRelativePos(new BlockPos(x, y, z));
                states[sent.size()] = stateAt(x, y, z);
                sent.add(new BlockPos(x, y, z));
            });

            TurnedChunkData.turnSectionChanges(positions, states, orientation);

            for (int change = 0; change < positions.length; change++) {
                BlockPos cell = sent.get(change);
                BlockPos seat = new BlockPos(
                        orientation.applyToLocalX(cell.getX(), cell.getZ(), BLOCK_WIDTH), cell.getY(),
                        orientation.applyToLocalZ(cell.getX(), cell.getZ(), BLOCK_WIDTH));
                assertEquals(SectionPos.sectionRelativePos(seat), positions[change],
                        orientation + " misplaced the change of " + cell);
                assertSame(orientation.applyToState(stateAt(cell.getX(), cell.getY(), cell.getZ())), states[change],
                        orientation + " left the state of " + cell + " unturned");
            }
        }
    }

    private static void forEachCell(int width, CellCheck check) {
        List<int[]> cells = new ArrayList<>();
        for (int y = 0; y < width; y++) {
            for (int z = 0; z < width; z++) {
                for (int x = 0; x < width; x++) {
                    cells.add(new int[] {x, y, z});
                }
            }
        }

        cells.forEach(cell -> check.at(cell[0], cell[1], cell[2]));
    }

    @FunctionalInterface
    private interface CellCheck {
        void at(int x, int y, int z);
    }
}
