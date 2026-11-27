package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.ticks.ProtoChunkTicks;

class BulkSectionAccessSeatTest {
    private static final int MIN_Y = 0;
    private static final int HEIGHT_BLOCKS = 16;
    private static final int SECTION_Y = 0;
    private static final int TIMEOUT_SECONDS = 10;
    private static final ChunkPos PHYSICAL = new ChunkPos(-17, -16);
    private static final ChunkPos PAST_THE_VERTEX = new ChunkPos(-17, -17);
    private static final String GET_MIN_Y = "getMinY";
    private static final String GET_HEIGHT = "getHeight";
    private static final String GET_CHUNK = "getChunk";
    private static final int GET_CHUNK_FULL_ARITY = 4;
    private static final String THREADING_DETECTOR_FIELD = "threadingDetector";
    private static final String LOCK_FIELD = "lock";

    private static ProtoChunk chunkAt(ChunkPos pos) {
        LevelHeightAccessor height = LevelHeightAccessor.create(MIN_Y, HEIGHT_BLOCKS);
        PalettedContainer<BlockState> states = new PalettedContainer<>(
                Blocks.AIR.defaultBlockState(), Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY));
        LevelChunkSection[] sections = {new LevelChunkSection(states, null)};
        return new ProtoChunk(pos, UpgradeData.EMPTY, sections, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(),
                height, null, null);
    }

    private static LevelAccessor levelAnswering(ProtoChunk chunk) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals(GET_MIN_Y)) {
                return MIN_Y;
            }

            if (method.getName().equals(GET_HEIGHT)) {
                return HEIGHT_BLOCKS;
            }

            if (method.getName().equals(GET_CHUNK) && method.getParameterCount() == GET_CHUNK_FULL_ARITY) {
                return chunk;
            }

            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }

            throw new UnsupportedOperationException(method.toString());
        };
        return (LevelAccessor) Proxy.newProxyInstance(
                LevelAccessor.class.getClassLoader(), new Class<?>[] {LevelAccessor.class}, handler);
    }

    private static BlockPos blockIn(ChunkPos pos) {
        return new BlockPos(pos.getMinBlockX(), SectionPos.sectionToBlockCoord(SECTION_Y), pos.getMinBlockZ());
    }

    private static int freeLocks(LevelChunkSection section) throws ReflectiveOperationException {
        Field detectorField = PalettedContainer.class.getDeclaredField(THREADING_DETECTOR_FIELD);
        detectorField.setAccessible(true);
        Field lockField = ThreadingDetector.class.getDeclaredField(LOCK_FIELD);
        lockField.setAccessible(true);
        Object detector = detectorField.get(section.getStates());
        return ((Semaphore) lockField.get(detector)).availablePermits();
    }

    @Test
    @Timeout(TIMEOUT_SECONDS)
    void twoRawNamesOfOneChunkAcquireItsSectionOnceAndReleaseItOnce() throws ReflectiveOperationException {
        ProtoChunk chunk = chunkAt(PHYSICAL);
        LevelChunkSection section = chunk.getSection(SECTION_Y);

        try (BulkSectionAccess access = new BulkSectionAccess(levelAnswering(chunk))) {
            assertSame(section, access.getSection(blockIn(PHYSICAL)));
            assertSame(section, access.getSection(blockIn(PAST_THE_VERTEX)));
            assertSame(section, access.getSection(blockIn(PHYSICAL)));
            assertEquals(0, freeLocks(section));
        }

        assertEquals(1, freeLocks(section));
    }
}
