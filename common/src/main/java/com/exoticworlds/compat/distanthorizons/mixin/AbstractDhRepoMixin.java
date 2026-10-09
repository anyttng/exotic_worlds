package com.exoticworlds.compat.distanthorizons.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.compat.distanthorizons.DhKeys;
import com.exoticworlds.compat.distanthorizons.DhLattice;
import com.exoticworlds.compat.distanthorizons.DhProbes;
import com.exoticworlds.compat.distanthorizons.DhRepoLevel;
import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.sql.dto.IBaseDTO;
import com.seibel.distanthorizons.core.sql.repo.AbstractDhRepo;

@Mixin(AbstractDhRepo.class)
public class AbstractDhRepoMixin implements DhRepoLevel {
    @Unique
    private IDhLevel toroidal$level;

    @Unique
    private Boolean toroidal$shapeSeen;

    @Override
    public void toroidal$bindLevel(IDhLevel level) {
        this.toroidal$level = level;
    }

    @Override
    public @Nullable DhLattice toroidal$lattice() {
        DhLattice lattice = DhShapes.of(this.toroidal$level);
        boolean present = lattice != null;
        if (this.toroidal$shapeSeen == null || this.toroidal$shapeSeen != present) {
            this.toroidal$shapeSeen = present;
            DhProbes.repoShape(this, this.toroidal$level, present);
        }

        return lattice;
    }

    @WrapMethod(method = "getByKey(Ljava/lang/Object;)Lcom/seibel/distanthorizons/core/sql/dto/IBaseDTO;")
    private @Nullable IBaseDTO<?> toroidal$answerTheAskedKey(Object key, Operation<@Nullable IBaseDTO<?>> original) {
        IBaseDTO<?> dto = original.call(key);
        if (dto != null && this.toroidal$lattice() != null) {
            DhKeys.reseat(dto, key);
        }

        return dto;
    }
}
