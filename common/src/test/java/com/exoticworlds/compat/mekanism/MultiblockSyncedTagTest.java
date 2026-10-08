package com.exoticworlds.compat.mekanism;

import static com.exoticworlds.compat.CompatFoldFixture.CYLINDER;
import static com.exoticworlds.compat.CompatFoldFixture.DECK_TORUS;
import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.SKEWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

class MultiblockSyncedTagTest {
    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED, CYLINDER);

    private static final BlockPos MIN = new BlockPos(255, 315, 0);
    private static final BlockPos MAX = new BlockPos(257, 317, 2);
    private static final BlockPos RENDER_LOCATION = new BlockPos(255, 316, 0);
    private static final BlockPos RENDER_Y = new BlockPos(255, 317, 0);
    private static final BlockPos VALVE = new BlockPos(257, 316, 1);
    private static final BlockPos COIL = new BlockPos(256, 316, 2);
    private static final BlockPos COMPLEX = new BlockPos(256, 316, 1);
    private static final BlockPos ASSEMBLY = new BlockPos(256, 316, 0);
    private static final BlockPos TILE_PAST_THE_EDGE = new BlockPos(-256, 315, 0);
    private static final BlockPos TILE_ON_THE_CANONICAL_MIN = new BlockPos(255, 315, 0);

    private static CompoundTag serverTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(MultiblockSyncedTag.MIN_KEY, NbtUtils.writeBlockPos(MIN));
        tag.put(MultiblockSyncedTag.MAX_KEY, NbtUtils.writeBlockPos(MAX));
        tag.put(MultiblockSyncedTag.RENDER_LOCATION_KEY, NbtUtils.writeBlockPos(RENDER_LOCATION));
        tag.put(MultiblockSyncedTag.RENDER_Y_KEY, NbtUtils.writeBlockPos(RENDER_Y));
        tag.put(MultiblockSyncedTag.VALVE_KEY, positions(VALVE));
        tag.put(MultiblockSyncedTag.COILS_KEY, positions(COIL));
        tag.put(MultiblockSyncedTag.COMPLEX_KEY, NbtUtils.writeBlockPos(COMPLEX));
        tag.put(MultiblockSyncedTag.ASSEMBLIES_KEY, positions(ASSEMBLY));
        return tag;
    }

    private static ListTag positions(BlockPos pos) {
        CompoundTag entry = new CompoundTag();
        entry.put(MultiblockSyncedTag.POSITION_KEY, NbtUtils.writeBlockPos(pos));
        ListTag list = new ListTag();
        list.add(entry);
        return list;
    }

    private static BlockPos read(CompoundTag tag, String key) {
        return NbtUtils.readBlockPos(tag, key).orElseThrow();
    }

    private static BlockPos readFirst(CompoundTag tag, String listKey) {
        return read(tag.getList(listKey, Tag.TAG_COMPOUND).getCompound(0), MultiblockSyncedTag.POSITION_KEY);
    }

    @Test
    void aTilePastTheEdgeReadsEveryPositionInItsOwnCopy() {
        for (WorldFold fold : TRANSLATING) {
            CompoundTag seated = MultiblockSyncedTag.seat(fold, TILE_PAST_THE_EDGE, serverTag());

            assertEquals(fold.nearestCopy(TILE_PAST_THE_EDGE, MIN), read(seated, MultiblockSyncedTag.MIN_KEY),
                    "min in " + fold);
            assertEquals(fold.nearestCopy(TILE_PAST_THE_EDGE, MAX), read(seated, MultiblockSyncedTag.MAX_KEY),
                    "max in " + fold);
            assertEquals(fold.nearestCopy(TILE_PAST_THE_EDGE, RENDER_LOCATION),
                    read(seated, MultiblockSyncedTag.RENDER_LOCATION_KEY), "render location in " + fold);
            assertEquals(fold.nearestCopy(TILE_PAST_THE_EDGE, RENDER_Y),
                    read(seated, MultiblockSyncedTag.RENDER_Y_KEY), "render y in " + fold);
            assertEquals(fold.nearestCopy(TILE_PAST_THE_EDGE, VALVE),
                    readFirst(seated, MultiblockSyncedTag.VALVE_KEY), "valve in " + fold);
            assertEquals(fold.nearestCopy(TILE_PAST_THE_EDGE, COIL),
                    readFirst(seated, MultiblockSyncedTag.COILS_KEY), "coil in " + fold);
            assertEquals(fold.nearestCopy(TILE_PAST_THE_EDGE, COMPLEX), read(seated, MultiblockSyncedTag.COMPLEX_KEY),
                    "complex in " + fold);
            assertEquals(fold.nearestCopy(TILE_PAST_THE_EDGE, ASSEMBLY),
                    readFirst(seated, MultiblockSyncedTag.ASSEMBLIES_KEY), "assembly in " + fold);
            assertNotEquals(MIN, read(seated, MultiblockSyncedTag.MIN_KEY), "the rig moved nothing in " + fold);
        }
    }

    @Test
    void theSentTagIsLeftAsTheServerWroteIt() {
        for (WorldFold fold : TRANSLATING) {
            CompoundTag sent = serverTag();
            MultiblockSyncedTag.seat(fold, TILE_PAST_THE_EDGE, sent);

            assertEquals(serverTag(), sent, "in " + fold);
        }
    }

    @Test
    void aTileInTheServersCopyGetsTheTagBack() {
        for (WorldFold fold : TRANSLATING) {
            CompoundTag sent = serverTag();

            assertSame(sent, MultiblockSyncedTag.seat(fold, TILE_ON_THE_CANONICAL_MIN, sent), "in " + fold);
        }
    }

    @Test
    void anUnwrappedLevelGetsTheTagBack() {
        CompoundTag sent = serverTag();

        assertSame(sent, MultiblockSyncedTag.seat(WorldFolds.NOOP, TILE_PAST_THE_EDGE, sent));
    }

    @Test
    void aTagWithoutBoundsGetsTheTagBack() {
        CompoundTag sent = new CompoundTag();
        sent.put(MultiblockSyncedTag.RENDER_LOCATION_KEY, NbtUtils.writeBlockPos(RENDER_LOCATION));

        assertSame(sent, MultiblockSyncedTag.seat(PER_AXIS, TILE_PAST_THE_EDGE, sent));
    }
}
