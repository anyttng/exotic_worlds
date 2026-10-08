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
    private static final ResourceLocation STANDARD_WIRE =
            ResourceLocation.fromNamespaceAndPath("electroenergetics", "standard");
    @Unique
    private static final Map<InWorldNodeConnection, WireEffect> toroidal$secondEndEffects = new HashMap<>();
    @Unique
    private static final Map<CatenaryConnection, WireEffect> toroidal$secondEndCatenaryEffects = new HashMap<>();

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD",
            target = ElectroEnergeticsInjectionTargets.CATENARY_LINES, opcode = Opcodes.GETSTATIC))
    private static List<CatenaryConnection> toroidal$catenaryFromEachEnd(List<CatenaryConnection> lines,
            @Local(name = "level") ClientLevel level) {
        return CatenaryCopies.fromEachEnd(level, lines);
    }

    @Inject(method = "addCatenary", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            shift = At.Shift.AFTER))
    private static void toroidal$addSecondEndCatenaryEffect(CatenaryConnection line, CallbackInfo ci) {
        toroidal$queueSecondEndCatenary(line);
    }

    @Inject(method = "removeCatenary", at = @At("RETURN"))
    private static void toroidal$removeSecondEndCatenaryEffect(CatenaryConnection line, CallbackInfo ci) {
        WireEffect effect = toroidal$secondEndCatenaryEffects.remove(line);
        if (effect != null) {
            VisualizationHelper.queueRemove(effect);
        }
    }

    @Inject(method = "clearAllCatenaryConnections", at = @At("RETURN"))
    private static void toroidal$clearSecondEndCatenaryEffects(CallbackInfo ci) {
        toroidal$dropSecondEndCatenaryEffects();
    }

    @Inject(method = "recreateVisuals", at = @At("RETURN"))
    private static void toroidal$recreateSecondEndCatenaryEffects(CallbackInfo ci) {
        toroidal$dropSecondEndCatenaryEffects();
        if (!CEEConfigs.client().disableFlywheelWireRendering.get()) {
            for (CatenaryConnection line : WireRenderer.CATENARY) {
                toroidal$queueSecondEndCatenary(line);
            }
        }
    }

    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.ALL_WIRE_CONNECTIONS))
    private static List<Pair<InWorldNodeConnection, WireData>> toroidal$passPerEnd(
            Operation<List<Pair<InWorldNodeConnection, WireData>>> original,
            @Local(name = "level") ClientLevel level) {
        return SecondEndWire.withSecondEnds(level, original.call());
    }

    @Inject(method = "render", at = @At(value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
            args = "ldc=renderBirds"))
    private static void toroidal$seatPass(LevelRenderer levelRenderer, PoseStack pose, Camera camera,
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
    private static void toroidal$addSecondEndEffect(InWorldNodeConnection connection, WireData data,
            CallbackInfo ci) {
        if (!CEEConfigs.client().disableFlywheelWireRendering.get()) {
            toroidal$queueSecondEnd(connection, data);
        }
    }

    @Inject(method = "removeConnections", at = @At("RETURN"))
    private static void toroidal$removeSecondEndEffect(InWorldNodeConnection connection, CallbackInfo ci) {
        WireEffect effect = toroidal$secondEndEffects.remove(connection);
        if (effect != null) {
            VisualizationHelper.queueRemove(effect);
        }
    }

    @Inject(method = "clearAllWireConnections", at = @At("RETURN"))
    private static void toroidal$clearSecondEndEffects(CallbackInfo ci) {
        toroidal$dropSecondEndEffects();
    }

    @Inject(method = "recreateVisuals", at = @At("RETURN"))
    private static void toroidal$recreateSecondEndEffects(CallbackInfo ci) {
        toroidal$dropSecondEndEffects();
        if (!CEEConfigs.client().disableFlywheelWireRendering.get()) {
            for (Pair<InWorldNodeConnection, WireData> wire : WireRenderer.WIRE_CONNECTIONS) {
                toroidal$queueSecondEnd(wire.getFirst(), wire.getSecond());
            }
        }
    }

    @Unique
    private static void toroidal$queueSecondEnd(InWorldNodeConnection connection, WireData data) {
        WireEffect effect = new WireEffect(Minecraft.getInstance().level, connection, data.wireType(), data);
        ((WireCopyHolder) (Object) effect).toroidal$setEnd(WireCopies.SECOND_END);
        WireEffect replaced = toroidal$secondEndEffects.put(connection, effect);
        if (replaced != null) {
            VisualizationHelper.queueRemove(replaced);
        }

        VisualizationHelper.queueAdd(effect);
    }

    @Unique
    private static void toroidal$queueSecondEndCatenary(CatenaryConnection line) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!CatenaryCopies.parted(level, line)) {
            return;
        }

        WireType standard = CEERegistries.WIRE_TYPE.get(STANDARD_WIRE);
        WireEffect effect = new WireEffect(level, line, standard,
                new WireData(standard, 0.0F, Collections.emptyList(), 0.0));
        ((WireCopyHolder) (Object) effect).toroidal$setEnd(WireCopies.SECOND_END);
        WireEffect replaced = toroidal$secondEndCatenaryEffects.put(line, effect);
        if (replaced != null) {
            VisualizationHelper.queueRemove(replaced);
        }

        VisualizationHelper.queueAdd(effect);
    }

    @Unique
    private static void toroidal$dropSecondEndCatenaryEffects() {
        for (WireEffect effect : toroidal$secondEndCatenaryEffects.values()) {
            VisualizationHelper.queueRemove(effect);
        }

        toroidal$secondEndCatenaryEffects.clear();
    }

    @Unique
    private static void toroidal$dropSecondEndEffects() {
        for (WireEffect effect : toroidal$secondEndEffects.values()) {
            VisualizationHelper.queueRemove(effect);
        }

        toroidal$secondEndEffects.clear();
    }
}
