package com.exoticworlds.compat.distanthorizons.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.distanthorizons.DhLatticeMemo;
import com.exoticworlds.compat.distanthorizons.DhLevelLattice;
import com.exoticworlds.compat.distanthorizons.DhRepoLevel;
import com.seibel.distanthorizons.core.level.AbstractDhLevel;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.sql.repo.BeaconBeamRepo;
import com.seibel.distanthorizons.core.sql.repo.ChunkHashRepo;

@Mixin(AbstractDhLevel.class)
public class AbstractDhLevelMixin implements DhLevelLattice {
    @Shadow
    public ChunkHashRepo chunkHashRepo;

    @Shadow
    public BeaconBeamRepo beaconBeamRepo;

    @Unique
    private final DhLatticeMemo exotic_worlds$lattices = new DhLatticeMemo();

    @Override
    public DhLatticeMemo exotic_worlds$lattices() {
        return this.exotic_worlds$lattices;
    }

    @Inject(method = "createAndSetSupportingRepos", at = @At("RETURN"))
    private void exotic_worlds$bindRepoLevels(File databaseFile, CallbackInfo ci) {
        IDhLevel level = (IDhLevel) (Object) this;
        if (this.chunkHashRepo != null) {
            ((DhRepoLevel) this.chunkHashRepo).exotic_worlds$bindLevel(level);
        }

        if (this.beaconBeamRepo != null) {
            ((DhRepoLevel) this.beaconBeamRepo).exotic_worlds$bindLevel(level);
        }
    }
}
