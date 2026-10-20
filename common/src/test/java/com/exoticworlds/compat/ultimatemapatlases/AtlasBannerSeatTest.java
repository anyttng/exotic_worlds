package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;

class AtlasBannerSeatTest {
    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    @Test
    void aBannerSeatsOnTheCopyNearestThePoint() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(TINY, TINY)));
        assertEquals(new BlockPos(262, 70, 10), AtlasBannerSeat.toward(fold, 263, 11, new BlockPos(-250, 70, 10)),
                "a banner on the lower side sits a lap up beside a point past the upper seam");
        assertEquals(new BlockPos(-250, 70, 10), AtlasBannerSeat.toward(fold, -249, 11, new BlockPos(-250, 70, 10)),
                "a point beside the canonical banner keeps it");
        assertEquals(new BlockPos(10, 70, -262), AtlasBannerSeat.toward(fold, 11, -261, new BlockPos(10, 70, 250)),
                "Z seats the same way below the lower seam");
    }

    @Test
    void bothAxesSeatTogetherOnASkewedTorus() {
        WorldFold fold = new DeckGroupFold(FlatShape.latticeTorus(new WorldLoopBounds(TINY, TINY), 8),
                GenerationOptions.DEFAULT);
        BlockPos seated = AtlasBannerSeat.toward(fold, 0, 500, new BlockPos(0, 70, -10));
        assertEquals(502, seated.getZ(), "the banner a lap up in Z");
        assertEquals(128, Math.abs(seated.getX()), "the same seat moves X by the skew");
    }

    @Test
    void aCylinderLeavesItsUnboundedAxis() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.cylinder(new WorldLoopBounds(TINY, AxisBounds.Unbounded.INSTANCE)));
        assertEquals(new BlockPos(262, 70, 4000), AtlasBannerSeat.toward(fold, 263, -4000, new BlockPos(-250, 70, 4000)),
                "X seats a lap up while Z keeps the banner however far the point is");
    }

    @Test
    void anUnwrappedLevelKeepsTheBanner() {
        assertEquals(new BlockPos(-250, 70, 10), AtlasBannerSeat.toward((WorldFold) null, 263, 11, new BlockPos(-250, 70, 10)),
                "no fold, no seat");
    }
}
