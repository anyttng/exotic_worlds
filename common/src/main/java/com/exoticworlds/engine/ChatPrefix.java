package com.exoticworlds.engine;

import com.exoticworlds.ExoticWorlds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;

public final class ChatPrefix {
    private static final String SPACE = " ";

    public static MutableComponent prefixed(Component message) {
        return Component.empty()
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal(ExoticWorlds.NAME))
                        .withStyle(ChatFormatting.GOLD))
                .append(SPACE)
                .append(message);
    }

    private ChatPrefix() {
    }
}
