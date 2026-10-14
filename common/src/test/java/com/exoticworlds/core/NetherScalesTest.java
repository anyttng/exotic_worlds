package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NetherScalesTest {
    private static final List<Integer> WIDTHS = List.of(16, 17, 20, 32, 64, 81, 128, 160, 1009, 4096);

    private static List<Integer> allowedNaive(int overworldChunkWidth) {
        List<Integer> allowed = new ArrayList<>();
        for (int scale = 1; scale <= overworldChunkWidth; scale++) {
            if (overworldChunkWidth % scale == 0 && overworldChunkWidth / scale >= WorldLoopSizes.MIN_CHUNK_WIDTH) {
                allowed.add(scale);
            }
        }
        return allowed;
    }

    private static List<Integer> allowedFor(int chunkWidth) {
        return NetherScales.allowedFor(chunkWidth, chunkWidth, FlatShape.NO_SKEW);
    }

    private static int normalize(int scale, int chunkWidth) {
        return NetherScales.normalize(scale, chunkWidth, chunkWidth, FlatShape.NO_SKEW);
    }

    private static int next(int scale, int chunkWidth) {
        return NetherScales.next(scale, chunkWidth, chunkWidth, FlatShape.NO_SKEW);
    }

    @Nested
    class AllowedScales {
        @Test
        void matchTheExhaustiveReferenceOnEveryWidthShape() {
            for (int width : WIDTHS) {
                assertEquals(allowedNaive(width), allowedFor(width), () -> "width " + width);
            }
        }

        @Test
        void everyAllowedScaleDividesTheWidthAndLeavesAWorkingNether() {
            for (int width : WIDTHS) {
                for (int scale : allowedFor(width)) {
                    assertEquals(0, width % scale, "width " + width + " scale " + scale);
                    assertTrue(NetherScales.netherChunkWidth(width, scale) >= WorldLoopSizes.MIN_CHUNK_WIDTH,
                            "width " + width + " scale " + scale + " leaves too narrow a nether");
                    assertEquals(width, NetherScales.netherChunkWidth(width, scale) * scale,
                            "width " + width + " scale " + scale + " does not divide evenly");
                }
            }
        }

        @Test
        void theSmallestWorldAdmitsOnlyOneToOne() {
            assertEquals(List.of(1), allowedFor(WorldLoopSizes.MIN_CHUNK_WIDTH));
            assertEquals(List.of(1), allowedFor(WorldLoopSizes.MIN_CHUNK_WIDTH - 2));
        }

        @Test
        void vanillasEightToOneNeedsAWorldEightMinimumsWide() {
            assertTrue(allowedFor(8 * WorldLoopSizes.MIN_CHUNK_WIDTH).contains(NetherScales.DEFAULT));
            assertFalse(allowedFor(8 * WorldLoopSizes.MIN_CHUNK_WIDTH - 16).contains(NetherScales.DEFAULT));
        }
    }

    @Nested
    class Normalization {
        @Test
        void returnsTheLargestAllowedScaleNotAboveAndBothOverloadsAgree() {
            for (int width : WIDTHS) {
                List<Integer> allowed = allowedFor(width);
                for (int scale = 0; scale <= 300; scale++) {
                    int normalized = normalize(scale, width);
                    assertTrue(allowed.contains(normalized),
                            "width " + width + ": normalize(" + scale + ") = " + normalized + " is not allowed");
                    for (int candidate : allowed) {
                        assertTrue(candidate > scale || candidate <= normalized,
                                "width " + width + ": normalize(" + scale + ") = " + normalized
                                        + " skipped the closer-from-below " + candidate);
                    }
                    assertTrue(normalized <= scale || normalized == allowed.get(0),
                            "width " + width + ": normalize(" + scale + ") = " + normalized + " fell upward");
                    assertEquals(normalized, NetherScales.normalize(scale, allowed),
                            "width " + width + ": the list overload disagrees on " + scale);
                }
            }
        }

        @Test
        void aMemberNormalizesToItself() {
            for (int width : WIDTHS) {
                for (int scale : allowedFor(width)) {
                    assertEquals(scale, normalize(scale, width), "width " + width);
                }
            }
        }

        @Test
        void theDefaultWinsWheneverTheWidthAdmitsIt() {
            assertEquals(8, normalize(NetherScales.DEFAULT, 128));
            assertEquals(8, normalize(NetherScales.DEFAULT, 4096));
        }

        @Test
        void theDefaultFallsDownTheChainWhenDisallowed() {
            assertEquals(2, normalize(NetherScales.DEFAULT, 32));
            assertEquals(1, normalize(NetherScales.DEFAULT, 16));
            assertEquals(3, normalize(NetherScales.DEFAULT, 129));
        }

        @Test
        void aDisallowedScaleFallsToTheLargestAllowedBelowIt() {
            assertEquals(2, normalize(3, 128));
            assertEquals(1, normalize(0, 128));
            assertEquals(8, normalize(300, 128));
        }
    }

    @Nested
    class TwoWidths {
        @Test
        void matchTheExhaustiveReferenceOfBothAxes() {
            for (int xWidth : WIDTHS) {
                for (int zWidth : WIDTHS) {
                    List<Integer> expected = new ArrayList<>(allowedNaive(xWidth));
                    expected.retainAll(allowedNaive(zWidth));
                    assertEquals(expected, NetherScales.allowedFor(xWidth, zWidth, FlatShape.NO_SKEW),
                            () -> "widths " + xWidth + " x " + zWidth);
                    assertEquals(expected, NetherScales.allowedFor(zWidth, xWidth, FlatShape.NO_SKEW),
                            () -> "widths " + zWidth + " x " + xWidth);
                }
            }
        }

        @Test
        void theNarrowerAxisCapsTheScale() {
            assertEquals(List.of(1, 2), NetherScales.allowedFor(32, 1024, FlatShape.NO_SKEW));
            assertEquals(2, NetherScales.normalize(NetherScales.DEFAULT, 32, 1024, FlatShape.NO_SKEW));
        }

        @Test
        void aScaleMustDivideBothWidths() {
            assertEquals(List.of(1, 2, 4), NetherScales.allowedFor(128, 100, FlatShape.NO_SKEW));
            assertEquals(4, NetherScales.normalize(NetherScales.DEFAULT, 128, 100, FlatShape.NO_SKEW));
            assertEquals(1, NetherScales.next(4, 128, 100, FlatShape.NO_SKEW));
        }
    }

    @Nested
    class Skew {
        @Test
        void everyAllowedScaleDividesTheSkewToo() {
            for (int skew = -64; skew <= 64; skew++) {
                for (int scale : NetherScales.allowedFor(256, 128, skew)) {
                    assertEquals(0, skew % scale, "skew " + skew + " scale " + scale);
                }
            }
        }

        @Test
        void theSkewNarrowsTheScalesTheWidthsAllow() {
            assertEquals(List.of(1, 2, 4), NetherScales.allowedFor(256, 256, 4));
            assertEquals(List.of(1), NetherScales.allowedFor(256, 256, 5));
            assertEquals(List.of(1, 2), NetherScales.allowedFor(256, 256, -6));
            assertEquals(4, NetherScales.normalize(NetherScales.DEFAULT, 256, 256, 4));
            assertEquals(1, NetherScales.next(4, 256, 256, 4));
        }
    }

    @Nested
    class Cycling {
        @Test
        void nextWalksTheWholeListInOrderAndWrapsAround() {
            for (int width : WIDTHS) {
                List<Integer> allowed = allowedFor(width);
                for (int index = 0; index < allowed.size(); index++) {
                    assertEquals(allowed.get((index + 1) % allowed.size()),
                            next(allowed.get(index), width),
                            "width " + width + " from " + allowed.get(index));
                }
            }
        }

        @Test
        void anUnknownScaleAdvancesToTheFirstAllowed() {
            assertEquals(1, next(7, 128));
            assertEquals(1, next(-3, 128));
        }
    }
}
