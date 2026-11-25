package com.exoticworlds.compat.ironsspellbooks;

public final class IronsSpellbooksInjectionTargets {
    public static final String BLOCK_ENTITY_INSIDE = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V";

    public static final String SPELL_ON_CAST = "onCast(Lnet/minecraft/world/level/Level;I"
            + "Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;"
            + "Lio/redspace/ironsspellbooks/api/magic/MagicData;)V";

    private IronsSpellbooksInjectionTargets() {
    }
}
