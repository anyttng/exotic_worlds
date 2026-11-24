package com.exoticworlds.engine.noise.check;

import java.util.List;

import com.exoticworlds.ExoticWorlds;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class SeamCommand {
    private static final String CHECK_EDGE_LITERAL = "check_edge";
    private static final String DETAILS_LITERAL = "details";
    static final String DETAILS_COMMAND =
            "/" + ExoticWorlds.MODID + " " + CHECK_EDGE_LITERAL + " " + DETAILS_LITERAL;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ExoticWorlds.MODID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal(CHECK_EDGE_LITERAL)
                        .executes(context -> report(context.getSource()))
                        .then(Commands.literal(DETAILS_LITERAL)
                                .executes(context -> reportOnce(context.getSource())))));
    }

    private static int reportOnce(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (SeamCheckRun.of(source.getServer()).isRunning()) {
            return refuse(source);
        }

        if (!SeamDetailsPending.of(source.getServer()).tryTake(player.getUUID())) {
            ExoticWorlds.LOGGER.info("[ew/edge] details_repeat player={}", player.getName().getString());
            return 0;
        }

        return report(source);
    }

    private static int report(CommandSourceStack source) {
        ExoticWorlds.LOGGER.info("[ew/edge] command source={}", source.getTextName());
        MinecraftServer server = source.getServer();
        List<ServerLevel> levels = PeriodicityCheck.wrappedLevels(server);
        if (levels.isEmpty()) {
            source.sendSuccess(SeamMessages::none, false);
            return 0;
        }

        if (levels.stream().allMatch(PeriodicityCheck::isFullVerdictKnown)) {
            return reportKnown(source, levels);
        }

        if (!SeamCheckRun.of(server).tryStart(source, levels)) {
            return refuse(source);
        }

        source.sendSuccess(SeamMessages::started, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int reportKnown(CommandSourceStack source, List<ServerLevel> levels) {
        source.sendSuccess(SeamMessages::header, false);
        int broken = 0;
        for (ServerLevel level : levels) {
            SeamVerdict verdict = PeriodicityCheck.fullVerdictOf(level);
            for (Component line : SeamMessages.report(verdict)) {
                source.sendSuccess(() -> line, false);
            }

            broken += verdict.isBroken() ? 1 : 0;
        }

        ExoticWorlds.LOGGER.info("[ew/edge] command_done dimensions={} broken={}", levels.size(), broken);
        return broken;
    }

    private static int refuse(CommandSourceStack source) {
        source.sendFailure(SeamMessages.running());
        return 0;
    }

    private SeamCommand() {
    }
}
