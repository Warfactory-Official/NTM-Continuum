package com.hbm.physics;

/** old-hbm {@code physics.Contact}: one contact point and its normal and two friction constraints. */
public final class Contact {

    public final RigidBody bodyA;
    public final RigidBody bodyB;
    public final Collider a;
    public final Collider b;
    public final V3 localA;
    public final V3 localB;
    public V3 globalA;
    public V3 globalB;
    public final V3 normal;
    public float depth;
    public final V3 tangent;
    public final V3 bitangent;

    public V3 rA;
    public V3 rB;

    public final Jacobian normalContact;
    public final Jacobian tangentContact;
    public final Jacobian bitangentContact;

    public Contact(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b, GJK.GJKInfo info) {
        this.a = a;
        this.b = b;
        if (bodyA == null) bodyA = RigidBody.DUMMY;
        if (bodyB == null) bodyB = RigidBody.DUMMY;
        this.bodyA = bodyA;
        this.bodyB = bodyB;
        localA = bodyA.globalToLocalPos(info.contactPointA);
        localB = bodyB.globalToLocalPos(info.contactPointB);
        globalA = info.contactPointA;
        globalB = info.contactPointB;
        normal = info.normal;
        depth = info.depth;
        // https://box2d.org/posts/2014/02/computing-a-basis/
        if (Math.abs(normal.x()) >= 0.57735) {
            tangent = new V3(normal.y(), -normal.x(), 0).normalize();
        } else {
            tangent = new V3(0, normal.z(), -normal.y()).normalize();
        }
        bitangent = normal.crossProduct(tangent);
        normalContact = new Jacobian(false);
        tangentContact = new Jacobian(true);
        bitangentContact = new Jacobian(true);
    }

    public void init(float dt) {
        rA = globalA.subtract(bodyA == RigidBody.DUMMY ? a.localCentroid : bodyA.globalCentroid);
        rB = globalB.subtract(bodyB == RigidBody.DUMMY ? b.localCentroid : bodyB.globalCentroid);
        normalContact.init(this, normal, dt);
        tangentContact.init(this, tangent, dt);
        bitangentContact.init(this, bitangent, dt);
    }

    public void solve(float dt) {
        normalContact.solve(this, dt);
        tangentContact.solve(this, dt);
        bitangentContact.solve(this, dt);
    }

    public static final class Jacobian {

        final boolean tangent;
        V3 jVa, jWa, jVb, jWb;
        float bias;
        double effectiveMass;
        double totalLambda;

        Jacobian(boolean tangent) {
            this.tangent = tangent;
        }

        void init(Contact c, V3 dir, float dt) {
            jVa = dir.negate();
            jWa = c.rA.crossProduct(dir).negate();
            jVb = dir;
            jWb = c.rB.crossProduct(dir);
            if (!tangent) {
                float closingVel = (float) c.bodyA.linearVelocity.negate()
                        .subtract(c.bodyA.angularVelocity.crossProduct(c.rA))
                        .add(c.bodyB.linearVelocity)
                        .add(c.bodyB.angularVelocity.crossProduct(c.rB))
                        .dotProduct(c.normal);
                float restitution = c.bodyA.restitution * c.bodyB.restitution;
                float beta = 0.2F;
                float dslop = 0.0005F;
                float rslop = 0.5F;
                bias = -(beta / dt) * Math.max(c.depth - dslop, 0) + Math.max(restitution * closingVel - rslop, 0);
            }
            effectiveMass = c.bodyA.inv_mass
                    + jWa.dotProduct(jWa.matTransform(c.bodyA.inv_globalInertiaTensor))
                    + c.bodyB.inv_mass
                    + jWb.dotProduct(jWb.matTransform(c.bodyB.inv_globalInertiaTensor));
            effectiveMass = 1D / effectiveMass;
            totalLambda = 0;
        }

        void solve(Contact c, float dt) {
            double jv = jVa.dotProduct(c.bodyA.linearVelocity)
                    + jWa.dotProduct(c.bodyA.angularVelocity)
                    + jVb.dotProduct(c.bodyB.linearVelocity)
                    + jWb.dotProduct(c.bodyB.angularVelocity);
            double lambda = effectiveMass * (-(jv + bias));
            double oldTotalLambda = totalLambda;
            if (tangent) {
                float friction = c.bodyA.friction * c.bodyB.friction;
                double maxFriction = friction * c.normalContact.totalLambda;
                totalLambda = Math.max(-maxFriction, Math.min(maxFriction, totalLambda + lambda));
            } else {
                totalLambda = Math.max(0, oldTotalLambda + lambda);
            }
            lambda = totalLambda - oldTotalLambda;
            c.bodyA.addLinearVelocity(jVa.multd(c.bodyA.inv_mass * lambda));
            c.bodyA.addAngularVelocity(jWa.matTransform(c.bodyA.inv_globalInertiaTensor).multd(lambda));
            c.bodyB.addLinearVelocity(jVb.multd(c.bodyB.inv_mass * lambda));
            c.bodyB.addAngularVelocity(jWb.matTransform(c.bodyB.inv_globalInertiaTensor).multd(lambda));
        }
    }
}
