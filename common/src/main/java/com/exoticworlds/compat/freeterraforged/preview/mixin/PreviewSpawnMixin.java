package com.exoticworlds.compat.freeterraforged.preview.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.preview.MarkerBox;
import com.exoticworlds.compat.preview.PreviewHandle;
import com.exoticworlds.compat.preview.SpawnPin;
import com.exoticworlds.compat.preview.TerraForgedPreview;

import com.mojang.blaze3d.platform.InputConstants;

import etcodehome.freeterraforged.client.gui.screen.presetconfig.Preview2D;
import etcodehome.freeterraforged.client.gui.screen.presetconfig.Preview3D;
import etcodehome.freeterraforged.data.worldgen.preset.settings.SpawnType;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

@Mixin(value = {Preview2D.class, Preview3D.class}, remap = false)
public abstract class PreviewSpawnMixin {
    @Unique
    private static final String PROPERTIES = "Letcodehome/freeterraforged/data/worldgen/preset/settings/WorldSettings$Properties;";

    @Unique
    private static final String SPAWN_TYPE =
            PROPERTIES + "spawnType:Letcodehome/freeterraforged/data/worldgen/preset/settings/SpawnType;";

    @Unique
    private static final String SPAWN_X = PROPERTIES + "spawnX:I";

    @Unique
    private static final String SPAWN_Z = PROPERTIES + "spawnZ:I";

    @Unique
    private static final int MARKER_BAR_SHADOW = 0;

    @Unique
    private static final int MARKER_BAR = 1;

    @Unique
    private static final int MARKER_STEM_SHADOW = 2;

    @Unique
    private static final int MARKER_STEM = 3;

    @Unique
    private static final String SPAWN_HINT_KEY = "gui.exotic_worlds.terraforged.preview.spawn_hint";

    @Unique
    private static final Component SPAWN_HINT = Component.translatable(SPAWN_HINT_KEY).withStyle(ChatFormatting.GRAY);

    @Unique
    private static final String RESET_HINT_KEY = "gui.exotic_worlds.terraforged.preview.reset_hint";

    @Unique
    private static final Component RESET_HINT = Component.translatable(RESET_HINT_KEY).withStyle(ChatFormatting.GRAY);

    @Unique
    private static final int HINT_INSET = 4;

    @Unique
    private static final int SPAWN_HINT_ABOVE_BOTTOM = 36;

    @Unique
    private static final int RESET_HINT_ABOVE_BOTTOM = 24;

    @Unique
    private static final int HINT_TEXT_COLOR = 0xFFFFFFFF;

    @Unique
    private @Nullable BlockPos exotic_worlds$drawing;

    @WrapMethod(method = "renderSpawnMarker")
    private void exotic_worlds$markEveryCopy(GuiGraphics graphics, Operation<Void> original) {
        PreviewHandle preview = (PreviewHandle) this;
        preview.exotic_worlds$readout().exotic_worlds$markers().clear();
        List<BlockPos> copies = TerraForgedPreview.customSpawnCopies(preview);
        if (copies == null) {
            original.call(graphics);
            return;
        }

        AbstractWidget widget = (AbstractWidget) (Object) this;
        graphics.enableScissor(widget.getX(), widget.getY(), widget.getX() + widget.getWidth(),
                widget.getY() + widget.getHeight());
        try {
            for (BlockPos copy : copies) {
                this.exotic_worlds$drawing = copy;
                original.call(graphics);
            }
        } finally {
            this.exotic_worlds$drawing = null;
            graphics.disableScissor();
        }
    }

    @ModifyExpressionValue(method = "renderSpawnMarker",
            at = @At(value = "FIELD", target = SPAWN_TYPE, opcode = Opcodes.GETSTATIC))
    private SpawnType exotic_worlds$markTheCopyAsPicked(SpawnType spawnType) {
        return this.exotic_worlds$drawing != null ? SpawnType.USER_SELECTED : spawnType;
    }

    @ModifyExpressionValue(method = "renderSpawnMarker",
            at = @At(value = "FIELD", target = SPAWN_X, opcode = Opcodes.GETSTATIC))
    private int exotic_worlds$markTheCopyX(int spawnX) {
        BlockPos copy = this.exotic_worlds$drawing;
        return copy != null ? copy.getX() : spawnX;
    }

    @ModifyExpressionValue(method = "renderSpawnMarker",
            at = @At(value = "FIELD", target = SPAWN_Z, opcode = Opcodes.GETSTATIC))
    private int exotic_worlds$markTheCopyZ(int spawnZ) {
        BlockPos copy = this.exotic_worlds$drawing;
        return copy != null ? copy.getZ() : spawnZ;
    }

    @WrapOperation(method = "renderSpawnMarker",
            at = @At(value = "INVOKE", target = InjectionTargets.GUI_GRAPHICS_FILL, ordinal = MARKER_BAR, remap = true))
    private void exotic_worlds$pinTheCopy(GuiGraphics graphics, int minX, int barY, int maxX, int maxY, int color,
            Operation<Void> original) {
        if (this.exotic_worlds$drawing == null) {
            original.call(graphics, minX, barY, maxX, maxY, color);
            return;
        }

        MarkerBox pin = MarkerBox.ofBar(minX, barY, maxX);
        SpawnPin.draw(graphics, pin.tipX(), pin.tipY());
        ((PreviewHandle) this).exotic_worlds$readout().exotic_worlds$markers().add(pin);
    }

    @WrapOperation(method = "renderSpawnMarker", at = {
            @At(value = "INVOKE", target = InjectionTargets.GUI_GRAPHICS_FILL, ordinal = MARKER_BAR_SHADOW,
                    remap = true),
            @At(value = "INVOKE", target = InjectionTargets.GUI_GRAPHICS_FILL, ordinal = MARKER_STEM_SHADOW,
                    remap = true),
            @At(value = "INVOKE", target = InjectionTargets.GUI_GRAPHICS_FILL, ordinal = MARKER_STEM,
                    remap = true)})
    private void exotic_worlds$leaveTheCrossOff(GuiGraphics graphics, int minX, int minY, int maxX, int maxY,
            int color, Operation<Void> original) {
        if (this.exotic_worlds$drawing == null) {
            original.call(graphics, minX, minY, maxX, maxY, color);
        }
    }

    @Inject(method = {"mouseClicked", "method_25402"}, at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$resetOnTheMarker(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> callback) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT
                && TerraForgedPreview.resetOnMarker((PreviewHandle) this, mouseX, mouseY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "updateLegend", at = @At("RETURN"))
    private void exotic_worlds$readTheCustomSpawn(int mouseX, int mouseY, CallbackInfoReturnable<Boolean> callback) {
        TerraForgedPreview.showCustomSpawn((PreviewHandle) this);
    }

    @Inject(method = {"renderWidget", "method_48579"}, at = @At("RETURN"))
    private void exotic_worlds$hintTheSpawnPick(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        if (!TerraForgedPreview.picksSpawn((PreviewHandle) this)) {
            return;
        }

        this.exotic_worlds$hint(graphics, SPAWN_HINT, SPAWN_HINT_ABOVE_BOTTOM);
        this.exotic_worlds$hint(graphics, RESET_HINT, RESET_HINT_ABOVE_BOTTOM);
    }

    @Unique
    private void exotic_worlds$hint(GuiGraphics graphics, Component hint, int aboveBottom) {
        AbstractWidget widget = (AbstractWidget) (Object) this;
        Font font = Minecraft.getInstance().font;
        if (font.width(hint) <= widget.getWidth() - 2 * HINT_INSET) {
            graphics.drawString(font, hint, widget.getX() + HINT_INSET,
                    widget.getY() + widget.getHeight() - aboveBottom, HINT_TEXT_COLOR);
        }
    }
}
