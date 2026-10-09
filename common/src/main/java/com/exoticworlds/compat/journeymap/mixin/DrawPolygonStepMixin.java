package com.exoticworlds.compat.journeymap.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.exoticworlds.compat.journeymap.OverlayCopies;

import journeymap.api.v2.client.model.ShapeProperties;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

@Mixin(targets = "journeymap.client.render.draw.DrawPolygonStep", remap = false)
public abstract class DrawPolygonStepMixin {
    @Unique
    private static final String GEOMETRY_TAIL = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
            + "DDLjourneymap/client/render/map/Renderer;DD)V";

    @Unique
    private static final String POLYGON_TAIL = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
            + "DDLjava/util/List;Ljava/util/List;Ljava/util/List;Lnet/minecraft/client/renderer/texture/AbstractTexture;"
            + "Lnet/minecraft/resources/Identifier;Ljourneymap/api/v2/client/model/ShapeProperties;)V";

    @Unique
    private static final String GUI_GEOMETRY =
            "drawGeometry(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lorg/joml/Matrix3x2fStack;" + GEOMETRY_TAIL;

    @Unique
    private static final String POSE_GEOMETRY =
            "drawGeometry(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + GEOMETRY_TAIL;

    @Unique
    private static final String GUI_POLYGON =
            "Ljourneymap/client/render/draw/DrawUtil;drawPolygon(Lorg/joml/Matrix3x2fStack;" + POLYGON_TAIL;

    @Unique
    private static final String POSE_POLYGON =
            "Ljourneymap/client/render/draw/DrawUtil;drawPolygon(Lcom/mojang/blaze3d/vertex/PoseStack;" + POLYGON_TAIL;

    @WrapOperation(method = GUI_GEOMETRY, at = @At(value = "INVOKE", target = GUI_POLYGON))
    private void toroidal$polygonOnEveryCopy(Matrix3x2fStack pose, MultiBufferSource.BufferSource buffers,
            double xOffset, double yOffset, List<?> fillPoints, List<?> strokePoints, List<?> texturePoints,
            AbstractTexture texture, Identifier textureId, ShapeProperties properties, Operation<Void> original) {
        for (double[] copy : ((OverlayCopies) (Object) this).toroidal$copies()) {
            original.call(pose, buffers, xOffset + copy[0], yOffset + copy[1], fillPoints, strokePoints,
                    texturePoints, texture, textureId, properties);
        }
    }

    @WrapOperation(method = POSE_GEOMETRY, at = @At(value = "INVOKE", target = POSE_POLYGON))
    private void toroidal$posePolygonOnEveryCopy(PoseStack pose, MultiBufferSource.BufferSource buffers,
            double xOffset, double yOffset, List<?> fillPoints, List<?> strokePoints, List<?> texturePoints,
            AbstractTexture texture, Identifier textureId, ShapeProperties properties, Operation<Void> original) {
        for (double[] copy : ((OverlayCopies) (Object) this).toroidal$copies()) {
            original.call(pose, buffers, xOffset + copy[0], yOffset + copy[1], fillPoints, strokePoints,
                    texturePoints, texture, textureId, properties);
        }
    }
}
