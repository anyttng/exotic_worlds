package com.exoticworlds.compat.apprenticecodex;

import static com.exoticworlds.compat.CompatFoldFixture.CYLINDER;
import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.SKEWED;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class BlockTargetFoldTest {
    private static final BlockPos INLAND_HIT = new BlockPos(40, 64, 20);
    private static final BlockPos LAST_BLOCK = new BlockPos(WORLD_BLOCKS / 2 - 1, 64, 20);

    @Test
    void aTargetPastTheBoundsMovesOneLapIntoThem() {
        assertEquals(onTop(INLAND_HIT), onTop(INLAND_HIT.east(WORLD_BLOCKS)).toServer(PER_AXIS));
    }

    @Test
    void placeAndHitLocationKeepTheirOffsetsOnEveryShape() {
        BlockPos hit = INLAND_HIT.offset(WORLD_BLOCKS + 7, 0, WORLD_BLOCKS + 9);
        for (WorldFold fold : List.of(PER_AXIS, CYLINDER, SKEWED)) {
            assertEquals(onTop(fold.fold(hit)), onTop(hit).toServer(fold), fold.toString());
        }
    }

    @Test
    void aHitOnTheFarFaceOfTheLastBlockKeepsThatBlockCanonical() {
        assertEquals(onEastFace(LAST_BLOCK), onEastFace(LAST_BLOCK.east(WORLD_BLOCKS)).toServer(PER_AXIS));
    }

    @Test
    void aTargetInsideTheBoundsComesBackAsTheSameObject() {
        BlockTarget inland = onTop(INLAND_HIT);
        assertSame(inland, inland.toServer(PER_AXIS));
        BlockTarget lastBlock = onEastFace(LAST_BLOCK);
        assertSame(lastBlock, lastBlock.toServer(PER_AXIS));
    }

    private static BlockTarget onTop(BlockPos hit) {
        return new BlockTarget(hit, Direction.UP, Vec3.upFromBottomCenterOf(hit, 1.0), hit.above(), Direction.DOWN);
    }

    private static BlockTarget onEastFace(BlockPos hit) {
        return new BlockTarget(hit, Direction.EAST, new Vec3(hit.getX() + 1.0, hit.getY() + 0.5, hit.getZ() + 0.5),
                hit.east(), Direction.WEST);
    }
}
