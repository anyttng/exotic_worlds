package com.exoticworlds.frames;

import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;

public final class XaeroFramesProcessor implements ClassProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroFramesProcessor.class);
    private static final ProcessorName NAME = new ProcessorName("exotic_worlds", "xaero_frames");
    private static final String AUDIT = "stripped the debug labels of extractRenderState";
    private static volatile MethodNode strippedRenderState;

    static MethodNode strippedRenderState() {
        return strippedRenderState;
    }

    @Override
    public ProcessorName name() {
        return NAME;
    }

    @Override
    public Set<ProcessorName> runsBefore() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && XaeroFrames.GUI_MAP_CLASS.equals(context.type().getInternalName());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        for (MethodNode method : context.node().methods) {
            if (XaeroFrames.RENDER_STATE_METHOD.equals(method.name)) {
                int removed = XaeroFrames.strip(method);
                strippedRenderState = copy(method);
                context.audit(AUDIT, String.valueOf(removed));
                LOGGER.info("[xaero-compat] frames gui_map_stripped labels_removed={}", removed);
                return ComputeFlags.SIMPLE_REWRITE;
            }
        }

        return ComputeFlags.NO_REWRITE;
    }

    private static MethodNode copy(MethodNode method) {
        ClassNode holder = new ClassNode();
        method.accept(holder);
        return holder.methods.getFirst();
    }
}
