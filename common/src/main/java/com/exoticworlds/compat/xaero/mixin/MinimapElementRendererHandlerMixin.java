package com.exoticworlds.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.xaero.XaeroFold;

import net.minecraft.world.phys.Vec3;

@Mixin(targets = "xaero.hud.minimap.element.render.MinimapElementRendererHandler", remap = false)
public abstract class MinimapElementRendererHandlerMixin {
    @Unique
    private static final String TRANSFORM_AND_RENDER = "transformAndRenderForRenderer(Ljava/lang/Object;Lxaero/hud/minimap/element/render/MinimapElementRenderer;Ljava/lang/Object;IDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/lib/client/graphics/XaeroBufferProvider;)Z";
    @Unique
    private static final String TRANSFORM_AND_RENDER_AT = "Lxaero/hud/minimap/element/render/MinimapElementRendererHandler;transformAndRenderForRenderer(Ljava/lang/Object;DDDLxaero/hud/minimap/element/render/MinimapElementRenderer;Ljava/lang/Object;IDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/lib/client/graphics/XaeroBufferProvider;)Z";
    @Unique
    private static final int ELEMENT_X_INDEX = 1;
    @Unique
    private static final int ELEMENT_Z_INDEX = 3;

    @Unique
    private boolean exotic_worlds$seatPending;
    @Unique
    private Vec3 exotic_worlds$seated = Vec3.ZERO;

    @ModifyArg(
            method = TRANSFORM_AND_RENDER,
            at = @At(value = "INVOKE", target = TRANSFORM_AND_RENDER_AT, ordinal = 0),
            index = ELEMENT_X_INDEX)
    private double exotic_worlds$seatElementX(double elementX, @Local(name = "elementZ") double elementZ) {
        return exotic_worlds$seat(elementX, elementZ).x;
    }

    @ModifyArg(
            method = TRANSFORM_AND_RENDER,
            at = @At(value = "INVOKE", target = TRANSFORM_AND_RENDER_AT, ordinal = 0),
            index = ELEMENT_Z_INDEX)
    private double exotic_worlds$seatElementZ(double elementZ, @Local(name = "elementX") double elementX) {
        return exotic_worlds$seat(elementX, elementZ).z;
    }

    // The first of the two handlers on a call seats the element, the second takes that seat, in either order.
    @Unique
    private Vec3 exotic_worlds$seat(double elementX, double elementZ) {
        if (this.exotic_worlds$seatPending) {
            this.exotic_worlds$seatPending = false;
            return this.exotic_worlds$seated;
        }

        this.exotic_worlds$seated = XaeroFold.nearestToCamera(elementX, elementZ);
        this.exotic_worlds$seatPending = true;
        return this.exotic_worlds$seated;
    }
}
