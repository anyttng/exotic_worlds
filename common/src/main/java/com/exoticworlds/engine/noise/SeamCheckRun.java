package com.exoticworlds.engine.noise;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.accessors.SeamCheckRunHolder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class SeamCheckRun {
    private static final String THREAD_NAME = "exotic-worlds-edge-check";
    private static final String FAILED_KEY = "command.failed";

    private final AtomicBoolean running = new AtomicBoolean();

    static SeamCheckRun of(MinecraftServer server) {
        return ((SeamCheckRunHolder) server).exotic_worlds$seamCheckRun();
    }

    boolean running() {
        return this.running.get();
    }

    boolean start(CommandSourceStack source, List<ServerLevel> levels) {
        if (!this.running.compareAndSet(false, true)) {
            return false;
        }

        Thread thread = new Thread(() -> read(source, levels), THREAD_NAME);
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private void read(CommandSourceStack source, List<ServerLevel> levels) {
        MinecraftServer server = source.getServer();
        try {
            for (ServerLevel level : levels) {
                if (!server.isRunning()) {
                    return;
                }

                SeamVerdict verdict = PeriodicityCheck.fullVerdictOf(level);
                server.execute(() -> send(source, SeamMessages.report(verdict)));
            }

            server.execute(() -> end(source, SeamMessages.finished(), false));
        } catch (RuntimeException failed) {
            ExoticWorlds.LOGGER.error("[ew/edge] run_failed text={}", failed.toString(), failed);
            server.execute(() -> end(source, Component.translatable(FAILED_KEY), true));
        }
    }

    private static void send(CommandSourceStack source, List<Component> lines) {
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
    }

    private void end(CommandSourceStack source, Component line, boolean failed) {
        this.running.set(false);
        if (failed) {
            source.sendFailure(line);
        } else {
            source.sendSuccess(() -> line, false);
        }
    }
}
