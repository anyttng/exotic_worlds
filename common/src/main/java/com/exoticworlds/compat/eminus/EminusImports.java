package com.exoticworlds.compat.eminus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public final class EminusImports {
    private static final String IMPORT_TILES_CLASS = "com.eminus.ingest.saved.ImportTiles";
    private static final String SAVED_COLUMN_CLASS = "com.eminus.ingest.saved.SavedColumn";
    private static final String TILE_BITS = "TILE_BITS";
    private static final String CENTER = "center";

    private EminusImports() {
    }

    public static int getTileBits() {
        return Handles.TILE_BITS;
    }

    public static Object readCenter(Object column) {
        try {
            return Handles.CENTER.invoke(column);
        } catch (Throwable failure) {
            throw new IllegalStateException("Eminus saved column center unreadable", failure);
        }
    }

    private static final class Handles {
        private static final int TILE_BITS;
        private static final MethodHandle CENTER;

        static {
            try {
                ClassLoader loader = EminusImports.class.getClassLoader();
                TILE_BITS = Class.forName(IMPORT_TILES_CLASS, false, loader).getField(EminusImports.TILE_BITS)
                        .getInt(null);
                Field center = Class.forName(SAVED_COLUMN_CLASS, false, loader).getDeclaredField(EminusImports.CENTER);
                center.setAccessible(true);
                CENTER = MethodHandles.lookup().unreflectGetter(center);
            } catch (ReflectiveOperationException missing) {
                throw new ExceptionInInitializerError(missing);
            }
        }

        private Handles() {
        }
    }
}
