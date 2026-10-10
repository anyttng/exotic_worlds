package com.exoticworlds.compat.reterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.reterraforged.LappedCenter;
import com.exoticworlds.compat.reterraforged.RtfLap;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.continent.SimpleContinent;
import raccoonman.reterraforged.world.worldgen.cell.continent.simple.SingleContinentGenerator;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

// The one continent is found at construction, before any lap is known; on a lap it is the canonical centre of the
// cell holding the origin, found again under the lap the level runs with.
@Mixin(value = SingleContinentGenerator.class, remap = false)
public abstract class SingleContinentGeneratorMixin {
    private static final String CENTER = "Lraccoonman/reterraforged/world/worldgen/cell/continent/simple/"
            + "SingleContinentGenerator;center:Lraccoonman/reterraforged/world/worldgen/noise/NoiseUtil$Vec2i;";

    private static final float ORIGIN = 0.0F;

    @Unique
    private volatile @Nullable LappedCenter toroidal$lappedCenter;

    @ModifyExpressionValue(method = "apply", at = @At(value = "FIELD", target = CENTER, opcode = Opcodes.GETFIELD))
    private NoiseUtil.Vec2i toroidal$canonicalCenter(NoiseUtil.Vec2i center) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return center;
        }

        double xLap = frame.lap(Direction.Axis.X);
        double zLap = frame.lap(Direction.Axis.Z);
        double skew = frame.skew();
        LappedCenter held = this.toroidal$lappedCenter;
        if (held != null && held.xLap() == xLap && held.zLap() == zLap && held.skew() == skew) {
            return held.center();
        }

        long packed = ((SimpleContinent) (Object) this).getNearestCenter(ORIGIN, ORIGIN);
        NoiseUtil.Vec2i lapped = new NoiseUtil.Vec2i(PosUtil.unpackLeft(packed), PosUtil.unpackRight(packed));
        this.toroidal$lappedCenter = new LappedCenter(xLap, zLap, skew, lapped);
        return lapped;
    }
}
