package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.ChatPrefix;
import com.exoticworlds.platform.Platforms;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

final class SeamMessages {
    private static final String KEY_PREFIX = "commands.exotic_worlds.check_edge.";
    private static final String NONE_KEY = KEY_PREFIX + "none";
    private static final String HEADER_KEY = KEY_PREFIX + "header";
    private static final String HOLDS_KEY = KEY_PREFIX + "holds";
    private static final String BROKEN_KEY = KEY_PREFIX + "broken";
    private static final String UNCHECKED_KEY = KEY_PREFIX + "unchecked";
    private static final String CAUSE_KEY_PREFIX = KEY_PREFIX + "cause.";
    private static final String CAUSE_MOD_KEY = CAUSE_KEY_PREFIX + "mod";
    private static final String CAUSE_AT_KEY = CAUSE_KEY_PREFIX + "at";
    private static final String CAUSE_VANILLA_KEY = CAUSE_KEY_PREFIX + "vanilla";
    private static final String CAUSE_VANILLA_NONE_KEY = CAUSE_VANILLA_KEY + ".none";
    private static final String CAUSE_BIOME_SOURCE_KEY = CAUSE_KEY_PREFIX + "biome_source";
    private static final String CAUSE_GENERATOR_KEY = CAUSE_KEY_PREFIX + "generator";
    private static final String CAUSE_FOLLOWS_KEY = CAUSE_KEY_PREFIX + "follows";
    private static final String CAUSE_UNKNOWN_KEY = CAUSE_KEY_PREFIX + "unknown";
    private static final String NOTICE_KEY = KEY_PREFIX + "notice";
    private static final String NOTICE_UNKNOWN_KEY = NOTICE_KEY + ".unknown";
    private static final String DETAILS_KEY = KEY_PREFIX + "details";
    private static final String DETAILS_HINT_KEY = DETAILS_KEY + ".hint";
    private static final String CAUSE_INDENT = "  ";
    private static final String SPACE = " ";

    static Component none() {
        return ChatPrefix.prefixed(Component.translatable(NONE_KEY));
    }

    static Component header() {
        return ChatPrefix.prefixed(Component.translatable(HEADER_KEY));
    }

    static List<Component> report(SeamVerdict verdict) {
        String dimension = verdict.dimension().location().toString();
        SeamCause.Generator unchecked = verdict.unchecked();
        if (unchecked != null) {
            return List.of(Component.translatable(UNCHECKED_KEY, dimension), causeLine(unchecked));
        }

        if (!verdict.broken()) {
            return List.of(Component.translatable(HOLDS_KEY, dimension).withStyle(ChatFormatting.GREEN));
        }

        List<Component> lines = new ArrayList<>();
        for (SeamVerdict.Break broken : verdict.breaks()) {
            lines.add(Component.translatable(BROKEN_KEY, dimension, broken.field()).withStyle(ChatFormatting.RED));
            lines.add(causeLine(broken.cause()));
        }

        return lines;
    }

    static Component notice(SeamVerdict verdict) {
        String dimension = verdict.dimension().location().toString();
        List<String> mods = verdict.breaks().stream()
                .flatMap(broken -> modsOf(broken.cause()))
                .distinct()
                .toList();
        MutableComponent line = mods.isEmpty()
                ? Component.translatable(NOTICE_UNKNOWN_KEY, dimension)
                : Component.translatable(NOTICE_KEY, dimension, ComponentUtils.formatList(mods));
        return ChatPrefix.prefixed(line.append(SPACE).append(details()));
    }

    private static Component details() {
        return ComponentUtils.wrapInSquareBrackets(Component.translatable(DETAILS_KEY))
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, SeamCommand.DETAILS_COMMAND))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(DETAILS_HINT_KEY))));
    }

    private static Component causeLine(SeamCause cause) {
        return Component.literal(CAUSE_INDENT).append(causeOf(cause));
    }

    private static Component causeOf(SeamCause cause) {
        return switch (cause) {
            case SeamCause.Node node when node.vanilla() -> node.candidates().isEmpty()
                    ? Component.translatable(CAUSE_VANILLA_NONE_KEY, node.type())
                    : Component.translatable(CAUSE_VANILLA_KEY, node.type(),
                            ComponentUtils.formatList(namesOf(node.candidates())));
            case SeamCause.Node node -> Component.translatable(CAUSE_MOD_KEY, ownerOf(node.namespace(), node.type()),
                    node.holder() != null
                            ? Component.translatable(CAUSE_AT_KEY, node.type(), node.holder())
                            : Component.literal(node.type()));
            case SeamCause.Biomes biomes -> Component.translatable(CAUSE_BIOME_SOURCE_KEY, biomes.type(),
                    ownerOf(biomes.namespace(), biomes.type()));
            case SeamCause.Generator generator -> Component.translatable(CAUSE_GENERATOR_KEY, generator.type(),
                    ownerOf(generator.namespace(), generator.type()));
            case SeamCause.Follows follows -> Component.translatable(CAUSE_FOLLOWS_KEY, follows.field());
            case SeamCause.NotFound ignored -> Component.translatable(CAUSE_UNKNOWN_KEY);
        };
    }

    private static Stream<String> modsOf(SeamCause cause) {
        return switch (cause) {
            case SeamCause.Node node when node.vanilla() -> namesOf(node.candidates()).stream();
            case SeamCause.Node node -> Stream.of(ownerOf(node.namespace(), node.type()));
            case SeamCause.Biomes biomes -> Stream.of(ownerOf(biomes.namespace(), biomes.type()));
            case SeamCause.Generator generator -> Stream.of(ownerOf(generator.namespace(), generator.type()));
            case SeamCause.Follows ignored -> Stream.empty();
            case SeamCause.NotFound ignored -> Stream.empty();
        };
    }

    private static List<String> namesOf(List<String> modIds) {
        return modIds.stream().map(modId -> Platforms.get().modName(modId).orElse(modId)).toList();
    }

    private static String ownerOf(@Nullable String namespace, String type) {
        return namespace != null ? Platforms.get().modName(namespace).orElse(namespace) : type;
    }

    private SeamMessages() {
    }
}
