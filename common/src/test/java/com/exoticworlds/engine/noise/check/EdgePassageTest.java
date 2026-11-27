package com.exoticworlds.engine.noise.check;

import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.atlases;
import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.cuboidFold;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;

import net.minecraft.world.level.ChunkPos;

class EdgePassageTest {
    private static final int GLUED_EDGES = 7;

    @Test
    void aCuboidHasAPassagePerGluedEdge() {
        for (CuboidAtlas atlas : atlases()) {
            assertEquals(GLUED_EDGES, SeamPassage.of(cuboidFold(atlas)).size());
        }
    }

    @Test
    void aSampleLiesOffTheNetAndItsPairOnTheFaceAcrossTheEdge() {
        for (CuboidAtlas atlas : atlases()) {
            for (EdgePassage passage : EdgePassage.of(cuboidFold(atlas), atlas)) {
                for (SeamCauseSearch.Point sample : passage.samples()) {
                    SeamCauseSearch.Point away = passage.away(sample);
                    CuboidFace across = atlas.faceAt(away.x(), away.z());

                    assertNull(atlas.faceAt(sample.x(), sample.z()), sample + " past " + passage.edge());
                    assertNotNull(across, away + " across " + passage.edge());
                    assertEquals(passage.edge().second(), across.side());
                    assertEquals(sample.y(), away.y());
                }
            }
        }
    }

    @Test
    void eachSideIsReadFromAChunkOfItsOwnFace() {
        for (CuboidAtlas atlas : atlases()) {
            WorldFold fold = cuboidFold(atlas);
            for (EdgePassage passage : EdgePassage.of(fold, atlas)) {
                for (SeamCauseSearch.Point sample : passage.samples()) {
                    assertEquals(passage.edge().first(), askingSide(atlas, passage, sample));
                    assertEquals(passage.edge().second(), askingSide(atlas, passage, passage.away(sample)));
                }
            }
        }
    }

    private static CuboidFace.Side askingSide(CuboidAtlas atlas, EdgePassage passage, SeamCauseSearch.Point at) {
        ChunkPos asking = passage.reading(at, () -> GenerationFoldContext.getContext().getAskingChunk());
        assertNotNull(asking);
        CuboidFace face = atlas.faceAt(asking.getMinBlockX(), asking.getMinBlockZ());
        assertNotNull(face, asking + " asks for " + at);
        return face.side();
    }
}
