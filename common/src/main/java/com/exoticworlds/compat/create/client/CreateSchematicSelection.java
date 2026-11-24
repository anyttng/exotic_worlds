package com.exoticworlds.compat.create.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.create.CreateSchematicFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class CreateSchematicSelection {
    private static final String OVERSIZED_SELECTION_KEY = "message.exotic_worlds.schematic.selection_wider_than_world";
    private static final String OVERSIZED_GROWTH_KEY = "message.exotic_worlds.schematic.selection_cannot_grow_past_world";

    public static boolean tryRefuseOversizedCorner(@Nullable BlockPos first, @Nullable BlockPos second) {
        return tryRefuseOversized(first, second, OVERSIZED_SELECTION_KEY);
    }

    public static boolean tryRefuseOversizedGrowth(@Nullable BlockPos first, @Nullable BlockPos second) {
        return tryRefuseOversized(first, second, OVERSIZED_GROWTH_KEY);
    }

    private static boolean tryRefuseOversized(@Nullable BlockPos first, @Nullable BlockPos second, String messageKey) {
        if (first == null || second == null
                || !CreateSchematicFold.isRegionExceedingWorld(Minecraft.getInstance().level, first, second)) {
            return false;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendOverlayMessage(Component.translatable(messageKey));
        }

        return true;
    }

    private CreateSchematicSelection() {
    }
}
