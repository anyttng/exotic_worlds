package com.exoticworlds.compat.create.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.compat.trainmap.XaeroTrainMap;
import com.exoticworlds.compat.create.CreateInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;
import com.exoticworlds.engine.seam.MapSurfaceCopies;
import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;

@Mixin(value = XaeroTrainMap.class, remap = false)
public abstract class XaeroTrainMapMixin {
    @WrapOperation(method = "onRender",
            at = @At(value = "INVOKE",
                    target = CreateInjectionTargets.TRAIN_MAP_MANAGER_RENDER_AND_PICK))
    private static List<FormattedText> exotic_worlds$onTheWorldMapSurface(GuiGraphics graphics, int mouseX, int mouseY,
            boolean linearFiltering, Rect2i bounds, Operation<List<FormattedText>> original) {
        Copies copies = XaeroWorldMapFold.worldMapCopies(bounds.getX(), bounds.getX() + bounds.getWidth(),
                bounds.getY(), bounds.getY() + bounds.getHeight());
        Copies previous = MapSurfaceCopies.bind(copies);
        try {
            return original.call(graphics, mouseX, mouseY, linearFiltering, bounds);
        } finally {
            MapSurfaceCopies.restore(previous);
        }
    }
}
