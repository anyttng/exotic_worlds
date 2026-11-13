package com.exoticworlds.compat.reterraforged.preview.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.map.SeamLine;
import com.exoticworlds.compat.preview.PreviewHandle;
import com.exoticworlds.compat.preview.PreviewView;
import com.exoticworlds.compat.preview.TerraForgedPreview;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import raccoonman.reterraforged.client.gui.screen.presetconfig.Preview2D;

@Mixin(value = Preview2D.class, remap = false)
public abstract class PreviewEdgesMixin {
    @Unique
    private static final int SEAM_THICKNESS = 1;

    @Inject(method = {"renderWidget", "method_48579"}, at = @At(value = "INVOKE",
            target = InjectionTargets.GUI_GRAPHICS_BLIT, shift = At.Shift.AFTER, remap = true))
    private void exotic_worlds$drawTheWorldEdges(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        PreviewView view = TerraForgedPreview.view((PreviewHandle) this);
        if (view == null || !view.copies().drawsSeamLines()) {
            return;
        }

        AbstractWidget widget = (AbstractWidget) (Object) this;
        int x = widget.getX();
        int y = widget.getY();
        int size = widget.getWidth();
        graphics.enableScissor(x, y, x + size, y + size);
        for (SeamLine line : view.seamLines(x, y, size)) {
            line.fill(graphics, SEAM_THICKNESS);
        }
        graphics.disableScissor();
    }
}
