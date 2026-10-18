package com.exoticworlds.compat.electroenergetics.mixin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.george_vi.electroenergetics.CEERegistries;
import com.george_vi.electroenergetics.client.WireEffect;
import com.george_vi.electroenergetics.client.WireRenderer;
import com.george_vi.electroenergetics.config.CEEConfigs;
import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryConnection;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNodeConnection;
import com.george_vi.electroenergetics.simulation.WireType;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.electroenergetics.CatenaryCopies;
import com.exoticworlds.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.exoticworlds.compat.electroenergetics.SecondEndWire;
import com.exoticworlds.compat.electroenergetics.WireCopies;
import com.exoticworlds.compat.electroenergetics.WireCopyHolder;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.createmod.catnip.data.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireRenderer.class, remap = false)
public abstract class WireRendererMixin {
    @Unique
    private static final ResourceLocation STANDARD_WIRE =
            ResourceLocation.fromNamespaceAndPath("electroenergetics", "standard");
    @Unique
    private static final Map<InWorldNodeConnection, WireEffect> exotic_worlds$secondEndEffects = new HashMap<>();
    @Unique
    private static final Map<CatenaryConnection, WireEffect> exotic_worlds$secondEndCatenaryEffects = new HashMap<>();

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD",
            target = ElectroEnergeticsInjectionTargets.CATENARY_LINES, opcode = Opcodes.GETSTATIC))
    private static List<CatenaryConnection> exotic_worlds$catenaryFromEachEnd(List<CatenaryConnection> lines,
            @Local(name = "level") ClientLevel level) {
        return CatenaryCopies.fromEachEnd(level, lines);
    }

    @Inject(method = "addCatenary", at = @At(value = "INVOKE",
            target = InjectionTargets.MAP_PUT,
            shift = At.Shift.AFTER))
    private static void exotic_worlds$addSecondEndCatenaryEffect(CatenaryConnection line, CallbackInfo ci) {
        exotic_worlds$queueSecondEndCatenary(line);
    }

    @Inject(method = "removeCatenary", at = @At("RETURN"))
    private static void exotic_worlds$removeSecondEndCatenaryEffect(CatenaryConnection line, CallbackInfo ci) {
        WireEffect effect = exotic_worlds$secondEndCatenaryEffects.remove(line);
        if (effect != null) {
            VisualizationHelper.queueRemove(effect);
        }
    }

    @Inject(method = "clearAllCatenaryConnections", at = @At("RETURN"))
    private static void exotic_worlds$clearSecondEndCatenaryEffects(CallbackInfo ci) {
        exotic_worlds$dropSecondEndCatenaryEffects();
    }

    @Inject(method = "recreateVisuals", at = @At("RETURN"))
    private static void exotic_worlds$recreateSecondEndCatenaryEffects(CallbackInfo ci) {
        exotic_worlds$dropSecondEndCatenaryEffects();
        if (!CEEConfigs.client().disableFlywheelWireRendering.get()) {
            for (CatenaryConnection line : WireRenderer.CATENARY) {
                exotic_worlds$queueSecondEndCatenary(line);
            }
        }
    }

    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.ALL_WIRE_CONNECTIONS))
    private static List<Pair<InWorldNodeConnection, WireData>> exotic_worlds$passPerEnd(
            Operation<List<Pair<InWorldNodeConnection, WireData>>> original,
            @Local(name = "level") ClientLevel level) {
        return SecondEndWire.withSecondEnds(level, original.call());
    }

    @Inject(method = "render", at = @At(value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
            args = "ldc=renderBirds"))
    private static void exotic_worlds$seatPass(LevelRenderer levelRenderer, PoseStack pose, Camera camera,
            CallbackInfo ci, @Local(name = "level") ClientLevel level, @Local(name = "wire") Pair<?, ?> wire,
            @Local(name = "pos1") LocalRef<Vec3> pos1, @Local(name = "pos2") LocalRef<Vec3> pos2,
            @Local(name = "distance") LocalDoubleRef distance,
            @Local(name = "isBlock1Outer") LocalBooleanRef outer1,
            @Local(name = "isBlock2Outer") LocalBooleanRef outer2) {
        int end = SecondEndWire.end(wire);
        WireCopies.Span span = WireCopies.from(WorldLoopAttachments.transformerOfReader(level), pos1.get(),
                pos2.get(), end);
        // A moved end's insulator and jumpers belong to the pass from that end, or they join across the world.
        if (end == WireCopies.SECOND_END) {
            outer1.set(false);
        } else if (!span.pos2().equals(pos2.get())) {
            outer2.set(false);
        }

        pos1.set(span.pos1());
        pos2.set(span.pos2());
        distance.set(span.pos1().distanceTo(span.pos2()));
    }

    @Inject(method = "addConnection", at = @At("RETURN"))
    private static void exotic_worlds$addSecondEndEffect(InWorldNodeConnection connection, WireData data,
            CallbackInfo ci) {
        if (!CEEConfigs.client().disableFlywheelWireRendering.get()) {
            exotic_worlds$queueSecondEnd(connection, data);
        }
    }

    @Inject(method = "removeConnections", at = @At("RETURN"))
    private static void exotic_worlds$removeSecondEndEffect(InWorldNodeConnection connection, CallbackInfo ci) {
        WireEffect effect = exotic_worlds$secondEndEffects.remove(connection);
        if (effect != null) {
            VisualizationHelper.queueRemove(effect);
        }
    }

    @Inject(method = "clearAllWireConnections", at = @At("RETURN"))
    private static void exotic_worlds$clearSecondEndEffects(CallbackInfo ci) {
        exotic_worlds$dropSecondEndEffects();
    }

    @Inject(method = "recreateVisuals", at = @At("RETURN"))
    private static void exotic_worlds$recreateSecondEndEffects(CallbackInfo ci) {
        exotic_worlds$dropSecondEndEffects();
        if (!CEEConfigs.client().disableFlywheelWireRendering.get()) {
            for (Pair<InWorldNodeConnection, WireData> wire : WireRenderer.WIRE_CONNECTIONS) {
                exotic_worlds$queueSecondEnd(wire.getFirst(), wire.getSecond());
            }
        }
    }

    @Unique
    private static void exotic_worlds$queueSecondEnd(InWorldNodeConnection connection, WireData data) {
        WireEffect effect = new WireEffect(Minecraft.getInstance().level, connection, data.wireType(), data);
        ((WireCopyHolder) (Object) effect).exotic_worlds$setEnd(WireCopies.SECOND_END);
        WireEffect replaced = exotic_worlds$secondEndEffects.put(connection, effect);
        if (replaced != null) {
            VisualizationHelper.queueRemove(replaced);
        }

        VisualizationHelper.queueAdd(effect);
    }

    @Unique
    private static void exotic_worlds$queueSecondEndCatenary(CatenaryConnection line) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!CatenaryCopies.parted(level, line)) {
            return;
        }

        WireType standard = CEERegistries.WIRE_TYPE.get(STANDARD_WIRE);
        WireEffect effect = new WireEffect(level, line, standard,
                new WireData(standard, 0.0F, Collections.emptyList(), 0.0));
        ((WireCopyHolder) (Object) effect).exotic_worlds$setEnd(WireCopies.SECOND_END);
        WireEffect replaced = exotic_worlds$secondEndCatenaryEffects.put(line, effect);
        if (replaced != null) {
            VisualizationHelper.queueRemove(replaced);
        }

        VisualizationHelper.queueAdd(effect);
    }

    @Unique
    private static void exotic_worlds$dropSecondEndCatenaryEffects() {
        for (WireEffect effect : exotic_worlds$secondEndCatenaryEffects.values()) {
            VisualizationHelper.queueRemove(effect);
        }

        exotic_worlds$secondEndCatenaryEffects.clear();
    }

    @Unique
    private static void exotic_worlds$dropSecondEndEffects() {
        for (WireEffect effect : exotic_worlds$secondEndEffects.values()) {
            VisualizationHelper.queueRemove(effect);
        }

        exotic_worlds$secondEndEffects.clear();
    }
}
