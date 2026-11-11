package com.exoticworlds.compat.ultraterraforged.preview.mixin;

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
import com.exoticworlds.compat.CreationShape;
import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.compat.ultraterraforged.preview.DrawnPreview;
import com.exoticworlds.compat.ultraterraforged.preview.PreviewSettings;
import com.exoticworlds.compat.ultraterraforged.preview.PreviewView;
import com.exoticworlds.compat.ultraterraforged.preview.SpawnPick;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.shape.WorldShapes;

import com.pandaismyname1.ultraterraforged.client.gui.createworld.PreviewSource;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainPreview;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;

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
    private static final double CLICK_SLOP_PIXELS = 2.0;

    @Unique
    private static final String SPAWN_PIN_PATH = "preview/spawn_pin";

    @Unique
    private static final Identifier SPAWN_PIN = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, SPAWN_PIN_PATH);

    @Unique
    private static final int PIN_WIDTH = 9;

    @Unique
    private static final int PIN_HEIGHT = 13;

    @Unique
    private static final int HINT_INSET = 4;

    @Unique
    private static final int HINT_ABOVE_BOTTOM = 24;

    @Unique
    private static final int HINT_TEXT_COLOR = 0xFFFFFFFF;

    @Shadow
    @Final
    private PreviewSource state;

    @Unique
    private @Nullable CarriedShape exotic_worlds$shown;

    @Unique
    private boolean exotic_worlds$pressed;

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
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.exotic_worlds$pressed = true;
        this.exotic_worlds$pressedX = event.x();
        this.exotic_worlds$pressedY = event.y();
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        boolean clicked = this.exotic_worlds$pressed
                && Math.abs(event.x() - this.exotic_worlds$pressedX) <= CLICK_SLOP_PIXELS
                && Math.abs(event.y() - this.exotic_worlds$pressedY) <= CLICK_SLOP_PIXELS;
        this.exotic_worlds$pressed = false;
        if (clicked) {
            this.exotic_worlds$pickSpawn(event.x(), event.y());
        }
    }

    @Unique
    private void exotic_worlds$pickSpawn(double mouseX, double mouseY) {
        PreviewView view = ((DrawnPreview) (Object) this).exotic_worlds$drawn();
        WorldCreationContext settings = PreviewSettings.of(this.state);
        if (view == null || settings == null || !this.exotic_worlds$picksSpawn()) {
            return;
        }

        BlockPos picked = view.blockAt(this.getX(), this.getY(), this.getWidth(), mouseX, mouseY);
        if (WorldShapes.moveSpawn(SpawnPick.at(view.shape(), picked))) {
            this.exotic_worlds$shown = CreationShape.carried(settings);
        }
    }

    @Inject(method = "extractWidgetRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;getInstance()Lnet/minecraft/client/Minecraft;", ordinal = 0))
    private void exotic_worlds$markTheSpawn(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        PreviewView view = ((DrawnPreview) (Object) this).exotic_worlds$drawn();
        CarriedShape shown = this.exotic_worlds$shown;
        if (view == null || shown == null || !shown.fold().isWrapped()) {
            return;
        }

        BlockPos marked = SpawnPick.marked(SpawnPoint.of(shown.fold()), view.searchCenter());
        int x = this.getX();
        int y = this.getY();
        int size = this.getWidth();
        graphics.enableScissor(x, y, x + size, y + size);
        for (PreviewView.Pixel pixel : view.copiesOf(marked, x, y, size)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPAWN_PIN, pixel.x() - PIN_WIDTH / 2,
                    pixel.y() - (PIN_HEIGHT - 1), PIN_WIDTH, PIN_HEIGHT);
        }
        graphics.disableScissor();
    }

    @Inject(method = "extractWidgetRenderState", at = @At("RETURN"))
    private void exotic_worlds$hintTheSpawnPick(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo callback) {
        PreviewView view = ((DrawnPreview) (Object) this).exotic_worlds$drawn();
        if (!this.exotic_worlds$picksSpawn() || view != null && this.isMouseOver(mouseX, mouseY)) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        if (font.width(SPAWN_HINT) <= this.getWidth() - 2 * HINT_INSET) {
            graphics.text(font, SPAWN_HINT, this.getX() + HINT_INSET, this.getY() + this.getHeight() - HINT_ABOVE_BOTTOM,
                    HINT_TEXT_COLOR);
        }
    }

    @Unique
    private boolean exotic_worlds$picksSpawn() {
        CarriedShape shown = this.exotic_worlds$shown;
        return shown != null && shown.fold().isWrapped();
    }

    @Unique
    private static @Nullable CarriedShape exotic_worlds$terrainOf(@Nullable CarriedShape carried) {
        return carried == null ? null : new CarriedShape(carried.shape(),
                carried.generationOptions().with(SpawnPoint.OPTION, SpawnPoint.ZERO));
    }

    @WrapOperation(method = "refresh", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/ScheduledExecutorService;"
                    + "schedule(Ljava/lang/Runnable;JLjava/util/concurrent/TimeUnit;)"
                    + "Ljava/util/concurrent/ScheduledFuture;"))
    private ScheduledFuture<?> exotic_worlds$generateUnderTheChosenShape(ScheduledExecutorService executor,
            Runnable generate, long delay, TimeUnit unit, Operation<ScheduledFuture<?>> original) {
        CarriedShape shown = this.exotic_worlds$shown;
        return original.call(executor,
                CreationShape.underRouterBuild(shown != null ? shown.fold() : null, generate), delay, unit);
    }

    @WrapOperation(method = "generate", at = @At(value = "INVOKE",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/GeneratorContext;"
                    + "makeUncached(Lcom/pandaismyname1/ultraterraforged/data/preset/settings/Preset;IIII)"
                    + "Lcom/pandaismyname1/ultraterraforged/world/worldgen/GeneratorContext;"))
    private GeneratorContext exotic_worlds$carryTheChosenShape(Preset preset, int seed, int tileSize, int tileBorder,
            int batchCount, Operation<GeneratorContext> original) {
        GeneratorContext context = original.call(preset, seed, tileSize, tileBorder, batchCount);
        WorldFold fold = CreationShape.routerBuildFold();
        if (fold.isWrapped()) {
            LapCarrier.carry(context, fold);
        }
        return context;
    }
}
