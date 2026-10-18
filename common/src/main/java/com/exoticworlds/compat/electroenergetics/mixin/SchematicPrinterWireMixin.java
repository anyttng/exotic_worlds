package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    @Unique
    private static final String SCHEMATIC_PRINTER_MIXIN = "com.george_vi.electroenergetics.mixins.SchematicPrinterMixin";
    @Unique
    private static final String PRINT_ADVANCE_HANDLER = "electroEnergetics$advanceCurrentPos";
    @Unique
    private static final String PRINT_TARGET_HANDLER = "electroEnergetics$handleCurrentTarget";

    @Shadow
    private SchematicLevel blockReader;

    @Shadow
    private BlockPos schematicAnchor;

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.WIRE_POS_AT_UNIT_DIP))
    @TargetHandler(
            mixin = SCHEMATIC_PRINTER_MIXIN,
            name = PRINT_ADVANCE_HANDLER)
    private Vec3 exotic_worlds$wireTargetInSchematicFrame(Vec3 end1, Vec3 end2, float t, Operation<Vec3> original) {
        Vec3 point = original.call(end1, WireTemplates.besideInSchematic(this.blockReader, end1, end2), t);
        return WireTemplates.inSchematicFrame(this.blockReader, this.schematicAnchor, point);
    }

    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/schematics/SchematicPrinter;getCurrentTarget()"
                            + "Lnet/minecraft/core/BlockPos;"))
    @TargetHandler(
            mixin = SCHEMATIC_PRINTER_MIXIN,
            name = PRINT_TARGET_HANDLER)
    private BlockPos exotic_worlds$canonicalLabelTarget(BlockPos target) {
        return WireTemplates.canonical(this.blockReader, target);
    }
}
