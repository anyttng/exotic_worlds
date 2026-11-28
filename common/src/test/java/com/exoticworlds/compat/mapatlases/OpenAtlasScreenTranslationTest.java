package com.exoticworlds.compat.mapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.engine.net.TranslationContext;
import com.exoticworlds.engine.seam.ClientPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

class OpenAtlasScreenTranslationTest {
    private static final int WIDTH_IN_CHUNKS = 32;
    private static final int WIDTH_BLOCKS = WIDTH_IN_CHUNKS * 16;
    private static final int VIEW_DISTANCE_CHUNKS = 12;

    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));

    private static final double CLIENT_X = 240.0;
    private static final BlockPos LECTERN_ACROSS_THE_SEAM = new BlockPos(-250, 64, 10);
    private static final BlockPos LECTERN_BESIDE_THE_CLIENT = new BlockPos(230, 64, 10);

    private static TranslationContext context() {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(CLIENT_X, 0.0, Level.OVERWORLD, TORUS);
        return new TranslationContext(
                TORUS,
                clientPosition,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD,
                VIEW_DISTANCE_CHUNKS,
                VIEW_DISTANCE_CHUNKS,
                entityId -> null,
                entityId -> null,
                () -> {
                });
    }

    @Test
    void aLecternAcrossTheSeamMovesToTheClientsCopy() {
        assertEquals(Optional.of(LECTERN_ACROSS_THE_SEAM.offset(WIDTH_BLOCKS, 0, 0)),
                OpenAtlasScreenTranslation.movedLectern(Optional.of(LECTERN_ACROSS_THE_SEAM), context()));
    }

    @Test
    void aLecternAlreadyInTheClientFrameDoesNotMove() {
        assertEquals(Optional.empty(),
                OpenAtlasScreenTranslation.movedLectern(Optional.of(LECTERN_BESIDE_THE_CLIENT), context()));
    }

    @Test
    void anOpenScreenWithNoLecternDoesNotMove() {
        assertEquals(Optional.empty(), OpenAtlasScreenTranslation.movedLectern(Optional.empty(), context()));
    }
}
