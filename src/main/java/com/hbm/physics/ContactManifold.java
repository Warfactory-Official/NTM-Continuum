package com.hbm.physics;

/** 1.12.2 {@code physics.ContactManifold}: up to four persistent contacts, refreshed and pruned every step. */
public final class ContactManifold {

    public static final float CONTACT_BREAK = 0.02F;
    public static final float CONTACT_BREAK_SQ = CONTACT_BREAK * CONTACT_BREAK;

    public final Contact[] contacts = new Contact[4];
    public int contactCount = 0;

    public void update() {
        for (int i = 0; i < contactCount; i++) {
            Contact c = contacts[i];
            c.globalA = c.bodyA.localToGlobalPos(c.localA);
            c.globalB = c.bodyB.localToGlobalPos(c.localB);
            contacts[i].depth = (float) c.globalA.subtract(c.globalB).dotProduct(c.normal);
        }
        for (int i = 0; i < contactCount; i++) {
            Contact c = contacts[i];
            if (c.depth > CONTACT_BREAK) {
                removeContact(i);
                i--;
            } else {
                V3 proj = c.globalA.subtract(c.normal.mult(c.depth));
                double orthoDistToB = proj.subtract(c.globalB).lengthSquared();
                if (orthoDistToB > CONTACT_BREAK_SQ) {
                    removeContact(i);
                    i--;
                }
            }
        }
    }

    public void removeContact(int idx) {
        contacts[idx] = null;
        for (int i = idx; i < 3; i++) {
            contacts[i] = contacts[i + 1];
            contacts[i + 1] = null;
        }
        contactCount--;
    }

    public boolean addContact(Contact c) {
        int idx = getContactIndex(c);
        boolean replace = true;
        if (idx < 0) {
            if (contactCount < 4) {
                idx = contactCount;
                replace = false;
            } else {
                idx = getLeastRemoteIndex(c);
            }
        }
        if (idx >= 0) {
            if (!replace) contactCount++;
            contacts[idx] = c;
            return true;
        }
        return false;
    }

    /** Bullet's rule, as the old class: drop the point whose removal loses the least area, never the deepest. */
    public int getLeastRemoteIndex(Contact c) {
        float deepest = -Float.MAX_VALUE;
        int deepIdx = -1;
        for (int i = 0; i < contactCount; i++) {
            if (contacts[i].depth > deepest) {
                deepest = contacts[i].depth;
                deepIdx = i;
            }
        }
        double res0 = 0, res1 = 0, res2 = 0, res3 = 0;
        if (deepIdx != 0) {
            V3 a = c.localA.subtract(contacts[1].localA);
            V3 b = contacts[3].localA.subtract(contacts[2].localA);
            res0 = a.crossProduct(b).lengthSquared();
        }
        if (deepIdx != 1) {
            V3 a = c.localA.subtract(contacts[0].localA);
            V3 b = contacts[3].localA.subtract(contacts[2].localA);
            res1 = a.crossProduct(b).lengthSquared();
        }
        if (deepIdx != 2) {
            V3 a = c.localA.subtract(contacts[0].localA);
            V3 b = contacts[3].localA.subtract(contacts[1].localA);
            res2 = a.crossProduct(b).lengthSquared();
        }
        if (deepIdx != 3) {
            V3 a = c.localA.subtract(contacts[0].localA);
            V3 b = contacts[2].localA.subtract(contacts[1].localA);
            res3 = a.crossProduct(b).lengthSquared();
        }
        return absMaxIdx(res0, res1, res2, res3);
    }

    public int getContactIndex(Contact c) {
        int idx = -1;
        double shortestDist = CONTACT_BREAK_SQ;
        for (int i = 0; i < contactCount; i++) {
            double dist = contacts[i].localA.subtract(c.localA).lengthSquared();
            if (dist < shortestDist) {
                shortestDist = dist;
                idx = i;
            }
        }
        return idx;
    }

    /** {@code BobMathUtil.absMaxIdx}. */
    static int absMaxIdx(double... numbers) {
        int idx = 0;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < numbers.length; i++) {
            double num = Math.abs(numbers[i]);
            if (num > max) {
                idx = i;
                max = num;
            }
        }
        return idx;
    }
}
