package com.exoticworlds.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.journeymap.OverlayCopies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

@Mixin(targets = "journeymap.client.render.draw.DrawImageStep", remap = false)
public abstract class DrawImageStepMixin {
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawTexture(Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Lnet/minecraft/resources/Identifier;IFDDDDDZZZ)V"))
    private void exotic_worlds$imageOnEveryCopy(GuiGraphicsExtractor graphics, Identifier texture, int color, float alpha,
            double x, double y, double widthPixels, double heightPixels, double rotation, boolean shouldFlip, boolean shouldBlur,
            boolean shouldClamp, Operation<Void> original) {
        for (double[] copy : ((OverlayCopies) (Object) this).exotic_worlds$getCopies()) {
            original.call(graphics, texture, color, alpha, x + copy[0], y + copy[1], widthPixels, heightPixels, rotation,
                    shouldFlip, shouldBlur, shouldClamp);
        }
    }
}
