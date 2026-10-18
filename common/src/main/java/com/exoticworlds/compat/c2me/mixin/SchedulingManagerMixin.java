package com.exoticworlds.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.compat.c2me.TransformerSourceBindable;
import com.exoticworlds.core.WorldFold;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;

@Mixin(SchedulingManager.class)
public class SchedulingManagerMixin implements TransformerSource, TransformerSourceBindable {
    // Written once on the thread that builds the chunk system, read on every worker that takes a lock.
    @Unique
    private volatile @Nullable TransformerSource exotic_worlds$source;

    @Override
    public void exotic_worlds$bindTransformerSource(TransformerSource source) {
        this.exotic_worlds$source = source;
    }

    @Override
    public @Nullable WorldFold exotic_worlds$wrappedTransformer() {
        TransformerSource source = this.exotic_worlds$source;
        return source != null ? source.exotic_worlds$wrappedTransformer() : null;
    }
}
