package com.exoticworlds.compat.ultraterraforged.preview.mixin;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.compat.preview.CreationShape;
import com.exoticworlds.compat.preview.PreviewView;
import com.exoticworlds.compat.preview.SpawnPick;
import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.compat.ultraterraforged.preview.DrawnPreview;
import com.exoticworlds.compat.ultraterraforged.preview.PreviewSettings;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.shape.WorldShapes;

import com.pandaismyname1.ultraterraforged.client.gui.createworld.PreviewSource;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainPreview;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Mixin(value = TerrainPreview.class, remap = false)
public abstract class TerrainPreviewMixin extends AbstractWidget {
    @Unique
    private static final String SPAWN_HINT_KEY = "gui.exotic_worlds.ultraterraforged.preview.spawn_hint";

    @Unique
    private static final Component SPAWN_HINT = Component.translatable(SPAWN_HINT_KEY).withStyle(ChatFormatting.GRAY);

    @Unique
    private static final String RESET_HINT_KEY = "gui.exotic_worlds.ultraterraforged.preview.reset_hint";

    @Unique
    private static final Component RESET_HINT = Component.translatable(RESET_HINT_KEY).withStyle(ChatFormatting.GRAY);

    @Unique
    private static final double CLICK_SLOP_PIXELS = 2.0;

    @Unique
    private static final String SPAWN_PIN_PATH = "preview/spawn_pin";

    @Unique
    private static final Identifier SPAWN_PIN = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, SPAWN_PIN_PATH);

    @Unique
    private static final int PIN_WIDTH_PIXELS = 9;

    @Unique
    private static final int PIN_HEIGHT_PIXELS = 13;

    @Unique
    private static final int HINT_INSET_PIXELS = 4;

    @Unique
    private static final int SPAWN_HINT_ABOVE_BOTTOM = 36;

    @Unique
    private static final int RESET_HINT_ABOVE_BOTTOM = 24;

    @Unique
    private static final int HINT_TEXT_COLOR = 0xFFFFFFFF;

    @Shadow
    @Final
    private PreviewSource state;

    @Unique
    private @Nullable CarriedShape exotic_worlds$shown;

    @Unique
    private boolean exotic_worlds$isPressed;

    @Unique
    private double exotic_worlds$pressedX;

    @Unique
    private double exotic_worlds$pressedY;

    private TerrainPreviewMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow
    public abstract void refresh();

    @Inject(method = "setBounds", at = @At("RETURN"))
    private void exotic_worlds$followTheChosenShape(CallbackInfo callback) {
        WorldCreationContext settings = PreviewSettings.of(this.state);
        if (settings == null) {
            return;
        }

        CarriedShape chosen = CreationShape.carried(settings);
        CarriedShape shown = this.exotic_worlds$shown;
        this.exotic_worlds$shown = chosen;
        if (!Objects.equals(exotic_worlds$terrainOf(chosen), exotic_worlds$terrainOf(shown))) {
            this.refresh();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && this.exotic_worlds$isOnPin(event.x(), event.y())) {
            this.exotic_worlds$moveSpawn(SpawnPoint.DEFAULT);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.exotic_worlds$isPressed = true;
        this.exotic_worlds$pressedX = event.x();
        this.exotic_worlds$pressedY = event.y();
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        boolean isClicked = this.exotic_worlds$isPressed
                && Math.abs(event.x() - this.exotic_worlds$pressedX) <= CLICK_SLOP_PIXELS
                && Math.abs(event.y() - this.exotic_worlds$pressedY) <= CLICK_SLOP_PIXELS;
        this.exotic_worlds$isPressed = false;
        if (isClicked) {
            this.exotic_worlds$pickSpawn(event.x(), event.y());
        }
    }

    @Unique
    private void exotic_worlds$pickSpawn(double mouseX, double mouseY) {
        PreviewView view = ((DrawnPreview) (Object) this).exotic_worlds$getDrawn();
        if (view == null || !this.exotic_worlds$isPickingSpawn()) {
            return;
        }

        BlockPos picked = view.blockAt(this.getX(), this.getY(), this.getWidth(), mouseX, mouseY);
        this.exotic_worlds$moveSpawn(SpawnPick.at(view.fold(), picked));
    }

    @Unique
    private void exotic_worlds$moveSpawn(SpawnPoint spawn) {
        WorldCreationContext settings = PreviewSettings.of(this.state);
        if (settings != null && WorldShapes.tryMoveSpawn(spawn)) {
            this.exotic_worlds$shown = CreationShape.carried(settings);
        }
    }

    @Unique
    private boolean exotic_worlds$isOnPin(double mouseX, double mouseY) {
        for (PreviewView.Pixel pixel : this.exotic_worlds$getPins()) {
            if (mouseX >= pixel.x() - PIN_WIDTH_PIXELS / 2 && mouseX < pixel.x() - PIN_WIDTH_PIXELS / 2 + PIN_WIDTH_PIXELS
                    && mouseY > pixel.y() - PIN_HEIGHT_PIXELS && mouseY <= pixel.y()) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private List<PreviewView.Pixel> exotic_worlds$getPins() {
        PreviewView view = ((DrawnPreview) (Object) this).exotic_worlds$getDrawn();
        CarriedShape shown = this.exotic_worlds$shown;
        if (view == null || shown == null || !shown.getFold().isWrapped()) {
            return List.of();
        }

        SpawnPoint spawn = SpawnPoint.of(shown.getFold());
        return spawn.isCustom()
                ? view.copiesOf(SpawnPick.pinOf(spawn), this.getX(), this.getY(), this.getWidth())
                : List.of();
    }

    @Inject(method = "extractWidgetRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;getInstance()Lnet/minecraft/client/Minecraft;", ordinal = 0))
    private void exotic_worlds$markTheSpawn(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        List<PreviewView.Pixel> pins = this.exotic_worlds$getPins();
        if (pins.isEmpty()) {
            return;
        }

        int x = this.getX();
        int y = this.getY();
        int sizePixels = this.getWidth();
        graphics.enableScissor(x, y, x + sizePixels, y + sizePixels);
        for (PreviewView.Pixel pixel : pins) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPAWN_PIN, pixel.x() - PIN_WIDTH_PIXELS / 2,
                    pixel.y() - (PIN_HEIGHT_PIXELS - 1), PIN_WIDTH_PIXELS, PIN_HEIGHT_PIXELS);
        }
        graphics.disableScissor();
    }

    @Inject(method = "extractWidgetRenderState", at = @At("RETURN"))
    private void exotic_worlds$hintTheSpawnPick(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo callback) {
        if (!this.exotic_worlds$isPickingSpawn()) {
            return;
        }

        this.exotic_worlds$hint(graphics, SPAWN_HINT, SPAWN_HINT_ABOVE_BOTTOM);
        this.exotic_worlds$hint(graphics, RESET_HINT, RESET_HINT_ABOVE_BOTTOM);
    }

    @Unique
    private void exotic_worlds$hint(GuiGraphicsExtractor graphics, Component hint, int aboveBottom) {
        Font font = Minecraft.getInstance().font;
        if (font.width(hint) <= this.getWidth() - 2 * HINT_INSET_PIXELS) {
            graphics.text(font, hint, this.getX() + HINT_INSET_PIXELS, this.getY() + this.getHeight() - aboveBottom,
                    HINT_TEXT_COLOR);
        }
    }

    @Unique
    private boolean exotic_worlds$isPickingSpawn() {
        CarriedShape shown = this.exotic_worlds$shown;
        return shown != null && shown.getFold().isWrapped();
    }

    @Unique
    private static @Nullable CarriedShape exotic_worlds$terrainOf(@Nullable CarriedShape carried) {
        return carried == null ? null : new CarriedShape(carried.getShape(),
                carried.getOptions().with(SpawnPoint.OPTION, SpawnPoint.DEFAULT));
    }

    @WrapOperation(method = "refresh", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/ScheduledExecutorService;"
                    + "schedule(Ljava/lang/Runnable;JLjava/util/concurrent/TimeUnit;)"
                    + "Ljava/util/concurrent/ScheduledFuture;"))
    private ScheduledFuture<?> exotic_worlds$generateUnderTheChosenShape(ScheduledExecutorService executor,
            Runnable generate, long delay, TimeUnit unit, Operation<ScheduledFuture<?>> original) {
        CarriedShape shown = this.exotic_worlds$shown;
        return original.call(executor,
                CreationShape.underRouterBuild(shown != null ? shown.getFold() : null, generate), delay, unit);
    }

    @WrapOperation(method = "generate", at = @At(value = "INVOKE",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/GeneratorContext;"
                    + "makeUncached(Lcom/pandaismyname1/ultraterraforged/data/preset/settings/Preset;IIII)"
                    + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/GeneratorContext;"))
    private GeneratorContext exotic_worlds$carryTheChosenShape(Preset preset, int seed, int tileSize, int tileBorder,
            int batchCount, Operation<GeneratorContext> original) {
        GeneratorContext context = original.call(preset, seed, tileSize, tileBorder, batchCount);
        WorldFold fold = CreationShape.getRouterBuildFold();
        if (fold.isWrapped()) {
            LapCarrier.carry(context, fold);
        }
        return context;
    }
}
