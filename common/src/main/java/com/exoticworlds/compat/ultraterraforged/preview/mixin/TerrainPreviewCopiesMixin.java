package com.exoticworlds.compat.ultraterraforged.preview.mixin;

import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.CreationShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.SeamLine;
import com.exoticworlds.compat.ultraterraforged.preview.DrawnPreview;
import com.exoticworlds.compat.ultraterraforged.preview.PreviewView;
import com.exoticworlds.core.ToroidalShapeView;

import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainPreview;
import com.pandaismyname1.ultraterraforged.data.preset.settings.WorldSettings;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mixin(value = TerrainPreview.class, remap = false)
public abstract class TerrainPreviewCopiesMixin implements DrawnPreview {
    @Unique
    private static final int SEAM_THICKNESS = 1;

    @Shadow
    @Final
    private static AtomicInteger GENERATION;

    @Shadow
    private float zoom;

    @Shadow
    private float panX;

    @Shadow
    private float panZ;

    @Unique
    private @Nullable PreviewView exotic_worlds$drawn;

    @Override
    public @Nullable PreviewView exotic_worlds$drawn() {
        return this.exotic_worlds$drawn;
    }

    @ModifyVariable(method = "generate", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float exotic_worlds$capTheZoom(float zoom, @Share("shape") LocalRef<ToroidalShape> shape,
            @Share("copies") LocalRef<MapCopies> copies) {
        shape.set(new ToroidalShapeView(CreationShape.routerBuildFold()));
        copies.set(MapCopies.current());
        int widgetPixels = ((AbstractWidget) (Object) this).getWidth();
        return Math.min(zoom, PreviewView.zoomCeiling(shape.get(), copies.get(), widgetPixels));
    }

    @ModifyVariable(method = "generate", at = @At(value = "LOAD", ordinal = 1), name = "centerX")
    private float exotic_worlds$holdCenterX(float centerX, @Local(argsOnly = true, ordinal = 0) float zoom,
            @Share("shape") LocalRef<ToroidalShape> shape, @Share("copies") LocalRef<MapCopies> copies,
            @Share("shiftX") LocalFloatRef shiftX) {
        float held = PreviewView.heldCenter(shape.get(), copies.get(), Direction.Axis.X, centerX, zoom);
        shiftX.set(held - centerX);
        return held;
    }

    @ModifyVariable(method = "generate", at = @At(value = "LOAD", ordinal = 1), name = "centerZ")
    private float exotic_worlds$holdCenterZ(float centerZ, @Local(argsOnly = true, ordinal = 0) float zoom,
            @Share("shape") LocalRef<ToroidalShape> shape, @Share("copies") LocalRef<MapCopies> copies,
            @Share("shiftZ") LocalFloatRef shiftZ) {
        float held = PreviewView.heldCenter(shape.get(), copies.get(), Direction.Axis.Z, centerZ, zoom);
        shiftZ.set(held - centerZ);
        return held;
    }

    @WrapOperation(method = "generate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;execute(Ljava/lang/Runnable;)V", ordinal = 0))
    private void exotic_worlds$adoptTheDrawnView(Minecraft minecraft, Runnable apply, Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int generation, @Local(argsOnly = true, ordinal = 0) float zoom,
            @Local(argsOnly = true, ordinal = 1) float panX, @Local(argsOnly = true, ordinal = 2) float panZ,
            @Local(name = "centerX") float centerX, @Local(name = "centerZ") float centerZ,
            @Local(name = "context") GeneratorContext context,
            @Local(name = "properties") WorldSettings.Properties properties,
            @Share("shape") LocalRef<ToroidalShape> shape, @Share("copies") LocalRef<MapCopies> copies,
            @Share("shiftX") LocalFloatRef shiftX, @Share("shiftZ") LocalFloatRef shiftZ) {
        PreviewView view = new PreviewView(shape.get(), copies.get(), centerX, centerZ, zoom,
                properties.spawnType.getSearchCenter(context));
        float heldPanX = panX + shiftX.get();
        float heldPanZ = panZ + shiftZ.get();
        original.call(minecraft, (Runnable) () -> {
            apply.run();
            if (generation == GENERATION.get()) {
                this.exotic_worlds$drawn = view;
                this.zoom = zoom;
                this.panX = heldPanX;
                this.panZ = heldPanZ;
            }
        });
    }

    @Inject(method = "extractWidgetRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"
                    + "Lnet/minecraft/resources/Identifier;IIFFIIII)V",
            shift = At.Shift.AFTER))
    private void exotic_worlds$drawTheWorldEdges(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo callback) {
        PreviewView view = this.exotic_worlds$drawn;
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

    @ModifyVariable(method = "renderReadout", at = @At("STORE"), name = "blockZ")
    private int exotic_worlds$readInsideTheWorld(int blockZ, @Local(name = "blockX") LocalIntRef blockX) {
        PreviewView view = this.exotic_worlds$drawn;
        if (view == null) {
            return blockZ;
        }

        BlockPos inside = view.inside(blockX.get(), blockZ);
        blockX.set(inside.getX());
        return inside.getZ();
    }
}
