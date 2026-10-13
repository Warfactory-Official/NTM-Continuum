package com.hbm.physics;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.2 {@code physics.RigidBody}: a convex-compound rigid body stepped eight times per tick against the world's
 * block boxes (sequential-impulse contacts, gravity 9.81). Ported statement for statement; the vecmath types are
 * {@link Mat3}/{@link Quat}, the world is a {@link CollisionSource}. {@link #renderMatrix} is {@code doGlTransform}.
 */
public class RigidBody {

    public static final V3[] cardinals = {
            new V3(1, 0, 0), new V3(0, 1, 0), new V3(0, 0, 1),
            new V3(-1, 0, 0), new V3(0, -1, 0), new V3(0, 0, -1)};

    /** The static partner of every world contact: infinite mass, never moves. */
    public static final RigidBody DUMMY = new RigidBody(null) {
        @Override public void solveContacts(float dt) {}
        @Override public void updateOrientation() {}
        @Override public void updateGlobalCentroidFromPosition() {}
        @Override public void updatePositionFromGlobalCentroid() {}
        @Override public void doTimeStep(float dt) {}
        @Override public void addColliders(Collider... collide) {}
        @Override public V3 globalToLocalPos(V3 pos) { return pos; }
        @Override public V3 localToGlobalPos(V3 pos) { return pos; }
        @Override public V3 globalToLocalVec(V3 vec) { return vec; }
        @Override public V3 localToGlobalVec(V3 vec) { return vec; }
        @Override public void addLinearVelocity(V3 v) {}
        @Override public void addAngularVelocity(V3 v) {}
    };

    static {
        DUMMY.inv_rotation = DUMMY.rotation.copy();
        DUMMY.localInertiaTensor = new Mat3();
        DUMMY.inv_localInertiaTensor = new Mat3();
        DUMMY.inv_globalInertiaTensor = new Mat3();
        DUMMY.localCentroid = V3.ZERO;
        DUMMY.globalCentroid = V3.ZERO;
    }

    public final @Nullable CollisionSource world;
    public Box boundingBox;

    public final List<Collider> colliders = new ArrayList<>();
    public final List<Box> colliderBoundingBoxes = new ArrayList<>();

    public V3 position = V3.ZERO;
    public V3 globalCentroid;
    public Mat3 rotation = new Mat3();
    public Mat3 inv_rotation;

    public V3 prevPosition = V3.ZERO;
    public final Quat prevRotation = new Quat();

    public V3 linearVelocity = V3.ZERO;
    public V3 angularVelocity = V3.ZERO;
    public V3 force = V3.ZERO;
    public V3 torque = V3.ZERO;

    public float mass;
    public float inv_mass;
    public Mat3 localInertiaTensor;
    public Mat3 inv_localInertiaTensor;
    public Mat3 inv_globalInertiaTensor;
    public float friction = 0.5F;
    public float restitution = 0;

    public V3 localCentroid;

    public final ContactManifold contacts = new ContactManifold();

    public RigidBody(@Nullable CollisionSource world) {
        this.world = world;
        rotation.setIdentity();
    }

    public RigidBody(@Nullable CollisionSource world, double x, double y, double z) {
        this(world);
        this.position = new V3(x, y, z);
    }

    /** One Minecraft tick: eight sub-steps of 1/160 s. */
    public void minecraftTimestep() {
        setPrevData();
        int timeStepSubDiv = 8;
        float step = 0.05F / (float) timeStepSubDiv;
        for (int i = 0; i < timeStepSubDiv; i++) doTimeStep(step);
    }

    public void doTimeStep(float dt) {
        contacts.update();
        List<Box> boxes = world == null ? List.of() : world.collisionBoxes(boundingBox);
        for (Box box : boxes) {
            for (int i = 0; i < colliders.size(); i++) {
                Collider c = colliders.get(i);
                if (!colliderBoundingBoxes.get(i).intersects(box)) continue;
                Collider b = new AABBCollider(box);
                GJK.GJKInfo info = GJK.colliding(this, null, c, b);
                if (info.result == GJK.Result.COLLIDING) contacts.addContact(new Contact(this, null, c, b, info));
            }
        }
        solveContacts(dt);
        integrateVelocityAndPosition(dt);
    }

    public void integrateVelocityAndPosition(float dt) {
        linearVelocity = linearVelocity.add(force.mult(inv_mass * dt));
        angularVelocity = angularVelocity.add(torque.mult(dt).matTransform(inv_globalInertiaTensor));
        force = V3.ZERO;
        torque = V3.ZERO;

        globalCentroid = globalCentroid.add(linearVelocity.mult(dt));
        if (angularVelocity.lengthSquared() > 0) {
            V3 axis = angularVelocity.normalize();
            double angle = angularVelocity.length() * dt;
            Mat3 turn = new Mat3();
            turn.setAxisAngle((float) axis.x(), (float) axis.y(), (float) axis.z(), (float) angle);
            turn.mul(rotation);
            rotation = turn;
            updateOrientation();
        }
        updatePositionFromGlobalCentroid();
        updateAABBs();
        addLinearVelocity(new V3(0, -9.81 * dt, 0));
    }

    public void setPrevData() {
        this.prevPosition = position;
        prevRotation.setFromMat(rotation);
    }

    public void solveContacts(float dt) {
        for (int j = 0; j < contacts.contactCount; j++) contacts.contacts[j].init(dt);
        int velocityIterations = 4;
        for (int i = 0; i < velocityIterations; i++) {
            for (int j = 0; j < contacts.contactCount; j++) contacts.contacts[j].solve(dt);
        }
    }

    public V3 localToGlobalPos(V3 pos) { return pos.matTransform(rotation).add(position); }

    public V3 globalToLocalPos(V3 pos) { return pos.subtract(position).matTransform(inv_rotation); }

    public V3 localToGlobalVec(V3 vec) { return vec.matTransform(rotation); }

    public V3 globalToLocalVec(V3 vec) { return vec.matTransform(inv_rotation); }

    public void addLinearVelocity(V3 v) { linearVelocity = linearVelocity.add(v); }

    public void addAngularVelocity(V3 v) { angularVelocity = angularVelocity.add(v); }

    public void impulseVelocityDirect(V3 force, V3 position) {
        linearVelocity = linearVelocity.add(force);
        angularVelocity = angularVelocity.add(position.subtract(globalCentroid).crossProduct(force));
    }

    public void updateOrientation() {
        Quat quat = new Quat();
        quat.setFromMat(rotation);
        quat.normalize();
        rotation.setFromQuat(quat);
        inv_rotation = rotation.copy();
        inv_rotation.transpose();
        inv_globalInertiaTensor.set(inv_rotation);
        inv_globalInertiaTensor.mul(inv_localInertiaTensor);
        inv_globalInertiaTensor.mul(rotation);
    }

    public void updatePositionFromGlobalCentroid() {
        position = globalCentroid.add(localCentroid.mult(-1).matTransform(rotation));
    }

    public void updateGlobalCentroidFromPosition() {
        globalCentroid = localCentroid.matTransform(rotation).add(position);
    }

    public void updateAABBs() {
        colliderBoundingBoxes.clear();
        double tMaxX, tMaxY, tMaxZ, tMinX, tMinY, tMinZ;
        tMaxX = tMaxY = tMaxZ = -Double.MAX_VALUE;
        tMinX = tMinY = tMinZ = Double.MAX_VALUE;
        for (Collider c : colliders) {
            double maxX = GJK.localSupport(this, c, cardinals[0]).x();
            double maxY = GJK.localSupport(this, c, cardinals[1]).y();
            double maxZ = GJK.localSupport(this, c, cardinals[2]).z();
            double minX = GJK.localSupport(this, c, cardinals[3]).x();
            double minY = GJK.localSupport(this, c, cardinals[4]).y();
            double minZ = GJK.localSupport(this, c, cardinals[5]).z();
            colliderBoundingBoxes.add(Box.of(minX, minY, minZ, maxX, maxY, maxZ));
            tMaxX = Math.max(tMaxX, maxX);
            tMaxY = Math.max(tMaxY, maxY);
            tMaxZ = Math.max(tMaxZ, maxZ);
            tMinX = Math.min(tMinX, minX);
            tMinY = Math.min(tMinY, minY);
            tMinZ = Math.min(tMinZ, minZ);
        }
        boundingBox = Box.of(tMinX, tMinY, tMinZ, tMaxX, tMaxY, tMaxZ);
    }

    public void addColliders(Collider... collide) {
        for (Collider c : collide) colliders.add(c);
        localCentroid = V3.ZERO;
        mass = 0;
        for (Collider c : colliders) {
            mass += c.mass;
            localCentroid = localCentroid.add(c.localCentroid.mult(c.mass));
        }
        inv_mass = 1F / mass;
        localCentroid = localCentroid.mult(inv_mass);

        localInertiaTensor = new Mat3();
        for (Collider c : colliders) {
            // https://en.wikipedia.org/wiki/Parallel_axis_theorem
            V3 colliderToLocal = localCentroid.subtract(c.localCentroid);
            double lenSquared = colliderToLocal.dotProduct(colliderToLocal);
            Mat3 outerProduct = colliderToLocal.outerProduct(colliderToLocal);
            Mat3 colliderToLocalMat = new Mat3();
            colliderToLocalMat.setIdentity();
            colliderToLocalMat.mul((float) lenSquared);
            colliderToLocalMat.sub(outerProduct);
            colliderToLocalMat.mul(c.mass);
            Mat3 cLocalIT = c.localInertiaTensor.copy();
            cLocalIT.add(colliderToLocalMat);
            localInertiaTensor.add(cLocalIT);
        }
        inv_localInertiaTensor = localInertiaTensor.copy();
        inv_localInertiaTensor.invert();
        inv_globalInertiaTensor = new Mat3();
        updateOrientation();
        updateGlobalCentroidFromPosition();
        this.prevPosition = position;
        updateAABBs();
    }

    /**
     * {@code doGlTransform}: the rotation slerped from this tick's back towards the last ({@code Quat4f.interpolate(prev,
     * 1 - partialTicks)}) and the interpolated position less {@code camera}.
     */
    public Matrix4f renderMatrix(double cameraX, double cameraY, double cameraZ, float partialTicks) {
        Quat quat = new Quat();
        quat.setFromMat(rotation);
        quat.interpolate(prevRotation, 1 - partialTicks);
        quat.normalize();
        Mat3 r = new Mat3();
        r.setFromQuat(quat);
        double px = prevPosition.x() + (position.x() - prevPosition.x()) * partialTicks - cameraX;
        double py = prevPosition.y() + (position.y() - prevPosition.y()) * partialTicks - cameraY;
        double pz = prevPosition.z() + (position.z() - prevPosition.z()) * partialTicks - cameraZ;
        return new Matrix4f(
                r.m00, r.m10, r.m20, 0,
                r.m01, r.m11, r.m21, 0,
                r.m02, r.m12, r.m22, 0,
                (float) px, (float) py, (float) pz, 1);
    }
}
