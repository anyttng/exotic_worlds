package com.exoticworlds.compat.ultraterraforged.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.SeamLine;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class PreviewViewTest {
    private static final ToroidalShape TORUS = MapShapes.torus(-16, 16);
    private static final ToroidalShape CYLINDER = MapShapes.cylinder(-16, 16);
    private static final ToroidalShape SKEWED = MapShapes.latticeTorus(-16, 16, 4);
    private static final int WIDGET_PIXELS = 200;

    @Test
    void repeatedCopiesKeepTheWorldAtLeast64PixelsWide() {
        assertEquals(6.25f, PreviewView.zoomCeiling(TORUS, MapCopies.REPEATED, WIDGET_PIXELS), 1e-6f,
                "512 blocks over 64 of 200 widget pixels showing 256 preview pixels: 512 * 200 / (256 * 64)");
        assertEquals(Float.POSITIVE_INFINITY, PreviewView.zoomCeiling(TestShapes.of(WorldFolds.NOOP),
                MapCopies.REPEATED, WIDGET_PIXELS), "a world that loops on no axis caps nothing");
    }

    @Test
    void aSingleCopyCoversThePreview() {
        assertEquals(2f, PreviewView.zoomCeiling(TORUS, MapCopies.SINGLE, WIDGET_PIXELS), 1e-6f,
                "512 blocks over 256 preview pixels");
        assertEquals(2f, PreviewView.zoomCeiling(CYLINDER, MapCopies.SINGLE, WIDGET_PIXELS), 1e-6f,
                "a cylinder caps by its looping axis alone");
    }

    @Test
    void aSingleCopyHoldsTheCenterInsideTheWorld() {
        assertEquals(256f - 128f, PreviewView.heldCenter(TORUS, MapCopies.SINGLE, Direction.Axis.X, 900f, 1f), 1e-6f,
                "half the 256-block window stays inside the edge at 256");
        assertEquals(-256f + 64f, PreviewView.heldCenter(TORUS, MapCopies.SINGLE, Direction.Axis.Z, -900f, 0.5f),
                1e-6f, "half the 128-block window stays inside the edge at -256");
        assertEquals(900f, PreviewView.heldCenter(TORUS, MapCopies.REPEATED, Direction.Axis.X, 900f, 1f), 1e-6f,
                "repeated copies hold nothing");
        assertEquals(-900f, PreviewView.heldCenter(CYLINDER, MapCopies.SINGLE, Direction.Axis.Z, -900f, 1f), 1e-6f,
                "the cylinder's endless axis holds nothing");
    }

    @Test
    void theEdgesLandWhereThePictureDrawsThem() {
        PreviewView view = new PreviewView(TORUS, MapCopies.REPEATED, 256f, 0f, 1f);
        assertEquals(List.of(new SeamLine(true, 138, 20, 276)), view.seamLines(10, 20, 256),
                "a 256-block window centered on the X edge at 256 meets it in the middle of a 256-pixel widget");
    }

    @Test
    void aCylinderDrawsItsLoopingAxisAlone() {
        PreviewView view = new PreviewView(CYLINDER, MapCopies.REPEATED, 256f, 256f, 1f);
        assertEquals(List.of(new SeamLine(true, 128, 0, 256)), view.seamLines(0, 0, 256),
                "the endless Z axis draws no line");
    }

    @Test
    void aSkewedEdgeBreaksIntoSegments() {
        PreviewView view = new PreviewView(SKEWED, MapCopies.REPEATED, 256f, 256f, 1f);
        List<SeamLine> lines = view.seamLines(0, 0, 256);
        assertEquals(2, lines.stream().filter(line -> !line.vertical()).count(),
                "the Z edge splits where the X edge of the moved row meets it");
        assertEquals(2, lines.stream().filter(SeamLine::vertical).map(SeamLine::at).distinct().count(),
                "the X edge past the Z edge is moved by the 64-block skew");
    }

    @Test
    void theReadoutNamesThePositionInsideTheWorld() {
        PreviewView view = new PreviewView(TORUS, MapCopies.REPEATED, 0f, 0f, 1f);
        assertEquals(new BlockPos(-212, 0, 40), view.inside(300, 40), "300 is one 512-block lap past -212");
        PreviewView skewed = new PreviewView(SKEWED, MapCopies.REPEATED, 0f, 0f, 1f);
        assertEquals(new BlockPos(-64, 0, -212), skewed.inside(0, 300),
                "crossing the Z edge moves X back by the 64-block skew");
    }

    @Test
    void aClickReadsTheBlockTheReadoutNames() {
        PreviewView view = new PreviewView(TORUS, MapCopies.REPEATED, 0f, 0f, 1f);
        assertEquals(new BlockPos(0, 0, -64), view.blockAt(10, 20, 256, 138.5, 84.0),
                "preview pixel 128 is the center column, pixel 64 lies 64 blocks above it");
        assertEquals(new BlockPos(0, 0, -64), view.blockAt(10, 20, 128, 74.0, 52.0),
                "a 128-pixel widget shows the same 256 preview pixels at half the scale");
    }

    @Test
    void theMarkerLandsOnEveryCopyInsideThePicture() {
        PreviewView view = new PreviewView(TORUS, MapCopies.REPEATED, 256f, 100f, 4f);
        assertEquals(Set.of(new PreviewView.Pixel(64, 103), new PreviewView.Pixel(192, 103),
                        new PreviewView.Pixel(64, 231), new PreviewView.Pixel(192, 231)),
                Set.copyOf(view.copiesOf(BlockPos.ZERO, 0, 0, 256)),
                "a 1024-block window from -256 to 768 and -412 to 612 holds the origin at 0 and one 512-block lap on");
    }

    @Test
    void aCylinderMarksItsEndlessAxisOnce() {
        PreviewView view = new PreviewView(CYLINDER, MapCopies.REPEATED, 256f, 100f, 4f);
        assertEquals(Set.of(new PreviewView.Pixel(64, 103), new PreviewView.Pixel(192, 103)),
                Set.copyOf(view.copiesOf(BlockPos.ZERO, 0, 0, 256)), "Z never repeats on a cylinder");
    }
}
