package com.exoticworlds.compat.preview;

import com.exoticworlds.ExoticWorlds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

// mc/1.21: the UltraTerraForged, FreeTerraForged and ReTerraForged previews draw and hit-test the Custom pin through it.
public final class SpawnPin {
    private static final String SPRITE_PATH = "preview/spawn_pin";

    private static final ResourceLocation SPRITE = ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, SPRITE_PATH);

    private static final int WIDTH = 9;

    private static final int HEIGHT = 13;

    public static void draw(GuiGraphics graphics, int tipX, int tipY) {
        graphics.blitSprite(SPRITE, left(tipX), tipY - (HEIGHT - 1), WIDTH, HEIGHT);
    }

    public static boolean covers(int tipX, int tipY, double mouseX, double mouseY) {
        return mouseX >= left(tipX) && mouseX < left(tipX) + WIDTH && mouseY > tipY - HEIGHT && mouseY <= tipY;
    }

    private static int left(int tipX) {
        return tipX - WIDTH / 2;
    }

    private SpawnPin() {
    }
}
