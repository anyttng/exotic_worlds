package com.exoticworlds.compat.eminus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eminus.store.SqliteCellStore;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

class EminusShapesTest {
    private static final int TINY_CHUNKS = 32;
    private static final int SMALL_CHUNKS = 64;
    private static final String MARKER_FILE = "exotic_worlds_shape";
    private static final byte[] PAGE = new byte[] {1, 2, 3, 4};

    @TempDir
    Path folder;

    @Test
    void aStoreLeftAsTheFoldedSessionClosedItStays() throws IOException {
        CellFold fold = torus(TINY_CHUNKS);
        writeStore();
        EminusShapes.stampStore(folder, fold);

        EminusShapes.matchStore(folder, fold);

        assertTrue(Files.exists(store()), "nothing wrote the store after the folded session closed it");
    }

    @Test
    void aStoreWrittenAfterTheFoldedSessionClosedItResets() throws IOException {
        CellFold fold = torus(TINY_CHUNKS);
        writeStore();
        EminusShapes.stampStore(folder, fold);
        Files.write(store(), PAGE, StandardOpenOption.APPEND);

        EminusShapes.matchStore(folder, fold);

        assertFalse(Files.exists(store()), "a session without the fold wrote unfolded keys into the store");
    }

    @Test
    void aMarkerHoldingTheShapeAloneResets() throws IOException {
        CellFold fold = torus(TINY_CHUNKS);
        writeStore();
        Files.writeString(folder.resolve(MARKER_FILE), fold.signature(), StandardCharsets.UTF_8);

        EminusShapes.matchStore(folder, fold);

        assertFalse(Files.exists(store()), "a marker without the files' stamp cannot vouch for what they hold");
    }

    @Test
    void aStoreStampedUnderAnotherShapeResets() throws IOException {
        writeStore();
        EminusShapes.stampStore(folder, torus(SMALL_CHUNKS));

        EminusShapes.matchStore(folder, torus(TINY_CHUNKS));

        assertFalse(Files.exists(store()), "the stored keys were folded by another lap");
    }

    private void writeStore() throws IOException {
        Files.write(store(), PAGE);
    }

    private Path store() {
        return folder.resolve(SqliteCellStore.FILE_NAME);
    }

    private static CellFold torus(int widthChunks) {
        return CellFold.of(WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidths(widthChunks, widthChunks))));
    }
}
