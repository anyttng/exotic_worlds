package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.railway_electrification.third_rail.RailContactShoeBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = RailContactShoeBlockEntity.class, remap = false)
public abstract class RailContactShoeBlockEntityMixin {
    @Unique
    private static final String WIRE_START = "toroidal$wireStart";

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
}
