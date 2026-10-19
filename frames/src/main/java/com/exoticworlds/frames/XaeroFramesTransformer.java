package com.exoticworlds.frames;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TargetType;
import cpw.mods.modlauncher.api.TransformerVoteResult;

public final class XaeroFramesTransformer implements ITransformer<MethodNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroFramesTransformer.class);
    private static volatile @Nullable MethodNode strippedRender;

    static @Nullable MethodNode strippedRender() {
        return strippedRender;
    }

    @Override
    public MethodNode transform(MethodNode input, ITransformerVotingContext context) {
        int removed = XaeroFrames.strip(input);
        strippedRender = copy(input);
        LOGGER.info("[ew/xaero-compat] frames gui_map_stripped labels_removed={}", removed);
        return input;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<Target<MethodNode>> targets() {
        return Set.of(Target.targetMethod(XaeroFrames.GUI_MAP_CLASS, XaeroFrames.RENDER_METHOD,
                XaeroFrames.RENDER_DESCRIPTOR));
    }

    @Override
    public TargetType<MethodNode> getTargetType() {
        return TargetType.METHOD;
    }

    private static MethodNode copy(MethodNode method) {
        ClassNode holder = new ClassNode();
        method.accept(holder);
        return holder.methods.getFirst();
    }
}
