package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

class BlockOffsetSeedTest {
    private static final int FACE_WIDTH_BLOCKS = 64;

    private static final SeamTransform CLOCKWISE_OVER_THE_EAST_EDGE =
            new SeamTransform(-1, 1, FACE_WIDTH_BLOCKS, -FACE_WIDTH_BLOCKS, true);

    private static final BlockPos PAST_THE_EDGE = new BlockPos(FACE_WIDTH_BLOCKS + 2, 70, 5);
    private static final Vec3 CANONICAL_OFFSET = new Vec3(0.2, 0.05, -0.1);
    private static final Vec3 OFFSET_AT_THE_COPY = new Vec3(-0.1, 0.05, -0.2);

    private static final WorldFold TURNED = (WorldFold) Proxy.newProxyInstance(
            WorldFold.class.getClassLoader(),
            new Class<?>[] {WorldFold.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "isPreservingLocalIndices" -> false;
                case "foldOriented" -> new WorldFold.Folded<>(
                        CLOCKWISE_OVER_THE_EAST_EDGE.apply((BlockPos) arguments[0]),
                        CLOCKWISE_OVER_THE_EAST_EDGE.getOrientation());
                default -> throw new UnsupportedOperationException(method.getName());
            });

    private static final WorldFold UPRIGHT = (WorldFold) Proxy.newProxyInstance(
            WorldFold.class.getClassLoader(),
            new Class<?>[] {WorldFold.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "isPreservingLocalIndices" -> true;
                default -> throw new UnsupportedOperationException(method.getName());
            });

    @Test
    void theOffsetAtATurnedCopyIsTheCanonicalOffsetTurnedIntoTheCopysFrame() {
        assertEquals(OFFSET_AT_THE_COPY, BlockOffsetSeed.inCopyFrame(TURNED, PAST_THE_EDGE, CANONICAL_OFFSET));
    }

    @Test
    void aFoldThatPreservesLocalIndicesHandsBackTheOffsetItself() {
        assertSame(CANONICAL_OFFSET, BlockOffsetSeed.inCopyFrame(UPRIGHT, PAST_THE_EDGE, CANONICAL_OFFSET));
    }
}
