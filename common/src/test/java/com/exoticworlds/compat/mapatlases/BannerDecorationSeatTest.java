package com.exoticworlds.compat.mapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;

class BannerDecorationSeatTest {
    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(TINY, TINY)));

    @Test
    void aBannerAcrossTheEdgeSeatsOnTheCopyNearestTheCenter() {
        assertEquals(new BlockPos(262, 70, 10),
                BannerDecorationSeat.towardMapCenter(TORUS, 240, 8, new BlockPos(-250, 70, 10)),
                "a banner beside the lower bound sits a lap up beside a center near the upper bound");
        assertEquals(new BlockPos(10, 70, -262),
                BannerDecorationSeat.towardMapCenter(TORUS, 8, -240, new BlockPos(10, 70, 250)),
                "Z seats the same way below the lower bound");
    }

    @Test
    void aBannerOnTheCentersSideIsKept() {
        BlockPos banner = new BlockPos(-250, 70, 10);
        assertSame(banner, BannerDecorationSeat.towardMapCenter(TORUS, -200, 0, banner),
                "a banner beside the center is handed back as it was");
    }

    @Test
    void theSeatMeasuresFromTheBlockCenterAsToggleBannerDoes() {
        BlockPos inside = new BlockPos(-246, 70, 0);
        assertSame(inside, BannerDecorationSeat.towardMapCenter(TORUS, 10, 0, inside),
                "a block center 255.5 blocks from the map center stays");
        assertEquals(new BlockPos(265, 70, 0),
                BannerDecorationSeat.towardMapCenter(TORUS, 10, 0, new BlockPos(-247, 70, 0)),
                "a block center 256.5 blocks away moves a lap");
    }

    @Test
    void bothAxesSeatTogetherOnASkewedTorus() {
        WorldFold fold = new DeckGroupFold(FlatShape.latticeTorus(new WorldLoopBounds(TINY, TINY), 8),
                ShapeOptions.DEFAULT);
        BlockPos seated = BannerDecorationSeat.towardMapCenter(fold, 0, 240, new BlockPos(0, 70, -250));
        assertEquals(262, seated.getZ(), "the banner a lap up in Z");
        assertEquals(128, Math.abs(seated.getX()), "the same seat moves X by the skew");
    }

    @Test
    void aCylinderLeavesItsUnboundedAxis() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.cylinder(new WorldLoopBounds(TINY, AxisBounds.Unbounded.INSTANCE)));
        assertEquals(new BlockPos(262, 70, 4000),
                BannerDecorationSeat.towardMapCenter(fold, 240, 4000, new BlockPos(-250, 70, 4000)),
                "X seats a lap up while Z keeps the banner");
    }

    @Test
    void anUnwrappedLevelKeepsTheBanner() {
        BlockPos banner = new BlockPos(-250, 70, 10);
        assertSame(banner, BannerDecorationSeat.towardMapCenter(null, 240, 8, banner), "no fold, no seat");
    }
}
