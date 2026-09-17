package com.hbm.blocks.rail;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** in retrospect, not the best name i could have chosen */
public interface IRailNTM {

    /** Returns a vector pointing to the closest snapping position given the starting position */
    default Vec3 getSnappingPos(Level level, int x, int y, int z, double trainX, double trainY, double trainZ) {
        RailContext info = new RailContext();
        if(!BlockRailNTM.railAt(level, x, y, z, info.owner)) return new Vec3(trainX, trainY, trainZ);
        getTravelLocation(level, x, y, z, trainX, trainY, trainZ, 0, 0, 0, 0, info,
                new MoveContext(RailCheckType.OTHER, 0));
        return new Vec3(info.x, info.y, info.z);
    }


    void getTravelLocation(Level level, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context);

    /** Returns that rail's gauge. Trains will derail if the gauge does not match. */
    TrackGauge getGauge(Level level, int x, int y, int z);

    enum TrackGauge {
        STANDARD,	//roughly 1.5m
        NARROW		//roughly 0.75m
    }

    final class RailContext {
        /** The angle at which the train ends up being on this rail */
        public float yaw;
        /** The amount of blocks still left to travel after completing the rail */
        public double overshoot;
        /** The exit position of that rail, valid only while {@link #hasPos} */
        public int posX, posY, posZ;
        public boolean hasPos;
        /** The position on this rail the train ends up at */
        public double x, y, z;
        /** The rail the current step is on, resolved by the walker so the rail block does not repeat the lookup */
        public final BlockRailNTM.Owner owner = new BlockRailNTM.Owner();

        /** Clears the hand-off; the position fields are always written before they are read. */
        public RailContext reset() { this.overshoot = 0; this.hasPos = false; return this; }
        public RailContext yaw(float y) { this.yaw = y; return this; }
        public RailContext dist(double d) { this.overshoot = d; return this; }
        public RailContext pos(int x, int y, int z) { this.posX = x; this.posY = y; this.posZ = z; this.hasPos = true; return this; }
        /** The rail's answer - 1.7's {@code return new Vec3(...)}. */
        public RailContext at(double x, double y, double z) { this.x = x; this.y = y; this.z = z; return this; }
    }

    /** A wrapper for additional information like stopping on rails and what type of check we're doing */
    final class MoveContext {
        public RailCheckType type;
        public double collisionBogieDistance;
        /** if a buffer stop or similar applies */
        public boolean collision = false;
        /** how much of the travel distance was cut short */
        public double overshoot;

        public MoveContext(RailCheckType type, double collisionBogieDistance) {
            set(type, collisionBogieDistance);
        }

        /** Re-arms a reused instance; the two output fields have to be cleared or a stale buffer hit carries over. */
        public MoveContext set(RailCheckType type, double collisionBogieDistance) {
            this.type = type;
            this.collisionBogieDistance = collisionBogieDistance;
            this.collision = false;
            this.overshoot = 0;
            return this;
        }
    }

    enum RailCheckType {
        CORE,
        FRONT,
        BACK,
        OTHER
    }
}
