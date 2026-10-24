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
import com.exoticworlds.compat.CreationShape;
import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.compat.ultraterraforged.preview.PreviewSettings;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.WorldFold;

import com.pandaismyname1.ultraterraforged.client.gui.createworld.PreviewSource;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainPreview;
import com.pandaismyname1.ultraterraforged.data.preset.settings.Preset;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

@Mixin(value = TerrainPreview.class, remap = false)
public abstract class TerrainPreviewMixin {
    @Shadow
    @Final
    private PreviewSource state;

    @Unique
    private @Nullable CarriedShape exotic_worlds$shown;

    @Shadow
    public abstract void refresh();

    @Inject(method = "setBounds", at = @At("RETURN"))
    private void exotic_worlds$followTheChosenShape(CallbackInfo callback) {
        WorldCreationContext settings = PreviewSettings.of(this.state);
        if (settings == null) {
            return;
        }

        CarriedShape chosen = CreationShape.carried(settings);
        if (!Objects.equals(chosen, this.exotic_worlds$shown)) {
            this.exotic_worlds$shown = chosen;
            this.refresh();
        }
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
