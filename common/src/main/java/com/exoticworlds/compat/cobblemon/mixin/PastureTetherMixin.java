package com.exoticworlds.compat.cobblemon.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(PokemonEntity.class)
public class PastureTetherMixin {
    @Inject(
            method = {"setTethering", "load(Lnet/minecraft/nbt/CompoundTag;)V"},
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
                    target = "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;tethering:Lcom/cobblemon/mod/common/block/entity/PokemonPastureBlockEntity$Tethering;"))
    private void exotic_worlds$bindTetherLevel(CallbackInfo ci) {
        PokemonEntity self = (PokemonEntity) (Object) this;
        PokemonPastureBlockEntity.Tethering tethering = self.getTethering();
        if (tethering != null && self.level() instanceof ServerLevel level) {
            ((LevelBindable) tethering).exotic_worlds$bindLevel(level);
        }
    }

    @WrapOperation(method = "checkPastureTether",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;contains(DDD)Z"))
    private boolean exotic_worlds$tetherThroughSeam(AABB box, double x, double y, double z,
            Operation<Boolean> original) {
        AABB seated = FoldedBoxQuery.toward((PokemonEntity) (Object) this, new Vec3(x, y, z), box);
        return original.call(seated, x, y, z);
    }
}
