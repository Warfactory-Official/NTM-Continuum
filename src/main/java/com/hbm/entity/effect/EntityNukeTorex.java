package com.hbm.entity.effect;

import dev.golem.core.data.CompatEntity;
import com.hbm.client.TorexClientEffects;
import com.hbm.entity.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

import java.util.ArrayList;

/**
 * Toroidal-convection mushroom-cloud effect entity (<b>Tor</b>oidal Convection Simulation <b>Ex</b>plosion).
 * A faithful port of 1.12.2's {@code EntityNukeTorex}: the entity is invisible server-side (it only ages
 * out at {@link #maxAge}); the whole visual simulation runs <em>client-side</em> in {@link #tick()},
 * spawning thousands of {@link Cloudlet} billboards that the {@code TorexRenderer} draws.
 *
 * <p>Cloudlet kinds ({@link TorexType}): STANDARD (the rising torus of convection cells), SHOCK (the
 * ground shockwave ring, first 150 ticks), RING (the collar around the core, first 200 ticks), and
 * CONDENSATION (the Wilson-cloud rings at heights 130/170, gated by biome humidity, first 220 ticks).
 *
 * <p>Client-only interactions (the sky flash, the detonation sound, biome humidity) are routed through
 * {@link TorexClientEffects} so a dedicated server never resolves {@code net.minecraft.client} symbols.
 * The scale + colour type ride {@link SynchedEntityData}. Not saved to disk ({@link #shouldBeSaved()}).
 */
public class EntityNukeTorex extends CompatEntity {

    public static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.BYTE);

    public static final int firstCondenseHeight = 130;
    public static final int secondCondenseHeight = 170;
    public static final int blastWaveHeadstart = 5;
    public static final int maxCloudlets = 20_000;

    // Nuke colors
    public static final double nr1 = 2.5;
    public static final double ng1 = 1.3;
    public static final double nb1 = 0.4;
    public static final double nr2 = 0.1;
    public static final double ng2 = 0.075;
    public static final double nb2 = 0.05;

    // Balefire colors
    public static final double br1 = 1;
    public static final double bg1 = 2;
    public static final double bb1 = 0.5;
    public static final double br2 = 0.1;
    public static final double bg2 = 0.1;
    public static final double bb2 = 0.1;

    public double coreHeight = 3;
    public double convectionHeight = 3;
    public double torusWidth = 3;
    public double rollerSize = 1;
    public double heat = 1;
    public double lastSpawnY = -1;
    public ArrayList<Cloudlet> cloudlets = new ArrayList<>();
    public int lastRenderSortTick = Integer.MIN_VALUE;
    public int maxAge = 1000;
    public float humidity = -1;

    public boolean didPlaySound = false;
    public boolean didShake = false;

    public EntityNukeTorex(EntityType<?> type, Level level) {
        super(type, level);
        // Fire immunity + the 20×40 bounding box come from the EntityType.Builder (see ModEntities).
    }

    @Override
    protected void defineData(dev.golem.core.data.SynchedData data) {
        data.define(SCALE, 1.0F);
        data.define(TYPE, (byte) 0);
    }

    @Override
    protected void loadData(DataIn input) {
        setScale(input.getFloatOr("scale", 1.0F));
        this.entityData.set(TYPE, input.getByteOr("type", (byte) 0));
    }

    @Override
    protected void saveData(DataOut output) {
        output.putFloat("scale", this.entityData.get(SCALE));
        output.putByte("type", this.entityData.get(TYPE));
    }

    /** The effect is transient — never persist it (old {@code writeToNBTOptional == false}). */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean onHurt(DamageSource source, float damage) {
        return false;
    }

    /** Render regardless of distance/frustum (old {@code isInRangeToRenderDist == true}). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {

            double s = this.getScale();
            double cs = 1.5;
            if (this.tickCount == 1) this.setScale((float) s);

            if (humidity == -1) humidity = TorexClientEffects.humidity(this);

            if (lastSpawnY == -1) {
                lastSpawnY = getY() - 3;
            }

            int spawnTarget = Math.max(level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(getX()), (int) Math.floor(getZ())) - 3, 1);
            double moveSpeed = 0.5D;

            if (Math.abs(spawnTarget - lastSpawnY) < moveSpeed) {
                lastSpawnY = spawnTarget;
            } else {
                lastSpawnY += moveSpeed * Math.signum(spawnTarget - lastSpawnY);
            }

            // spawn mush clouds
            double range = (torusWidth - rollerSize) * 0.5;
            double simSpeed = getSimulationSpeed();
            int lifetime = Math.min((this.tickCount * this.tickCount) + 200, maxAge - this.tickCount + 200);
            int toSpawn = (int) (0.6 * Math.min(Math.max(0, maxCloudlets - cloudlets.size()), Math.ceil(10 * simSpeed * simSpeed * Math.min(1, 1200 / (double) lifetime))));

            for (int i = 0; i < toSpawn; i++) {
                double x = getX() + random.nextGaussian() * range;
                double z = getZ() + random.nextGaussian() * range;
                Cloudlet cloud = new Cloudlet(x, lastSpawnY, z, (float) (random.nextDouble() * 2D * Math.PI), 0, lifetime);
                cloud.setScale((float) (Math.sqrt(s) * 3 + this.tickCount * 0.0025 * s), (float) (Math.sqrt(s) * 3 + this.tickCount * 0.0025 * 6 * cs * s));
                cloudlets.add(cloud);
            }

            if (this.tickCount < 120 * s) {
                TorexClientEffects.skyFlash(level());
            }

            // spawn shock clouds
            if (this.tickCount < 150) {

                int cloudCount = Math.min(this.tickCount * 2, 100);
                int shockLife = Math.max(400 - this.tickCount * 20, 50);

                for (int i = 0; i < cloudCount; i++) {
                    double vx = (this.tickCount + random.nextDouble() * 2) * 1.5;
                    float rot = (float) (Math.PI * 2 * random.nextDouble());
                    // rotate (vx, 0, 0) around Y by rot
                    double cos = Math.cos(rot);
                    double sin = Math.sin(rot);
                    double rx = vx * cos;
                    double rz = -vx * sin;
                    this.cloudlets.add(new Cloudlet(rx + getX(), level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) (rx + getX()) + 1, (int) (rz + getZ())), rz + getZ(), rot, 0, shockLife, TorexType.SHOCK)
                            .setScale((float) s * 5F, (float) s * 2F).setMotion(Mth.clamp(0.25 * this.tickCount - 5, 0, 1)));
                }

                if (!didPlaySound) {
                    didPlaySound = TorexClientEffects.tryPlayDetonationSound(this);
                }
            }

            // spawn ring clouds
            if (this.tickCount < 200) {
                lifetime *= s;
                for (int i = 0; i < 2; i++) {
                    Cloudlet cloud = new Cloudlet(getX(), getY() + coreHeight, getZ(), (float) (random.nextDouble() * 2D * Math.PI), 0, lifetime, TorexType.RING);
                    cloud.setScale((float) (Math.sqrt(s) * cs + this.tickCount * 0.0015 * s), (float) (Math.sqrt(s) * cs + this.tickCount * 0.0015 * 6 * cs * s));
                    cloudlets.add(cloud);
                }
            }

            if (this.humidity > 0 && this.tickCount < 220) {
                // spawn lower condensation clouds
                spawnCondensationClouds(this.tickCount, this.humidity, firstCondenseHeight, 80, 4, s, cs);
                // spawn upper condensation clouds
                spawnCondensationClouds(this.tickCount, this.humidity, secondCondenseHeight, 80, 2, s, cs);
            }

            for (int i = cloudlets.size() - 1; i >= 0; i--) {
                Cloudlet cloud = cloudlets.get(i);
                if (cloud.isDead) {
                    cloudlets.remove(i);
                    continue;
                }
                cloud.update();
            }

            coreHeight += 0.15;
            torusWidth += 0.05;
            rollerSize = torusWidth * 0.35;
            convectionHeight = coreHeight + rollerSize;

            int maxHeat = (int) (50 * s * s);
            heat = maxHeat - Math.pow((maxHeat * this.tickCount) / maxAge, 0.6);
        }

        if (!level().isClientSide() && this.tickCount > maxAge) {
            this.discard();
        }

        super.tick();
    }

    public void spawnCondensationClouds(int age, float humidity, int height, int count, int spreadAngle, double s, double cs) {
        if ((getY() + age) > height) {

            for (int i = 0; i < (int) (5 * humidity * count / (double) spreadAngle); i++) {
                for (int j = 1; j < spreadAngle; j++) {
                    float angle = (float) (Math.PI * 2 * random.nextDouble());
                    // vec = (0, age, 0) rotated around Z then around Y
                    double vy = age;
                    double vx = 0;
                    float zRot = (float) Math.acos((height - getY()) / (age)) + (float) Math.toRadians(humidity * humidity * 90 * j * (0.1 * random.nextDouble() - 0.05));
                    // rotateAroundZ(zRot): x' = x*cos + y*sin ; y' = y*cos - x*sin
                    double zc = Math.cos(zRot);
                    double zs = Math.sin(zRot);
                    double rx = vx * zc + vy * zs;
                    double ry = vy * zc - vx * zs;
                    double rz = 0;
                    // rotateAroundY(angle): x' = x*cos + z*sin ; z' = z*cos - x*sin
                    double yc = Math.cos(angle);
                    double ys = Math.sin(angle);
                    double fx = rx * yc + rz * ys;
                    double fz = rz * yc - rx * ys;
                    Cloudlet cloud = new Cloudlet(getX() + fx, getY() + ry, getZ() + fz, angle, 0, (int) ((20 + age / 10) * (1 + random.nextDouble() * 0.1)), TorexType.CONDENSATION);
                    cloud.setScale(3F * (float) (cs * s), 4F * (float) (cs * s));
                    cloudlets.add(cloud);
                }
            }
        }
    }

    public EntityNukeTorex setScale(float scale) {
        if (!level().isClientSide())
            this.entityData.set(SCALE, scale);
        this.coreHeight = this.coreHeight * scale;
        this.convectionHeight = this.convectionHeight * scale;
        this.torusWidth = this.torusWidth * scale;
        this.rollerSize = this.rollerSize * scale;
        this.maxAge = (int) (45 * 20 * scale);
        return this;
    }

    public EntityNukeTorex setType(int type) {
        this.entityData.set(TYPE, (byte) type);
        return this;
    }

    public double getScale() {
        return this.entityData.get(SCALE);
    }

    /** The colour scheme: 0 = nuke (orange), 1 = balefire (green). Named to avoid {@code Entity.getType()}. */
    public byte getTorexType() {
        return this.entityData.get(TYPE);
    }

    public double getSimulationSpeed() {

        int simSlow = maxAge / 4;
        int life = this.tickCount;

        if (life > maxAge) {
            return 0D;
        }

        if (life > simSlow) {
            return 1D - ((double) (life - simSlow) / (double) (maxAge - simSlow));
        }

        return 1.0D;
    }

    public float getAlpha() {

        int fadeOut = maxAge * 3 / 4;
        int life = this.tickCount;

        if (life > fadeOut) {
            float fac = (float) (life - fadeOut) / (float) (maxAge - fadeOut);
            return 1F - fac;
        }

        return 1.0F;
    }

    public class Cloudlet {

        public double posX;
        public double posY;
        public double posZ;
        public double prevPosX;
        public double prevPosY;
        public double prevPosZ;
        public double motionX;
        public double motionY;
        public double motionZ;
        public int age;
        public int cloudletLife;
        public float angle;
        public boolean isDead = false;
        float rangeMod = 1.0F;
        public float colorMod = 1.0F;
        public double colorR;
        public double colorG;
        public double colorB;
        public double prevColorR;
        public double prevColorG;
        public double prevColorB;
        public double renderSortDistanceSq;
        public TorexType type;
        public float startingScale = 3F;
        public float growingScale = 5F;
        private double computedMotionX;
        private double computedMotionY;
        private double computedMotionZ;

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge) {
            this(posX, posY, posZ, angle, age, maxAge, TorexType.STANDARD);
        }

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge, TorexType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.age = age;
            this.cloudletLife = maxAge;
            this.angle = angle;
            this.rangeMod = 0.3F + random.nextFloat() * 0.7F;
            this.colorMod = 0.8F + random.nextFloat() * 0.2F;
            this.type = type;

            this.updateColor();
        }

        private double motionMult = 1F;
        private double motionConvectionMult = 0.5F;
        private double motionLiftMult = 0.625F;
        private double motionRingMult = 0.5F;
        private double motionCondensationMult = 1F;
        private double motionShockwaveMult = 1F;

        private void update() {
            age++;

            if (age > cloudletLife) {
                this.isDead = true;
            }

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            double simDeltaX = EntityNukeTorex.this.getX() - this.posX;
            double simDeltaZ = EntityNukeTorex.this.getZ() - this.posZ;
            double simPosX = EntityNukeTorex.this.getX() + Math.sqrt(simDeltaX * simDeltaX + simDeltaZ * simDeltaZ);

            if (this.type == TorexType.STANDARD) {
                getConvectionMotion(simPosX);
                double convectionX = this.computedMotionX;
                double convectionY = this.computedMotionY;
                double convectionZ = this.computedMotionZ;
                getLiftMotion(simPosX);

                double factor = Mth.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0, 1);
                double inverseFactor = 1D - factor;
                this.motionX = convectionX * factor + this.computedMotionX * inverseFactor;
                this.motionY = convectionY * factor + this.computedMotionY * inverseFactor;
                this.motionZ = convectionZ * factor + this.computedMotionZ * inverseFactor;
            } else if (this.type == TorexType.RING) {
                getRingMotion(simPosX);
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            } else if (this.type == TorexType.CONDENSATION) {
                getCondensationMotion();
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            } else if (this.type == TorexType.SHOCK) {
                getShockwaveMotion();
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            }

            double mult = this.motionMult * getSimulationSpeed();

            this.posX += this.motionX * mult;
            this.posY += this.motionY * mult;
            this.posZ += this.motionZ * mult;

            this.updateColor();
        }

        private void getCondensationMotion() {
            double speed = motionCondensationMult * EntityNukeTorex.this.getScale() * 0.125D;
            setNormalizedMotion(this.posX - EntityNukeTorex.this.getX(), 0D, this.posZ - EntityNukeTorex.this.getZ(), speed);
        }

        private void getShockwaveMotion() {
            double speed = motionShockwaveMult * EntityNukeTorex.this.getScale() * 0.25D;
            setNormalizedMotion(this.posX - EntityNukeTorex.this.getX(), 0D, this.posZ - EntityNukeTorex.this.getZ(), speed);
        }

        private void getRingMotion(double simPosX) {

            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double torusPosX = EntityNukeTorex.this.getX() + torusWidth;
            double torusPosY = EntityNukeTorex.this.getY() + coreHeight * 0.5D;

            double deltaX = torusPosX - simPosX;
            double deltaY = torusPosY - this.posY;

            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod * 0.25D;
            double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / roller - 1D;

            double func = 1D - Math.exp(-dist);
            float angle = (float) (func * Math.PI * 0.5D); // [0;90°]

            double rotX = -deltaX / dist;
            double rotY = -deltaY / dist;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            double rotatedX = rotX * cos + rotY * sin;
            double rotatedY = rotY * cos - rotX * sin;

            setNormalizedMotion(torusPosX + rotatedX - simPosX, torusPosY + rotatedY - this.posY, 0D, motionRingMult * 0.5D);
            rotateComputedMotionAroundY();
        }

        /* simulated on a 2D-plane along the X/Y axis */
        private void getConvectionMotion(double simPosX) {

            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double torusPosX = EntityNukeTorex.this.getX() + torusWidth;
            double torusPosY = EntityNukeTorex.this.getY() + coreHeight;

            double deltaX = torusPosX - simPosX;
            double deltaY = torusPosY - this.posY;

            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod;
            double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / roller - 1D;

            double func = 1D - Math.exp(-dist);
            float angle = (float) (func * Math.PI * 0.5D); // [0;90°]

            double rotX = -deltaX / dist;
            double rotY = -deltaY / dist;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            double rotatedX = rotX * cos + rotY * sin;
            double rotatedY = rotY * cos - rotX * sin;

            setNormalizedMotion(torusPosX + rotatedX - simPosX, torusPosY + rotatedY - this.posY, 0D, motionConvectionMult);
            rotateComputedMotionAroundY();
        }

        private void getLiftMotion(double simPosX) {
            double scale = Mth.clamp(1D - (simPosX - (EntityNukeTorex.this.getX() + torusWidth)), 0, 1) * motionLiftMult;

            setNormalizedMotion(
                    EntityNukeTorex.this.getX() - this.posX,
                    (EntityNukeTorex.this.getY() + convectionHeight) - this.posY,
                    EntityNukeTorex.this.getZ() - this.posZ,
                    scale);
        }

        private void setComputedMotion(double x, double y, double z) {
            this.computedMotionX = x;
            this.computedMotionY = y;
            this.computedMotionZ = z;
        }

        private void setNormalizedMotion(double x, double y, double z, double speed) {
            double lengthSq = x * x + y * y + z * z;
            if (lengthSq < 1.0E-8D) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double scale = speed / Math.sqrt(lengthSq);
            setComputedMotion(x * scale, y * scale, z * scale);
        }

        private void rotateComputedMotionAroundY() {
            float cos = Mth.cos(this.angle);
            float sin = Mth.sin(this.angle);
            double motionX = this.computedMotionX;
            double motionZ = this.computedMotionZ;
            this.computedMotionX = motionX * cos + motionZ * sin;
            this.computedMotionZ = motionZ * cos - motionX * sin;
        }

        private void updateColor() {
            this.prevColorR = this.colorR;
            this.prevColorG = this.colorG;
            this.prevColorB = this.colorB;

            double exX = EntityNukeTorex.this.getX();
            double exY = EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight;
            double exZ = EntityNukeTorex.this.getZ();

            double distX = exX - posX;
            double distY = exY - posY;
            double distZ = exZ - posZ;

            double distSq = distX * distX + distY * distY + distZ * distZ;
            distSq /= this.type == TorexType.SHOCK ? EntityNukeTorex.this.heat * 3 : EntityNukeTorex.this.heat;

            double col = 2D / Math.max(distSq, 1); // col goes from 2-0

            byte type = EntityNukeTorex.this.getTorexType();
            if (type == 0) {
                this.colorR = nr2 + (nr1 - nr2) * col;
                this.colorG = ng2 + (ng1 - ng2) * col;
                this.colorB = nb2 + (nb1 - nb2) * col;
                return;
            }

            this.colorR = br2 + (br1 - br2) * col;
            this.colorG = bg2 + (bg1 - bg2) * col;
            this.colorB = bb2 + (bb1 - bb2) * col;
        }

        public float getAlpha() {
            float alpha = (1F - ((float) age / (float) cloudletLife)) * EntityNukeTorex.this.getAlpha();
            if (this.type == TorexType.CONDENSATION) alpha *= 0.25;
            return Mth.clamp(alpha, 0.0001F, 1F);
        }

        public float getScale() {
            return startingScale + ((float) age / (float) cloudletLife) * growingScale;
        }

        public Cloudlet setScale(float start, float grow) {
            this.startingScale = start;
            this.growingScale = grow;
            return this;
        }

        public Cloudlet setMotion(double mult) {
            this.motionMult = mult;
            return this;
        }
    }

    public enum TorexType {
        STANDARD,
        RING,
        CONDENSATION,
        SHOCK
    }

    /** Builds (but does not spawn) a standard Torex; the caller adds it to the level. */
    public static EntityNukeTorex statFac(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), level).setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F));
        torex.setPos(x, y, z);
        return torex;
    }

    /** Builds (but does not spawn) a balefire Torex; the caller adds it to the level. */
    public static EntityNukeTorex statFacBale(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), level).setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F)).setType(1);
        torex.setPos(x, y, z);
        return torex;
    }
}
