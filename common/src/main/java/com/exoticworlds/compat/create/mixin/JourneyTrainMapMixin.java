package com.exoticworlds.compat.create.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.compat.trainmap.JourneyTrainMap;
import com.exoticworlds.compat.create.CreateInjectionTargets;
import com.exoticworlds.compat.journeymap.JourneyMapFold;
import com.exoticworlds.engine.seam.MapSurfaceCopies;
import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;

@Mixin(value = JourneyTrainMap.class, remap = false)
public abstract class JourneyTrainMapMixin {
    @WrapOperation(method = "onRender",
            at = @At(value = "INVOKE",
                    target = CreateInjectionTargets.TRAIN_MAP_MANAGER_RENDER_AND_PICK))
    private static List<FormattedText> exotic_worlds$onTheFullscreenSurface(GuiGraphics graphics, int mouseX, int mouseY,
            boolean linearFiltering, Rect2i bounds, Operation<List<FormattedText>> original) {
        Copies previous = MapSurfaceCopies.bind(JourneyMapFold.fullscreenCopies());
        try {
            return original.call(graphics, mouseX, mouseY, linearFiltering, bounds);
        } finally {
            MapSurfaceCopies.restore(previous);
        }
    }
}
