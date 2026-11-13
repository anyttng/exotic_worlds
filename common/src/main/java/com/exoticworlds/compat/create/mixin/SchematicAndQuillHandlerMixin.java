package com.exoticworlds.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.schematics.client.SchematicAndQuillHandler;
import com.exoticworlds.compat.create.client.CreateSchematicSelection;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

@Mixin(SchematicAndQuillHandler.class)
public class SchematicAndQuillHandlerMixin {
    @Shadow
    public @Nullable BlockPos firstPos;

    @Shadow
    private @Nullable BlockPos selectedPos;

    @Inject(method = "onMouseInput", cancellable = true,
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = "Lcom/zurrtum/create/client/content/schematics/client/SchematicAndQuillHandler;"
                            + "secondPos:Lnet/minecraft/core/BlockPos;"))
    private void exotic_worlds$refuseOversizedSelection(Minecraft mc, int button,
            CallbackInfoReturnable<Boolean> callback) {
        if (CreateSchematicSelection.refuseOversizedCorner(firstPos, selectedPos)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", cancellable = true,
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = "Lcom/zurrtum/create/client/content/schematics/client/SchematicAndQuillHandler;"
                            + "firstPos:Lnet/minecraft/core/BlockPos;"))
    private void exotic_worlds$refuseOversizedGrowth(Minecraft mc, double delta,
            CallbackInfoReturnable<Boolean> callback, @Local(name = "bb") AABB grown) {
        if (CreateSchematicSelection.refuseOversizedGrowth(
                BlockPos.containing(grown.minX, grown.minY, grown.minZ),
                BlockPos.containing(grown.maxX, grown.maxY, grown.maxZ))) {
            callback.setReturnValue(true);
        }
    }
}
