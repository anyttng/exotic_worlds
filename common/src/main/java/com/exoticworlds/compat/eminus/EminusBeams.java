package com.exoticworlds.compat.eminus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public final class EminusBeams {
    private static final String BEAM_CLASS = "com.eminus.beam.Beam";
    private static final String POS = "pos";
    private static final String SECTIONS = "sections";

    private EminusBeams() {
    }

    public static long pos(Object beam) {
        try {
            return (long) Handles.POS.invoke(beam);
        } catch (Throwable failure) {
            throw new IllegalStateException("Eminus beam position unreadable", failure);
        }
    }

    public static Object at(Object beam, long pos) {
        try {
            return Handles.CREATE.invoke(pos, (List<?>) Handles.SECTIONS.invoke(beam));
        } catch (Throwable failure) {
            throw new IllegalStateException("Eminus beam not copied", failure);
        }
    }

    private static final class Handles {
        private static final MethodHandle CREATE;
        private static final MethodHandle POS;
        private static final MethodHandle SECTIONS;

        static {
            try {
                Class<?> beam = Class.forName(BEAM_CLASS, false, EminusBeams.class.getClassLoader());
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                CREATE = lookup.findConstructor(beam, MethodType.methodType(void.class, long.class, List.class));
                POS = lookup.findVirtual(beam, EminusBeams.POS, MethodType.methodType(long.class));
                SECTIONS = lookup.findVirtual(beam, EminusBeams.SECTIONS, MethodType.methodType(List.class));
            } catch (ReflectiveOperationException missing) {
                throw new ExceptionInInitializerError(missing);
            }
        }
    }
}
