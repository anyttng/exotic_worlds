package com.exoticworlds.compat.eminus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.RecordComponent;

import org.junit.jupiter.api.Test;

import com.eminus.mesh.CellMesh;

class MeshKeysTest {
    private static final long KEY = 11L;
    private static final long COPY_KEY = 523L;
    private static final int OCCUPANCY = 3;

    @Test
    void aCopyChangesTheKeyAndKeepsEveryOtherComponent() throws ReflectiveOperationException {
        CellMesh mesh = new CellMesh(KEY, OCCUPANCY, new long[] {1L, 2L}, new int[] {0}, new int[] {2},
                new long[] {5L});

        CellMesh copy = MeshKeys.withKey(mesh, COPY_KEY);

        assertEquals(COPY_KEY, copy.key());
        for (RecordComponent component : CellMesh.class.getRecordComponents()) {
            if (component.getName().equals("key")) {
                continue;
            }

            Object before = component.getAccessor().invoke(mesh);
            Object after = component.getAccessor().invoke(copy);
            if (before.getClass().isArray()) {
                assertSame(before, after, component.getName());
            } else {
                assertEquals(before, after, component.getName());
            }
        }
    }
}
