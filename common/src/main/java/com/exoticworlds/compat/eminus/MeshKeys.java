package com.exoticworlds.compat.eminus;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import com.eminus.mesh.CellMesh;

public final class MeshKeys {
    private static final String KEY_COMPONENT = "key";
    private static final RecordComponent[] COMPONENTS = CellMesh.class.getRecordComponents();
    private static final Constructor<CellMesh> CANONICAL = canonical();

    public static CellMesh withKey(CellMesh mesh, long key) {
        Object[] values = new Object[COMPONENTS.length];
        try {
            for (int index = 0; index < COMPONENTS.length; index++) {
                values[index] = COMPONENTS[index].getName().equals(KEY_COMPONENT)
                        ? key
                        : COMPONENTS[index].getAccessor().invoke(mesh);
            }

            return CANONICAL.newInstance(values);
        } catch (ReflectiveOperationException refused) {
            throw new IllegalStateException("a mesh could not be copied under another cell key", refused);
        }
    }

    private static Constructor<CellMesh> canonical() {
        try {
            return CellMesh.class.getDeclaredConstructor(
                    Arrays.stream(COMPONENTS).map(RecordComponent::getType).toArray(Class<?>[]::new));
        } catch (NoSuchMethodException missing) {
            throw new IllegalStateException("the mesh record has no canonical constructor", missing);
        }
    }

    private MeshKeys() {
    }
}
