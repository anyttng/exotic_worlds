package com.exoticworlds.compat.xaero;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public final class XaeroFrameBudget {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String GUI_MAP_RESOURCE = "xaero/map/gui/GuiMap.class";
    static final String RENDER_STATE_METHOD = "extractRenderState";
    static final long BUDGET = 95_000_000L;
    private static final String FRAMES_CLASS = "com.exoticworlds.frames.XaeroFrames";
    private static final String STRIP_METHOD = "strip";
    private static final String SET_COMPUTE_LIMITS = "setComputeLimits";
    private static final String LIMIT_EXCEPTION = "LimitExceededException";
    private static final String OBJECT = "java/lang/Object";
    private static @Nullable Boolean exceeded;

    record Verdict(boolean classFound, boolean limitPresent, boolean stripped, boolean exceeded) {
        static final Verdict ABSENT = new Verdict(false, false, false, false);
    }

    public static synchronized boolean exceeded() {
        if (exceeded == null) {
            long started = System.nanoTime();
            ClassLoader loader = XaeroFrameBudget.class.getClassLoader();
            Verdict verdict = measure(loader, read(loader, GUI_MAP_RESOURCE), RENDER_STATE_METHOD, BUDGET, true);
            LOGGER.info("[ew/xaero-compat] gate xaero_worldmap_frames budget_exceeded={} asm_limit_present={} class_found={}"
                    + " stripped={} budget={} ms={}", verdict.exceeded(), verdict.limitPresent(), verdict.classFound(),
                    verdict.stripped(), BUDGET, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            exceeded = verdict.exceeded();
        }

        return exceeded;
    }

    static byte @Nullable [] read(ClassLoader loader, String resource) {
        try (InputStream stream = loader.getResourceAsStream(resource)) {
            return stream == null ? null : stream.readAllBytes();
        } catch (IOException unreadable) {
            return null;
        }
    }

    static Verdict measure(ClassLoader loader, byte @Nullable [] classBytes, String methodName, long budget,
            boolean strip) {
        if (classBytes == null) {
            return Verdict.ABSENT;
        }

        Method limits = computeLimits();
        if (limits == null) {
            return new Verdict(true, false, false, false);
        }

        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, 0);
        node.methods.removeIf(candidate -> !methodName.equals(candidate.name));
        boolean stripped = strip && !node.methods.isEmpty() && strip(loader, node.methods.getFirst());
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String first, String second) {
                return OBJECT;
            }
        };
        try {
            limits.invoke(writer, Integer.MAX_VALUE, budget);
            node.accept(writer);
            return new Verdict(true, true, stripped, false);
        } catch (IllegalAccessException | InvocationTargetException unreachable) {
            throw new IllegalStateException(SET_COMPUTE_LIMITS, unreachable);
        } catch (RuntimeException refused) {
            if (LIMIT_EXCEPTION.equals(refused.getClass().getSimpleName())) {
                return new Verdict(true, true, stripped, true);
            }

            throw refused;
        }
    }

    private static @Nullable Method computeLimits() {
        try {
            return ClassWriter.class.getMethod(SET_COMPUTE_LIMITS, int.class, long.class);
        } catch (NoSuchMethodException absent) {
            return null;
        }
    }

    private static boolean strip(ClassLoader loader, MethodNode method) {
        try {
            Class.forName(FRAMES_CLASS, true, loader).getMethod(STRIP_METHOD, MethodNode.class).invoke(null, method);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException absent) {
            return false;
        } catch (IllegalAccessException | InvocationTargetException unreachable) {
            throw new IllegalStateException(FRAMES_CLASS, unreachable);
        }
    }

    private XaeroFrameBudget() {
    }
}
