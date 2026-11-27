package com.exoticworlds.core.fold.cuboid;

import com.exoticworlds.core.fold.SeamTransform;

import java.util.ArrayList;
import java.util.List;

final class CuboidUnfoldings {
    private static final int NO_SIDE = -1;

    private final SeamTransform[][][] moves;

    private CuboidUnfoldings(SeamTransform[][][] moves) {
        this.moves = moves;
    }

    static CuboidUnfoldings of(CuboidFold.Crossing[][] crossings) {
        int faces = crossings.length;
        SeamTransform[][][] moves = new SeamTransform[faces][faces][];
        for (int anchor = 0; anchor < faces; anchor++) {
            for (int target = 0; target < faces; target++) {
                moves[anchor][target] = unfoldingsOf(crossings, anchor, target);
            }
        }

        return new CuboidUnfoldings(moves);
    }

    private static SeamTransform[] unfoldingsOf(CuboidFold.Crossing[][] crossings, int anchor, int target) {
        if (anchor == target) {
            return new SeamTransform[] {SeamTransform.IDENTITY};
        }

        List<SeamTransform> unfoldings = new ArrayList<>();
        int direct = sideToward(crossings[anchor], target);
        if (direct != NO_SIDE) {
            unfoldings.add(crossings[anchor][direct].transform().inverse());
        }

        for (int side = 0; side < CuboidFold.SIDES; side++) {
            CuboidFold.Crossing first = crossings[anchor][side];
            int onward = side == direct ? NO_SIDE : sideToward(crossings[first.face()], target);
            if (onward != NO_SIDE) {
                unfoldings.add(first.transform().then(crossings[first.face()][onward].transform()).inverse());
            }
        }

        return unmovedFirst(unfoldings);
    }

    private static int sideToward(CuboidFold.Crossing[] sides, int face) {
        for (int side = 0; side < sides.length; side++) {
            if (sides[side].face() == face) {
                return side;
            }
        }

        return NO_SIDE;
    }

    private static SeamTransform[] unmovedFirst(List<SeamTransform> unfoldings) {
        for (int index = 1; index < unfoldings.size(); index++) {
            if (unfoldings.get(index).isIdentity()) {
                unfoldings.add(0, unfoldings.remove(index));
            }
        }

        return unfoldings.toArray(SeamTransform[]::new);
    }

    SeamTransform nearest(int anchorFace, int targetFace, double anchorX, double anchorZ, double x, double z) {
        SeamTransform[] unfoldings = this.moves[anchorFace][targetFace];
        SeamTransform nearest = unfoldings[0];
        double nearestDistance = sqrDistance(nearest, anchorX, anchorZ, x, z);
        for (int index = 1; index < unfoldings.length; index++) {
            double distance = sqrDistance(unfoldings[index], anchorX, anchorZ, x, z);
            if (distance < nearestDistance) {
                nearest = unfoldings[index];
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static double sqrDistance(SeamTransform unfolding, double anchorX, double anchorZ, double x, double z) {
        double dx = unfolding.applyX(x, z) - anchorX;
        double dz = unfolding.applyZ(x, z) - anchorZ;
        return dx * dx + dz * dz;
    }
}
