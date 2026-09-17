package com.hbm.entity.missile;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.interfaces.StoredItems;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import net.minecraft.server.level.ServerPlayer;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import com.hbm.client.ClientEffects;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.util.ChunkUtil;
import com.hbm.lib.ModDamageSources;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.items.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 * 1.7 {@code entity/missile/EntitySoyuz} - the launch vehicle: rides its own acceleration straight up,
 * cooks anything under the plume, and past y=600 either orbits the satellite stack (mode 0) or drops an
 * {@link EntitySoyuzCapsule} lander onto the target column (mode 1).
 */
public class EntitySoyuz extends CompatEntity implements StoredItems {

    // 1.7 dataWatcher id 8, watchableObjectInt
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(EntitySoyuz.class, EntityDataSerializers.INT);
    private static final long NONE = Long.MIN_VALUE;

    double acceleration = 0.00D;
    public int mode;
    public int targetX;
    public int targetZ;
    boolean memed = false;
    // 18 slots like 1.7; EMPTY stands in for 1.7's null slots
    private final ItemStack[] payload = new ItemStack[18];

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < payload.length; i++) {
            if (visitor.visit(payload[i]) && payload[i].isEmpty()) payload[i] = ItemStack.EMPTY;
        }
    }
    // 1.7 loaded the landing chunk once pre-drop; forced-chunk window released on removal
    private long forcedChunk = NONE;

    public EntitySoyuz(EntityType<? extends EntitySoyuz> type, Level level) {
        super(type, level);
        Arrays.fill(this.payload, ItemStack.EMPTY);
        // 1.7 also set ignoreFrustumCheck here; render-side concerns moved to the renderer.
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(SKIN, 0);
    }

    @Override
    public void tick() {
        // 1.7 onUpdate replaces Entity.onUpdate wholesale - no super.tick(), no baseTick side effects

        Vec3 motion = getDeltaMovement();
        if (motion.y < 2.0D) {
            acceleration += 0.00025D;
            motion = new Vec3(motion.x, motion.y + acceleration, motion.z);
            setDeltaMovement(motion);
        }

        // 1.7 setLocationAndAngles(posX + motionX, posY + motionY, posZ + motionZ, 0, 0) - pinned upright
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setYRot(0.0F);
        setXRot(0.0F);

        if (level() instanceof ServerLevel server) {

            List<Entity> list = level().getEntities(this, new AABB(getX() - 5, getY() - 15, getZ() - 5,
                    getX() + 5, getY(), getZ() + 5));

            for (Entity e : list) {
                dev.golem.core.world.Effects.ignite(e, 15); // 1.7 setFire(15)

                // 1.7 attackEntityFrom: i-frames throttle the plume to one hit per 20 ticks
                Compat.hurt(e, server, ModDamageSources.source(level(), ModDamageSources.EXHAUST), 100.0F);

                if (e instanceof Player) {
                    if (!memed) {
                        memed = true;
                        level().playSound(null, getX(), getY(), getZ(), ModSounds.ALARM_SOYUZED.get(),
                                SoundSource.NEUTRAL, 100.0F, 1.0F);
                    }

                    if (e instanceof ServerPlayer serverPlayer) {
                        HbmCriteria.detonation(serverPlayer, DetonationTrigger.Kind.SOYUZ_EXHAUST);
                    }
                }
            }
        }

        if (level().isClientSide()) {
            spawnExhaust(getX(), getY(), getZ());
            spawnExhaust(getX() + 2.75, getY(), getZ());
            spawnExhaust(getX() - 2.75, getY(), getZ());
            spawnExhaust(getX(), getY(), getZ() + 2.75);
            spawnExhaust(getX(), getY(), getZ() - 2.75);
        }

        if (getY() > 600 && !level().isClientSide()) {
            deployPayload();
        }
    }

    // 1.7 effectNT "exhaust"/"soyuz": one rocket-flame per nozzle per tick, default scale 1,
    // maxAge 300 + rand(50), downward wash -0.75..-0.25, gaussian jitter through the nozzle
    // width rand*0.25-0.5 (the 350-block cull rode the removed effectNT packet)
    private void spawnExhaust(double x, double y, double z) {
        double width = random.nextDouble() * 0.25 - 0.5;
        ClientEffects.spawnRocketFlame(level(), x + random.nextGaussian() * width, y,
                z + random.nextGaussian() * width, 1.0F, 0.0D, -0.75D + random.nextDouble() * 0.5D, 0.0D,
                300 + random.nextInt(50));
    }

    private void deployPayload() {

        if (mode == 0) {

            ItemStack load = this.payload[0];

            if (!load.isEmpty()) {

                if (load.is(ModItems.flame_pony.get())) {
                    ExplosionLarge.spawnTracers(level(), getX(), getY(), getZ(), 25);
                    for (Player p : level().players()) {
                        if (p instanceof ServerPlayer serverPlayer) HbmCriteria.orbit(serverPlayer, load);
                    }
                }

                // 1.7:109-113 awards achFOEQ a second time here; SatelliteRelay.onOrbit below is the same
                // award on the same tick, so the port fires it once.

                // 1.7:115-120 - every satellite reaches orbit here, and nowhere else
                SatelliteType type = SatelliteType.fromStack(load);
                if (type != null && level() instanceof ServerLevel orbitLevel) {
                    Satellite.orbit(orbitLevel, type, ISatChip.getFreqS(load), getX(), getY(), getZ());
                }
            }
        }

        if (mode == 1) {

            EntitySoyuzCapsule capsule = new EntitySoyuzCapsule(ModNtmEntities.SOYUZ_CAPSULE.get(), level());
            capsule.payload = this.payload;
            capsule.soyuz = this.getSkin();
            capsule.setPos(targetX + 0.5, 600, targetZ + 0.5);

            // 1.7: provider.loadChunk(targetX >> 4, targetZ >> 4)
            if (level() instanceof ServerLevel server) {
                forcePayloadChunk(server, targetX >> 4, targetZ >> 4);
            }

            level().addFreshEntity(capsule);
        }

        this.discard(); // 1.7 setDead()
    }

    private void forcePayloadChunk(ServerLevel server, int cx, int cz) {
        long key = ChunkPos.pack(cx, cz);
        if (key == forcedChunk) return;
        releaseForcedChunk();
        ChunkUtil.claimForced(server, key);
        forcedChunk = key;
    }

    private void releaseForcedChunk() {
        if (forcedChunk != NONE && level() instanceof ServerLevel server) {
            ChunkUtil.releaseForced(server, forcedChunk);
        }
        forcedChunk = NONE;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseForcedChunk();
        super.remove(reason);
    }

    public void setSat(ItemStack stack) {
        this.payload[0] = stack == null ? ItemStack.EMPTY : stack;
    }

    public void setPayload(List<ItemStack> payload) {
        for (int i = 0; i < payload.size() && i < this.payload.length; i++) {
            ItemStack stack = payload.get(i);
            this.payload[i] = stack == null ? ItemStack.EMPTY : stack;
        }
    }

    public void setSkin(int i) {
        entityData.set(SKIN, i);
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        // 1.7 did not override attackEntityFrom, so it took the plain Entity default: not damageable.
        return false;
    }

    // 1.7 isInRangeToRenderDist(distance < 500000); the argument was already squared there too
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void loadData(DataIn input) {
        setSkin(input.getIntOr("skin", 0));
        targetX = input.getIntOr("targetX", targetX);
        targetZ = input.getIntOr("targetZ", targetZ);
        mode = input.getIntOr("mode", mode);

        // 1.7 "items" tag list with per-entry "slot" bytes; the codec list preserves slot order instead
        List<ItemStack> list = input.read("items", dev.golem.core.world.ItemStacks.optionalCodec().listOf()).orElse(List.of());
        for (int i = 0; i < list.size() && i < payload.length; i++) {
            payload[i] = list.get(i);
        }
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("skin", getSkin());
        output.putInt("targetX", targetX);
        output.putInt("targetZ", targetZ);
        output.putInt("mode", mode);

        output.store("items", dev.golem.core.world.ItemStacks.optionalCodec().listOf(), List.of(payload));
    }
}
