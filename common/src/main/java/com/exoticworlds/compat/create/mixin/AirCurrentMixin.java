package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;
import com.exoticworlds.core.DeckTransformation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = AirCurrent.class, remap = false)
public class AirCurrentMixin {
    @Unique
    private static final String TICK_AFFECTED_ENTITIES = "tickAffectedEntities";
    @Unique
    private static final String SEATED_ENTITY = "toroidal$seatedEntity";
    @Unique
    private static final String SEAT = "toroidal$seat";

    @WrapOperation(
            method = TICK_AFFECTED_ENTITIES,
            at = @At(value = "INVOKE",
                    target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private AABB toroidal$foldEntityBox(Entity entity, Operation<AABB> original,
            @Share(SEATED_ENTITY) LocalRef<Entity> seatedEntity, @Share(SEAT) LocalRef<DeckTransformation> seat) {
        AABB rawBox = original.call(entity);
        return toroidal$seatOf(entity, entity.position(), seatedEntity, seat).apply(rawBox);
    }

    @WrapOperation(
            method = TICK_AFFECTED_ENTITIES,
            at = @At(value = "INVOKE",
                    target = InjectionTargets.ENTITY_POSITION))
    private Vec3 toroidal$foldEntityPosition(Entity entity, Operation<Vec3> original,
            @Share(SEATED_ENTITY) LocalRef<Entity> seatedEntity, @Share(SEAT) LocalRef<DeckTransformation> seat) {
        Vec3 raw = original.call(entity);
        return toroidal$seatOf(entity, raw, seatedEntity, seat).apply(raw);
    }

    @Unique
    private DeckTransformation toroidal$seatOf(Entity entity, Vec3 raw, LocalRef<Entity> seatedEntity,
            LocalRef<DeckTransformation> seat) {
        if (seatedEntity.get() != entity) {
            seatedEntity.set(entity);
            seat.set(toroidal$seatTransformation(raw));
        }

        return seat.get();
    }

    @Unique
    private DeckTransformation toroidal$seatTransformation(Vec3 raw) {
        AirCurrent self = (AirCurrent) (Object) this;
        return CreateSeamFold.nearestCopyTransformation(self.source.getAirCurrentWorld(),
                Vec3.atCenterOf(self.source.getAirCurrentPos()), raw);
    }
}
