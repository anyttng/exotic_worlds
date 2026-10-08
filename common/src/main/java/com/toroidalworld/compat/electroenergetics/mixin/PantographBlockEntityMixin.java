package com.toroidalworld.compat.electroenergetics.mixin;

import java.util.function.UnaryOperator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.railway_electrification.pantograph.PantographBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.toroidalworld.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.toroidalworld.compat.electroenergetics.WireSpan;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = PantographBlockEntity.class, remap = false)
public abstract class PantographBlockEntityMixin {
    @Unique
    private static final String WIRE_START = "toroidal$wireStart";
    private static final String CATENARY_START = "Lcom/george_vi/electroenergetics/content/railway_electrification/"
            + "catenary/CatenaryConnection;getStartPos()Lnet/minecraft/world/phys/Vec3;";
    private static final String CATENARY_END = "Lcom/george_vi/electroenergetics/content/railway_electrification/"
            + "catenary/CatenaryConnection;getEndingPos()Lnet/minecraft/world/phys/Vec3;";

    @ModifyExpressionValue(method = "handleOnServer",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.NODE_GET_POSITION, ordinal = 0))
    private Vec3 toroidal$wireStartBesideCollector(Vec3 start, @Local(name = "inWorldPos") Vec3 collector,
            @Share(WIRE_START) LocalRef<Vec3> seated) {
        seated.set(WireSpan.seatIfPresent(((BlockEntity) (Object) this).getLevel(), collector, start));
        return seated.get();
    }

    @ModifyExpressionValue(method = "handleOnServer",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.NODE_GET_POSITION, ordinal = 1))
    private Vec3 toroidal$wireEndBesideStart(Vec3 end, @Share(WIRE_START) LocalRef<Vec3> seated) {
        return WireSpan.seatIfPresent(((BlockEntity) (Object) this).getLevel(), seated.get(), end);
    }

    @ModifyExpressionValue(method = "handleOnClient",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.NODE_GET_POSITION, ordinal = 0))
    private Vec3 toroidal$clientWireStartBesideCollector(Vec3 start,
            @Local(name = "positionTransform") UnaryOperator<Vec3> toWorld, @Local(name = "pantographPos") Vec3 head,
            @Share(WIRE_START) LocalRef<Vec3> seated) {
        seated.set(WireSpan.seatOnClientIfPresent(this.toroidal$level(), toWorld.apply(head), start));
        return seated.get();
    }

    @ModifyExpressionValue(method = "handleOnClient",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.NODE_GET_POSITION, ordinal = 1))
    private Vec3 toroidal$clientWireEndBesideStart(Vec3 end, @Share(WIRE_START) LocalRef<Vec3> seated) {
        return WireSpan.seatOnClientIfPresent(this.toroidal$level(), seated.get(), end);
    }

    @ModifyExpressionValue(method = "handleOnClient", at = @At(value = "INVOKE", target = CATENARY_START))
    private Vec3 toroidal$clientCatenaryStartBesideCollector(Vec3 start,
            @Local(name = "positionTransform") UnaryOperator<Vec3> toWorld, @Local(name = "pantographPos") Vec3 head,
            @Share(WIRE_START) LocalRef<Vec3> seated) {
        seated.set(WireSpan.seatOnClientIfPresent(this.toroidal$level(), toWorld.apply(head), start));
        return seated.get();
    }

    @ModifyExpressionValue(method = "handleOnClient", at = @At(value = "INVOKE", target = CATENARY_END))
    private Vec3 toroidal$clientCatenaryEndBesideStart(Vec3 end, @Share(WIRE_START) LocalRef<Vec3> seated) {
        return WireSpan.seatOnClientIfPresent(this.toroidal$level(), seated.get(), end);
    }

    @Unique
    private Level toroidal$level() {
        return ((BlockEntity) (Object) this).getLevel();
    }

    @ModifyExpressionValue(method = "handleOnServer", at = @At(value = "INVOKE", target = CATENARY_START))
    private Vec3 toroidal$catenaryStartBesideCollector(Vec3 start, @Local(name = "inWorldPos") Vec3 collector,
            @Share(WIRE_START) LocalRef<Vec3> seated) {
        seated.set(WireSpan.seatIfPresent(((BlockEntity) (Object) this).getLevel(), collector, start));
        return seated.get();
    }

    @ModifyExpressionValue(method = "handleOnServer", at = @At(value = "INVOKE", target = CATENARY_END))
    private Vec3 toroidal$catenaryEndBesideStart(Vec3 end, @Share(WIRE_START) LocalRef<Vec3> seated) {
        return WireSpan.seatIfPresent(((BlockEntity) (Object) this).getLevel(), seated.get(), end);
    }
}
