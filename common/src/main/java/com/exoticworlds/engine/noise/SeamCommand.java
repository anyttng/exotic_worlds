package com.exoticworlds.engine.noise;

import java.util.List;

import com.exoticworlds.ExoticWorlds;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SeamCommand {
    private static final String EDGE_LITERAL = "edge";
    private static final String DETAILS_LITERAL = "details";
    static final String DETAILS_COMMAND = "/" + ExoticWorlds.MODID + " " + EDGE_LITERAL + " " + DETAILS_LITERAL;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ExoticWorlds.MODID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal(EDGE_LITERAL)
                        .executes(context -> report(context.getSource()))
                        .then(Commands.literal(DETAILS_LITERAL)
                                .executes(context -> reportOnce(context.getSource())))));
    }

    private static int reportOnce(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!SeamNoticeRecord.of(source.getServer()).takeDetailsPending(player.getUUID())) {
            ExoticWorlds.LOGGER.info("[ew/edge] details_repeat player={}", player.getName().getString());
            return 0;
        }

        return report(source);
    }

    private static int report(CommandSourceStack source) {
        ExoticWorlds.LOGGER.info("[ew/edge] command source={}", source.getTextName());
        List<SeamVerdict> verdicts = PeriodicityCheck.verdictsOf(source.getServer());
        if (verdicts.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(SeamMessages.NONE_KEY), false);
            return 0;
        }

        int broken = 0;
        for (SeamVerdict verdict : verdicts) {
            for (Component line : SeamMessages.report(verdict)) {
                source.sendSuccess(() -> line, false);
            }

            broken += verdict.broken() ? 1 : 0;
        }

        ExoticWorlds.LOGGER.info("[ew/edge] command_done dimensions={} broken={}", verdicts.size(), broken);
        return broken;
    }

    private SeamCommand() {
    }
}
