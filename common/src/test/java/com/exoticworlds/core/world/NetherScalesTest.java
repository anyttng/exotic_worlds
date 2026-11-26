package com.exoticworlds.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NetherScalesTest {
    private static final List<Integer> WIDTHS_CHUNKS = List.of(16, 17, 20, 32, 64, 81, 128, 160, 1009, 4096);

    private static List<Integer> allowedNaive(int overworldWidthChunks) {
        List<Integer> allowed = new ArrayList<>();
        for (int scale = 1; scale <= overworldWidthChunks; scale++) {
            if (overworldWidthChunks % scale == 0 && overworldWidthChunks / scale >= WorldLoopSizes.MIN_WIDTH_CHUNKS) {
                allowed.add(scale);
            }
        }
        return allowed;
    }

    private static List<Integer> allowedFor(int widthChunks) {
        return NetherScales.allowedFor(widthChunks, widthChunks, FlatShape.NO_SKEW);
    }

    private static int normalize(int scale, int widthChunks) {
        return NetherScales.normalize(scale, allowedFor(widthChunks));
    }

    @Nested
    class AllowedScales {
        @Test
        void matchTheExhaustiveReferenceOnEveryWidthShape() {
            for (int widthChunks : WIDTHS_CHUNKS) {
                assertEquals(allowedNaive(widthChunks), allowedFor(widthChunks), () -> "width " + widthChunks);
            }
        }

        @Test
        void everyAllowedScaleDividesTheWidthAndLeavesAWorkingNether() {
            for (int widthChunks : WIDTHS_CHUNKS) {
                for (int scale : allowedFor(widthChunks)) {
                    assertEquals(0, widthChunks % scale, "width " + widthChunks + " scale " + scale);
                    assertTrue(widthChunks / scale >= WorldLoopSizes.MIN_WIDTH_CHUNKS,
                            "width " + widthChunks + " scale " + scale + " leaves too narrow a nether");
                }
            }
        }

        @Test
        void theSmallestWorldAdmitsOnlyOneToOne() {
            assertEquals(List.of(1), allowedFor(WorldLoopSizes.MIN_WIDTH_CHUNKS));
            assertEquals(List.of(1), allowedFor(WorldLoopSizes.MIN_WIDTH_CHUNKS - 2));
        }

        @Test
        void vanillasEightToOneNeedsAWorldEightMinimumsWide() {
            assertTrue(allowedFor(8 * WorldLoopSizes.MIN_WIDTH_CHUNKS).contains(NetherScales.DEFAULT));
            assertFalse(allowedFor(8 * WorldLoopSizes.MIN_WIDTH_CHUNKS - 16).contains(NetherScales.DEFAULT));
        }
    }

    @Nested
    class Normalization {
        @Test
        void returnsTheLargestAllowedScaleNotAboveAndBothOverloadsAgree() {
            for (int widthChunks : WIDTHS_CHUNKS) {
                List<Integer> allowed = allowedFor(widthChunks);
                for (int scale = 0; scale <= 300; scale++) {
                    int normalized = normalize(scale, widthChunks);
                    assertTrue(allowed.contains(normalized),
                            "width " + widthChunks + ": normalize(" + scale + ") = " + normalized + " is not allowed");
                    for (int candidate : allowed) {
                        assertTrue(candidate > scale || candidate <= normalized,
                                "width " + widthChunks + ": normalize(" + scale + ") = " + normalized
                                        + " skipped the closer-from-below " + candidate);
                    }
                    assertTrue(normalized <= scale || normalized == allowed.get(0),
                            "width " + widthChunks + ": normalize(" + scale + ") = " + normalized + " fell upward");
                    assertEquals(normalized, NetherScales.normalize(scale, allowed),
                            "width " + widthChunks + ": the list overload disagrees on " + scale);
                }
            }
        }

        @Test
        void aMemberNormalizesToItself() {
            for (int widthChunks : WIDTHS_CHUNKS) {
                for (int scale : allowedFor(widthChunks)) {
                    assertEquals(scale, normalize(scale, widthChunks), "width " + widthChunks);
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
            for (int xWidthChunks : WIDTHS_CHUNKS) {
                for (int zWidthChunks : WIDTHS_CHUNKS) {
                    List<Integer> expected = new ArrayList<>(allowedNaive(xWidthChunks));
                    expected.retainAll(allowedNaive(zWidthChunks));
                    assertEquals(expected, NetherScales.allowedFor(xWidthChunks, zWidthChunks, FlatShape.NO_SKEW),
                            () -> "widths " + xWidthChunks + " x " + zWidthChunks);
                    assertEquals(expected, NetherScales.allowedFor(zWidthChunks, xWidthChunks, FlatShape.NO_SKEW),
                            () -> "widths " + zWidthChunks + " x " + xWidthChunks);
                }
            }
        }

        @Test
        void theNarrowerAxisCapsTheScale() {
            assertEquals(List.of(1, 2), NetherScales.allowedFor(32, 1024, FlatShape.NO_SKEW));
            assertEquals(2, NetherScales.normalize(NetherScales.DEFAULT,
                    NetherScales.allowedFor(32, 1024, FlatShape.NO_SKEW)));
        }

        @Test
        void aScaleMustDivideBothWidths() {
            assertEquals(List.of(1, 2, 4), NetherScales.allowedFor(128, 100, FlatShape.NO_SKEW));
            assertEquals(4, NetherScales.normalize(NetherScales.DEFAULT,
                    NetherScales.allowedFor(128, 100, FlatShape.NO_SKEW)));
        }
    }

    @Nested
    class Skew {
        @Test
        void everyAllowedScaleDividesTheSkewToo() {
            for (int skewChunks = -64; skewChunks <= 64; skewChunks++) {
                for (int scale : NetherScales.allowedFor(256, 128, skewChunks)) {
                    assertEquals(0, skewChunks % scale, "skew " + skewChunks + " scale " + scale);
                }
            }
        }

        @Test
        void theSkewNarrowsTheScalesTheWidthsAllow() {
            assertEquals(List.of(1, 2, 4), NetherScales.allowedFor(256, 256, 4));
            assertEquals(List.of(1), NetherScales.allowedFor(256, 256, 5));
            assertEquals(List.of(1, 2), NetherScales.allowedFor(256, 256, -6));
            assertEquals(4, NetherScales.normalize(NetherScales.DEFAULT, NetherScales.allowedFor(256, 256, 4)));
        }
    }
}
