package com.exoticworlds.frames;

import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XaeroFramesValve {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroFramesValve.class);
    private static final String OBJECT = "java/lang/Object";
    private static final String WITHIN = "within";

    public static boolean restore(ClassNode node) {
        MethodNode stripped = XaeroFramesTransformer.strippedRender();
        if (stripped == null) {
            LOGGER.warn("[ew/xaero-compat] frames gui_map_valve stripped_copy=false");
            return false;
        }

        List<MethodNode> methods = node.methods;
        for (int index = 0; index < methods.size(); index++) {
            MethodNode method = methods.get(index);
            if (!XaeroFrames.RENDER_METHOD.equals(method.name) || !stripped.desc.equals(method.desc)) {
                continue;
            }

            long started = System.nanoTime();
            String refusal = refusal(node, method);
            long ms = (System.nanoTime() - started) / 1_000_000L;
            if (refusal == null) {
                LOGGER.info("[ew/xaero-compat] frames gui_map_valve exceeded=false restored=false ms={}", ms);
                return false;
            }

            methods.set(index, stripped);
            LOGGER.warn("[ew/xaero-compat] frames gui_map_valve exceeded=true restored=true ms={} cause={}", ms, refusal);
            return true;
        }

        return false;
    }

    private static String refusal(ClassNode node, MethodNode method) {
        ClassNode one = new ClassNode();
        node.accept(one);
        one.methods.removeIf(candidate -> !(candidate.name.equals(method.name) && candidate.desc.equals(method.desc)));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String first, String second) {
                return OBJECT;
            }
        };
        try {
            one.accept(writer);
            return null;
        } catch (RuntimeException refused) {
            String cause = refused.getClass().getSimpleName();
            return cause.isEmpty() ? WITHIN : cause;
        }
    }

    private XaeroFramesValve() {
    }
}
