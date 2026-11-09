package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.schematics.cannon.SchematicannonScreen;
import com.zurrtum.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.client.engine.SyncedTagFold;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.ValueInput;

@Mixin(SchematicannonScreen.class)
public class SchematicannonScreenMixin {
    @WrapOperation(
            method = "create(Lnet/minecraft/client/Minecraft;Lcom/zurrtum/create/foundation/gui/menu/MenuType;"
                    + "ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;"
                    + "Lnet/minecraft/network/RegistryFriendlyByteBuf;)"
                    + "Lcom/zurrtum/create/client/content/schematics/cannon/SchematicannonScreen;",
            at = @At(value = "INVOKE", target = InjectionTargets.TAG_VALUE_INPUT_CREATE))
    private static ValueInput exotic_worlds$seatMenuTag(ProblemReporter reporter, HolderLookup.Provider registries,
            CompoundTag tag, Operation<ValueInput> original, @Local SchematicannonBlockEntity entity) {
        return original.call(reporter, registries, SyncedTagFold.inFrameOf(entity, tag));
    }
}
