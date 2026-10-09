package com.exoticworlds.compat.distanthorizons;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

public final class FoldBindingExecutor extends AbstractExecutorService {
    private final ExecutorService delegate;

    private final WorldFold fold;

    public FoldBindingExecutor(ExecutorService delegate, WorldFold fold) {
        this.delegate = delegate;
        this.fold = fold;
    }

    @Override
    public void execute(Runnable command) {
        this.delegate.execute(() -> GenerationTransformerContext.runWithTransformer(this.fold, command));
    }

    @Override
    public void shutdown() {
        this.delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.delegate.awaitTermination(timeout, unit);
    }
}
