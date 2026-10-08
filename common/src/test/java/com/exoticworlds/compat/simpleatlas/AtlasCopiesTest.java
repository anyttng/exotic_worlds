package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class AtlasCopiesTest {
    private static final int BLOCKS_PER_TILE = 128;

    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    private static final AxisBounds.Looped UNEVEN = new AxisBounds.Looped(-17, 17);

    private static final float PERIOD = 40.0F;

    private static final float BASE = 40.0F;

    private static ToroidalShape shape(AxisBounds x, AxisBounds z) {
        return TestShapes.of(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(x, z))));
    }

    @Test
    void aTorusRepeatsEveryWorldWidthInTiles() {
        ToroidalShape torus = shape(TINY, TINY);

        assertEquals(4, AtlasCopies.lapTiles(torus, Direction.Axis.X, BLOCKS_PER_TILE));
        assertEquals(4, AtlasCopies.lapTiles(torus, Direction.Axis.Z, BLOCKS_PER_TILE));
    }

    @Test
    void aCylinderRepeatsOnItsLoopingAxisOnly() {
        ToroidalShape cylinder = shape(TINY, AxisBounds.Unbounded.INSTANCE);

        assertEquals(4, AtlasCopies.lapTiles(cylinder, Direction.Axis.X, BLOCKS_PER_TILE));
        assertEquals(0, AtlasCopies.lapTiles(cylinder, Direction.Axis.Z, BLOCKS_PER_TILE));
    }

    @Test
    void aWidthThatIsNotAWholeNumberOfMapsDrawsNoCopies() {
        assertEquals(0, AtlasCopies.lapTiles(shape(UNEVEN, UNEVEN), Direction.Axis.X, BLOCKS_PER_TILE));
        assertEquals(0, AtlasCopies.lapTiles(null, Direction.Axis.X, BLOCKS_PER_TILE));
    }

    @Test
    void anAreaAsWideAsTheBaseShowsTheBaseAlone() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(PERIOD, PERIOD, 0.0F, 0.0F, BASE, BASE,
                new AtlasView(0.0F, 0.0F, 40.0F, 40.0F));

        assertEquals(List.of(new AtlasCopies.Offset(0.0F, 0.0F)), offsets);
    }

    @Test
    void anAreaAcrossTwoLapsShowsTwoCopies() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(PERIOD, 0.0F, 0.0F, 0.0F, BASE, BASE,
                new AtlasView(20.0F, 0.0F, 40.0F, 40.0F));

        assertEquals(List.of(new AtlasCopies.Offset(0.0F, 0.0F), new AtlasCopies.Offset(40.0F, 0.0F)), offsets);
    }

    @Test
    void aWideAreaFillsWithCopiesOnBothAxes() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(PERIOD, PERIOD, 0.0F, 0.0F, BASE, BASE,
                new AtlasView(-50.0F, -50.0F, 150.0F, 150.0F));

        assertEquals(25, offsets.size());
        assertEquals(new AtlasCopies.Offset(-80.0F, -80.0F), offsets.getFirst());
        assertEquals(new AtlasCopies.Offset(80.0F, 80.0F), offsets.getLast());
    }

    @Test
    void aSquareStartingHalfATileBeforeItsFirstMapTakesTheCopyALapLeft() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(240.0F, 0.0F, 5.0F, 0.0F, 240.0F, 10.0F,
                new AtlasView(0.0F, 0.0F, 240.0F, 10.0F));

        assertEquals(List.of(new AtlasCopies.Offset(-240.0F, 0.0F), new AtlasCopies.Offset(0.0F, 0.0F)), offsets);
    }

    @Test
    void aPointerOverACopyLandsOnTheSameSpotOfTheBase() {
        assertEquals(15.0, AtlasCopies.ontoBase(95.0, PERIOD, 0.0F, BASE));
        assertEquals(35.0, AtlasCopies.ontoBase(-5.0, PERIOD, 0.0F, BASE));
        assertEquals(20.0, AtlasCopies.ontoBase(20.0, PERIOD, 0.0F, BASE));
        assertEquals(95.0, AtlasCopies.ontoBase(95.0, 0.0F, 0.0F, BASE));
    }

    @Test
    void aPointerOffTheBaseAndOffEveryCopyStaysWhereItIs() {
        assertEquals(30.0, AtlasCopies.ontoBase(30.0, 100.0F, 0.0F, 20.0F));
    }
}
