package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;

class FoldedQuartTest {
    private static final int FACE_WIDTH_BLOCKS = 64;
    private static final int Y = 70;
    private static final int QUART_Y = Y >> 2;

    private static final int ZOOM_CENTER = 2;
    private static final int QUART_BITS = 2;
    private static final int QUART_MASK = 3;
    private static final double ZOOM = 4.0;

    private static final SeamTransform CLOCKWISE_OVER_THE_EAST_EDGE =
            new SeamTransform(-1, 1, FACE_WIDTH_BLOCKS, -FACE_WIDTH_BLOCKS, true);

    private static final WorldFold TURNED = (WorldFold) Proxy.newProxyInstance(
            WorldFold.class.getClassLoader(),
            new Class<?>[] {WorldFold.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "isPreservingLocalIndices" -> false;
                case "foldOriented" -> new WorldFold.Folded<>(
                        CLOCKWISE_OVER_THE_EAST_EDGE.apply((BlockPos) arguments[0]),
                        CLOCKWISE_OVER_THE_EAST_EDGE.getOrientation());
                case "foldBlockNode" ->
                        CLOCKWISE_OVER_THE_EAST_EDGE.apply(BlockPos.of((long) arguments[0])).asLong();
                default -> throw new UnsupportedOperationException(method.getName());
            });

    private static final WorldFold UPRIGHT = (WorldFold) Proxy.newProxyInstance(
            WorldFold.class.getClassLoader(),
            new Class<?>[] {WorldFold.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "isPreservingLocalIndices" -> true;
                default -> throw new UnsupportedOperationException(method.getName());
            });

    private record Corner(int quartX, int quartZ, double distanceX, double distanceZ) {
    }

    private static Set<Corner> vanillaCorners(int blockX, int blockZ) {
        int absX = blockX - ZOOM_CENTER;
        int absZ = blockZ - ZOOM_CENTER;
        int parentX = absX >> QUART_BITS;
        int parentZ = absZ >> QUART_BITS;
        double fractX = (absX & QUART_MASK) / ZOOM;
        double fractZ = (absZ & QUART_MASK) / ZOOM;
        Set<Corner> corners = new HashSet<>();
        for (int stepX = 0; stepX <= 1; stepX++) {
            for (int stepZ = 0; stepZ <= 1; stepZ++) {
                corners.add(new Corner(parentX + stepX, parentZ + stepZ, fractX - stepX, fractZ - stepZ));
            }
        }

        return corners;
    }

    private static Set<Corner> foldedCorners(int blockX, int blockZ) {
        Set<Corner> corners = new HashSet<>();
        for (Corner raw : vanillaCorners(blockX, blockZ)) {
            long folded = FoldedQuart.fold(TURNED, raw.quartX(), QUART_Y, raw.quartZ());
            FoldOrientation orientation = FoldedQuart.orientation(TURNED, raw.quartX(), QUART_Y, raw.quartZ());
            corners.add(new Corner(FoldedQuart.x(folded), FoldedQuart.z(folded),
                    FoldedQuart.distanceX(orientation, raw.distanceX(), raw.distanceZ()),
                    FoldedQuart.distanceZ(orientation, raw.distanceX(), raw.distanceZ())));
        }

        return corners;
    }

    @Test
    void aBlockPastATurnedEdgeZoomsOnTheCornersOfItsCopyAtTheSameDistances() {
        for (int blockX = FACE_WIDTH_BLOCKS; blockX < FACE_WIDTH_BLOCKS + 8; blockX++) {
            for (int blockZ = 5; blockZ < 13; blockZ++) {
                BlockPos copy = CLOCKWISE_OVER_THE_EAST_EDGE.apply(new BlockPos(blockX, Y, blockZ));
                assertEquals(vanillaCorners(copy.getX(), copy.getZ()), foldedCorners(blockX, blockZ),
                        "block " + blockX + ", " + blockZ);
            }
        }
    }

    @Test
    void aFoldThatPreservesLocalIndicesKeepsTheDistanceWithoutReadingTheFold() {
        FoldOrientation orientation = FoldedQuart.orientation(UPRIGHT, 3, QUART_Y, 5);

        assertSame(FoldOrientation.IDENTITY, orientation);
        assertEquals(0.25, FoldedQuart.distanceX(orientation, 0.25, -0.5));
        assertEquals(-0.5, FoldedQuart.distanceZ(orientation, 0.25, -0.5));
    }
}
