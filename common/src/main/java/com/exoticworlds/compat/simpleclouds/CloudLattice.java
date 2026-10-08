package com.exoticworlds.compat.simpleclouds;

public record CloudLattice(float lapAX, float lapAZ, float lapBX, float lapBZ, float inverse00, float inverse01,
        float inverse10, float inverse11) {
    public static final CloudLattice NONE = new CloudLattice(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public static final int PACKED_SIZE = 8;

    private static final float UNBOUNDED = 0.0F;
    private static final int MAX_REDUCTION_STEPS = 64;
    private static final int NEIGHBOURS = 1;

    public static CloudLattice of(float lapX, float lapZ, float m00, float m01, float m10, float m11) {
        if (lapX == UNBOUNDED && lapZ == UNBOUNDED) {
            return NONE;
        }

        if (lapX == UNBOUNDED || lapZ == UNBOUNDED) {
            float tx = m00 * lapX + m10 * lapZ;
            float tz = m01 * lapX + m11 * lapZ;
            float squared = tx * tx + tz * tz;
            return new CloudLattice(lapX, lapZ, 0.0F, 0.0F, tx / squared, 0.0F, tz / squared, 0.0F);
        }

        float ax = lapX;
        float az = 0.0F;
        float bx = 0.0F;
        float bz = lapZ;
        for (int step = 0; step < MAX_REDUCTION_STEPS; step++) {
            if (squaredImage(ax, az, m00, m01, m10, m11) > squaredImage(bx, bz, m00, m01, m10, m11)) {
                float swapX = ax;
                float swapZ = az;
                ax = bx;
                az = bz;
                bx = swapX;
                bz = swapZ;
            }

            float t1x = m00 * ax + m10 * az;
            float t1z = m01 * ax + m11 * az;
            float t2x = m00 * bx + m10 * bz;
            float t2z = m01 * bx + m11 * bz;
            int mu = Math.round((t1x * t2x + t1z * t2z) / (t1x * t1x + t1z * t1z));
            if (mu == 0) {
                break;
            }

            bx -= mu * ax;
            bz -= mu * az;
        }

        float t1x = m00 * ax + m10 * az;
        float t1z = m01 * ax + m11 * az;
        float t2x = m00 * bx + m10 * bz;
        float t2z = m01 * bx + m11 * bz;
        float determinant = t1x * t2z - t2x * t1z;
        return new CloudLattice(ax, az, bx, bz, t2z / determinant, -t1z / determinant, -t2x / determinant,
                t1x / determinant);
    }

    public float seatedX(float posX, float posZ, float m00, float m01, float m10, float m11, float x, float z) {
        return seat(posX, posZ, m00, m01, m10, m11, x, z, true);
    }

    public float seatedZ(float posX, float posZ, float m00, float m01, float m10, float m11, float x, float z) {
        return seat(posX, posZ, m00, m01, m10, m11, x, z, false);
    }

    public void pack(float[] target, int offset) {
        target[offset] = this.lapAX;
        target[offset + 1] = this.lapAZ;
        target[offset + 2] = this.lapBX;
        target[offset + 3] = this.lapBZ;
        target[offset + 4] = this.inverse00;
        target[offset + 5] = this.inverse01;
        target[offset + 6] = this.inverse10;
        target[offset + 7] = this.inverse11;
    }

    // Ties break on the seated position, so the X and Z reads of one query pick the same copy from either lap.
    private float seat(float posX, float posZ, float m00, float m01, float m10, float m11, float x, float z,
            boolean wantX) {
        float vx = x - posX;
        float vz = z - posZ;
        float ux = m00 * vx + m10 * vz;
        float uz = m01 * vx + m11 * vz;
        int baseA = -Math.round(this.inverse00 * ux + this.inverse10 * uz);
        int baseB = -Math.round(this.inverse01 * ux + this.inverse11 * uz);
        float bestX = x;
        float bestZ = z;
        float bestLength = Float.POSITIVE_INFINITY;
        for (int i = -NEIGHBOURS; i <= NEIGHBOURS; i++) {
            for (int j = -NEIGHBOURS; j <= NEIGHBOURS; j++) {
                int a = baseA + i;
                int b = baseB + j;
                float seatedX = x + a * this.lapAX + b * this.lapBX;
                float seatedZ = z + a * this.lapAZ + b * this.lapBZ;
                float length = squaredImage(seatedX - posX, seatedZ - posZ, m00, m01, m10, m11);
                if (length < bestLength || length == bestLength
                        && (seatedX < bestX || seatedX == bestX && seatedZ < bestZ)) {
                    bestX = seatedX;
                    bestZ = seatedZ;
                    bestLength = length;
                }
            }
        }

        return wantX ? bestX : bestZ;
    }

    private static float squaredImage(float x, float z, float m00, float m01, float m10, float m11) {
        float tx = m00 * x + m10 * z;
        float tz = m01 * x + m11 * z;
        return tx * tx + tz * tz;
    }
}
