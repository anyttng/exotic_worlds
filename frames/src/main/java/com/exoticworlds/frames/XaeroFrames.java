package com.exoticworlds.frames;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public final class XaeroFrames {
    public static final String GUI_MAP_CLASS = "xaero/map/gui/GuiMap";
    public static final String RENDER_METHOD = "render";
    public static final String RENDER_DESCRIPTOR = "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";

    public static int strip(MethodNode method) {
        if (method.instructions == null) {
            return 0;
        }

        Set<LabelNode> kept = keptLabels(method);
        int removed = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null;) {
            AbstractInsnNode next = insn.getNext();
            if (insn instanceof LineNumberNode || (insn instanceof LabelNode label && !kept.contains(label))) {
                method.instructions.remove(insn);
                removed++;
            }

            insn = next;
        }

        return removed;
    }

    private static Set<LabelNode> keptLabels(MethodNode method) {
        Set<LabelNode> kept = new HashSet<>();
        for (AbstractInsnNode insn : method.instructions) {
            switch (insn) {
                case JumpInsnNode jump -> kept.add(jump.label);
                case TableSwitchInsnNode table -> {
                    kept.add(table.dflt);
                    kept.addAll(table.labels);
                }
                case LookupSwitchInsnNode lookup -> {
                    kept.add(lookup.dflt);
                    kept.addAll(lookup.labels);
                }
                case FrameNode frame -> keepLabelBefore(frame, kept);
                default -> {
                }
            }
        }

        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            kept.add(block.start);
            kept.add(block.end);
            kept.add(block.handler);
        }

        if (method.localVariables != null) {
            for (LocalVariableNode variable : method.localVariables) {
                kept.add(variable.start);
                kept.add(variable.end);
            }
        }

        keepAnnotationLabels(method.visibleLocalVariableAnnotations, kept);
        keepAnnotationLabels(method.invisibleLocalVariableAnnotations, kept);
        return kept;
    }

    private static void keepLabelBefore(AbstractInsnNode insn, Set<LabelNode> kept) {
        for (AbstractInsnNode previous = insn.getPrevious(); previous != null; previous = previous.getPrevious()) {
            if (previous instanceof LabelNode label) {
                kept.add(label);
                return;
            }
        }
    }

    private static void keepAnnotationLabels(List<LocalVariableAnnotationNode> annotations, Set<LabelNode> kept) {
        if (annotations == null) {
            return;
        }

        for (LocalVariableAnnotationNode annotation : annotations) {
            kept.addAll(annotation.start);
            kept.addAll(annotation.end);
        }
    }

    private XaeroFrames() {
    }
}
