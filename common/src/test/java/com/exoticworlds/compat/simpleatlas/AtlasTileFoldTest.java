package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.world.phys.Vec3;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

class AtlasTileFoldTest {
    private static final String OVERWORLD = "minecraft:overworld";

    private static final int BLOCKS_PER_TILE = 128;

    private static final ToroidalShape TORUS = MapShapes.torus(-8, 8);

    private static final ToroidalShape LATTICE = MapShapes.latticeTorus(-8, 8, 4);

    @Test
    void aPointPastTheZSeamIsSeatedOnTheSkewedCopyNearestTheTile() {
        assertEquals(new Vec3(8.0, 0.0, 136.0), AtlasTileFold.nearestToTile(LATTICE, tile(0, 112), -56.0, -120.0));
    }

    @Test
    void aClickPastTheZSeamFoldsByTheSkewToo() {
        assertEquals(new Vec3(-56.0, 0.0, -120.0), AtlasTileFold.foldOnTile(LATTICE, 8.0, 136.0));
    }

    @Test
    void aTileAcrossTheXSeamDrawsOneColumn() {
        assertEquals(Set.of(new AtlasTileFold.SeamLine(true, 64, 0, 128)),
                Set.copyOf(AtlasTileFold.seamLines(TORUS, tile(128, 0), BLOCKS_PER_TILE)));
    }

    @Test
    void theXSeamStopsAtTheZSeamWhereTheSkewMovesIt() {
        assertEquals(Set.of(new AtlasTileFold.SeamLine(true, 64, 0, 64), new AtlasTileFold.SeamLine(false, 64, 0, 128)),
                Set.copyOf(AtlasTileFold.seamLines(LATTICE, tile(128, 128), BLOCKS_PER_TILE)));
    }

    @Test
    void aDimensionThatDoesNotWrapDrawsNoSeam() {
        assertEquals(0, AtlasTileFold.seamLines(null, tile(128, 0), BLOCKS_PER_TILE).size());
    }

    private static AtlasTilePayload tile(int centerX, int centerZ) {
        return new AtlasTilePayload(1, centerX, centerZ, 0, 0, OVERWORLD);
    }
}
