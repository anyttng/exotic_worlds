package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.exoticworlds.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireTemplates;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SchematicPrinter.class, priority = 1300, remap = false)
public abstract class SchematicPrinterWireMixin {
    @Shadow
    private SchematicLevel blockReader;

    @Shadow
    private BlockPos schematicAnchor;

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE",
                    target = "Lcom/george_vi/electroenergetics/foundation/QuadraticWireHelper;posAt"
                            + "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    @TargetHandler(
            mixin = ElectroEnergeticsInjectionTargets.SCHEMATIC_PRINTER_MIXIN,
            name = ElectroEnergeticsInjectionTargets.PRINT_ADVANCE_HANDLER)
    private Vec3 toroidal$wireTargetInSchematicFrame(Vec3 end1, Vec3 end2, float t, Operation<Vec3> original) {
        Vec3 point = original.call(end1, WireTemplates.besideInSchematic(this.blockReader, end1, end2), t);
        return WireTemplates.inSchematicFrame(this.blockReader, this.schematicAnchor, point);
    }

    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/schematics/SchematicPrinter;getCurrentTarget()"
                            + "Lnet/minecraft/core/BlockPos;"))
    @TargetHandler(
            mixin = ElectroEnergeticsInjectionTargets.SCHEMATIC_PRINTER_MIXIN,
            name = ElectroEnergeticsInjectionTargets.PRINT_TARGET_HANDLER)
    private BlockPos toroidal$canonicalLabelTarget(BlockPos target) {
        return WireTemplates.canonical(this.blockReader, target);
    }
}
