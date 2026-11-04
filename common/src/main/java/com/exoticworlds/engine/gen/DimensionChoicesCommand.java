package com.exoticworlds.engine.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.engine.net.DimensionChoicesPayload;
import com.exoticworlds.platform.Platforms;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.LevelStem;

public final class DimensionChoicesCommand {
    private static final String DIMENSIONS_LITERAL = "dimensions";
    private static final String COMMAND = "/" + ExoticWorlds.MODID + " " + DIMENSIONS_LITERAL;
    private static final String KEY_PREFIX = "commands.exotic_worlds.dimensions.";
    private static final String UNDECIDED_KEY = KEY_PREFIX + "undecided";
    private static final String CONFIGURE_KEY = KEY_PREFIX + "configure";
    private static final String CONFIGURE_HINT_KEY = CONFIGURE_KEY + ".hint";
    private static final String NO_CLIENT_KEY = KEY_PREFIX + "no_client";
    private static final String SAVED_KEY = KEY_PREFIX + "saved";
    private static final String REFUSED_KEY = KEY_PREFIX + "refused";
    private static final String DROPPED_KEY = KEY_PREFIX + "dropped";
    private static final String NAME_SEPARATOR = ", ";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ExoticWorlds.MODID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal(DIMENSIONS_LITERAL).executes(context -> open(context.getSource()))));
    }

    public static void hintTo(ServerPlayer player) {
        if (!isOperator(player)) {
            return;
        }

        List<ResourceKey<LevelStem>> undecided = DimensionChoices.undecided(player.level().getServer());
        if (undecided.isEmpty()) {
            return;
        }

        Component configure = ComponentUtils.wrapInSquareBrackets(Component.translatable(CONFIGURE_KEY))
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand(COMMAND))
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable(CONFIGURE_HINT_KEY))));
        player.sendSystemMessage(Component.translatable(UNDECIDED_KEY, names(undecided)).append(" ").append(configure));
    }

    public static void apply(ServerPlayer player, Map<ResourceKey<LevelStem>, DimensionTable.Entry> choices) {
        if (!isOperator(player)) {
            ExoticWorlds.LOGGER.warn("[ew/dimensions] apply refused=not_operator player={}",
                    player.getName().getString());
            return;
        }

        MinecraftServer server = player.level().getServer();
        List<ResourceKey<LevelStem>> refused = new ArrayList<>();
        choices.forEach((key, entry) -> {
            if (DimensionChoices.choose(server, key, entry) != null) {
                refused.add(key);
            }
        });

        if (refused.size() < choices.size()) {
            player.sendSystemMessage(Component.translatable(SAVED_KEY));
        }

        if (!refused.isEmpty()) {
            player.sendSystemMessage(Component.translatable(REFUSED_KEY, names(refused)).withStyle(ChatFormatting.RED));
        }
    }

    static void tellDropped(MinecraftServer server, ResourceKey<LevelStem> key) {
        Component message = Component.translatable(DROPPED_KEY, key.identifier().toString())
                .withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isOperator(player)) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<DimensionChoicesPayload.Row> rows = DimensionChoices.rows(source.getServer());
        ExoticWorlds.LOGGER.info("[ew/dimensions] open player={} rows={}", player.getName().getString(), rows.size());
        if (!Platforms.get().sendDimensionChoices(player, new DimensionChoicesPayload(rows))) {
            source.sendFailure(Component.translatable(NO_CLIENT_KEY));
            return 0;
        }

        return rows.size();
    }

    private static boolean isOperator(ServerPlayer player) {
        return Commands.LEVEL_GAMEMASTERS.check(player.permissions());
    }

    private static String names(List<ResourceKey<LevelStem>> keys) {
        return keys.stream()
                .map(key -> key.identifier().toString())
                .sorted()
                .collect(Collectors.joining(NAME_SEPARATOR));
    }

    private DimensionChoicesCommand() {
    }
}
