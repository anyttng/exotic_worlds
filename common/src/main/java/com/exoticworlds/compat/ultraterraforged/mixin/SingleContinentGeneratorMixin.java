package com.exoticworlds.compat.ultraterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.ultraterraforged.LappedCenter;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.SimpleContinent;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.simple.SingleContinentGenerator;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

// The continent found at construction knows no lap; on a lap it is refound as the origin cell's canonical center.
@Mixin(value = SingleContinentGenerator.class, remap = false)
public abstract class SingleContinentGeneratorMixin {
    @Unique
    private static final String CENTER = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/continent/simple/"
            + "SingleContinentGenerator;center:Lcom/pandaismyname1/ultraterraforged/world/worldgen/noise/NoiseUtil$Vec2i;";

    @Unique
    private static final float ORIGIN = 0.0F;

    @Unique
    private volatile @Nullable LappedCenter exotic_worlds$lappedCenter;

    @ModifyExpressionValue(method = "apply", at = @At(value = "FIELD", target = CENTER, opcode = Opcodes.GETFIELD))
    private NoiseUtil.Vec2i exotic_worlds$canonicalCenter(NoiseUtil.Vec2i center) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            return center;
        }

        double xLap = frame.lap(Direction.Axis.X);
        double zLap = frame.lap(Direction.Axis.Z);
        double skew = frame.skew();
        LappedCenter held = this.exotic_worlds$lappedCenter;
        if (held != null && held.xLap() == xLap && held.zLap() == zLap && held.skew() == skew) {
            return held.center();
        }

        long packed = ((SimpleContinent) (Object) this).getNearestCenter(ORIGIN, ORIGIN);
        NoiseUtil.Vec2i lapped = new NoiseUtil.Vec2i(PosUtil.unpackLeft(packed), PosUtil.unpackRight(packed));
        this.exotic_worlds$lappedCenter = new LappedCenter(xLap, zLap, skew, lapped);
        return lapped;
    }
}
