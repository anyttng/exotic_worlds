package com.exoticworlds.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import java.util.function.Predicate;

final class ScanSeeds {
    private static final int THREADS = 8;

    private static final String THREAD_NAME = "scan-seed";

    private static final ExecutorService POOL = Executors.newFixedThreadPool(THREADS, task -> {
        Thread thread = new Thread(task, THREAD_NAME);
        thread.setDaemon(true);
        return thread;
    });

    static <R> List<R> inOrder(int count, IntFunction<R> seedTask) {
        return run(0, count, seedTask);
    }

    static <R> List<R> untilFirst(int count, IntFunction<R> seedTask, Predicate<R> stop) {
        List<R> results = new ArrayList<>();
        for (int start = 0; start < count; start += THREADS) {
            for (R result : run(start, Math.min(count, start + THREADS), seedTask)) {
                results.add(result);
                if (stop.test(result)) {
                    return results;
                }
            }
        }

        return results;
    }

    private static <R> List<R> run(int from, int to, IntFunction<R> seedTask) {
        List<Future<R>> pending = new ArrayList<>();
        for (int index = from; index < to; index++) {
            int seedIndex = index;
            pending.add(POOL.submit(() -> seedTask.apply(seedIndex)));
        }

        List<R> results = new ArrayList<>();
        for (Future<R> future : pending) {
            results.add(await(future));
        }

        return results;
    }

    private static <R> R await(Future<R> future) {
        try {
            return future.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        } catch (ExecutionException failed) {
            throw rethrown(failed.getCause());
        }
    }

    private static RuntimeException rethrown(Throwable cause) {
        if (cause instanceof Error error) {
            throw error;
        }

        return cause instanceof RuntimeException runtime ? runtime : new IllegalStateException(cause);
    }

    private ScanSeeds() {
    }
}
