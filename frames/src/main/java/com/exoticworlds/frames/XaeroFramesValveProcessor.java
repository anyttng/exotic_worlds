package com.exoticworlds.frames;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;

public final class XaeroFramesValveProcessor implements ClassProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroFramesValveProcessor.class);
    private static final ProcessorName NAME = new ProcessorName("exotic_worlds", "xaero_frames_valve");
    private static final String AUDIT = "restored the stripped extractRenderState over the mixin-merged one";
    private static final String OBJECT = "java/lang/Object";
    private static final String WITHIN = "within";

    @Override
    public ProcessorName name() {
        return NAME;
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && XaeroFrames.GUI_MAP_CLASS.equals(context.type().getInternalName());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        MethodNode stripped = XaeroFramesProcessor.strippedRenderState();
        if (stripped == null) {
            LOGGER.warn("[ew/xaero-compat] frames gui_map_valve stripped_copy=false");
            return ComputeFlags.NO_REWRITE;
        }

        List<MethodNode> methods = context.node().methods;
        for (int index = 0; index < methods.size(); index++) {
            MethodNode method = methods.get(index);
            if (!XaeroFrames.RENDER_STATE_METHOD.equals(method.name) || !stripped.desc.equals(method.desc)) {
                continue;
            }

            long started = System.nanoTime();
            String refusal = refusal(context.node(), method);
            long ms = (System.nanoTime() - started) / 1_000_000L;
            if (refusal == null) {
                LOGGER.info("[ew/xaero-compat] frames gui_map_valve exceeded=false restored=false ms={}", ms);
                return ComputeFlags.NO_REWRITE;
            }

            methods.set(index, stripped);
            context.audit(AUDIT, refusal);
            LOGGER.warn("[ew/xaero-compat] frames gui_map_valve exceeded=true restored=true ms={} cause={}", ms, refusal);
            return ComputeFlags.SIMPLE_REWRITE;
        }

        return ComputeFlags.NO_REWRITE;
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
}
