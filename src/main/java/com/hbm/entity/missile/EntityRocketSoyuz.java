package com.hbm.entity.missile;

import com.hbm.items.ModMissileItems;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModNtmEntities;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnums;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageSources;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1.7 {@code EntityRocketSoyuz}: cooks anything under the plume, and past y=600 either orbits the satellite (mode 0)
 * or drops an {@link EntitySoyuzCapsule} lander onto the target column (mode 1).
 */
public class EntityRocketSoyuz extends EntityRocketBase {

    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(EntityRocketSoyuz.class, EntityDataSerializers.INT);
    private static final long NONE = Long.MIN_VALUE;

    public int mode;
    public int targetX;
    public int targetZ;
    boolean memed = false;
    // 1.7 loaded the landing chunk once before the drop; the forced chunk is released on removal
    private long forcedChunk = NONE;

    public EntityRocketSoyuz(EntityType<? extends EntityRocketSoyuz> type, Level level) {
        super(type, level, 18);
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(SKIN, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (level() instanceof ServerLevel server) {

            List<Entity> list = level().getEntities(this, new AABB(getX() - 5, getY() - 15, getZ() - 5,
                    getX() + 5, getY(), getZ() + 5));

            for (Entity e : list) {
                Effects.ignite(e, 15);
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
    }

    // 1.7 effectNT "exhaust"/"soyuz": one rocket flame per nozzle per tick, washed downward
    private void spawnExhaust(double x, double y, double z) {
        double width = random.nextDouble() * 0.25 - 0.5;
        ClientEffects.spawnRocketFlame(level(), x + random.nextGaussian() * width, y,
                z + random.nextGaussian() * width, 1.0F, 0.0D, -0.75D + random.nextDouble() * 0.5D, 0.0D,
                300 + random.nextInt(50));
    }

    @Override
    protected void deployPayload(ServerLevel level) {

        if (mode == 0) {
            ItemStack load = this.payload[0];

            if (!load.isEmpty()) {

                if (load.is(ModItems.flame_pony.get())) {
                    ExplosionLarge.spawnTracers(level, getX(), getY(), getZ(), 25);
                    // 1.7 achSpace, awarded to every player in the world; the trigger keys on the rocket
                    ItemStack rocket = ModMissileItems.missile_soyuz.stack(ItemEnums.SoyuzSkinType.NORMAL);
                    for (ServerPlayer p : level.players()) HbmCriteria.orbit(p, rocket);
                }

                if (load.getItem() instanceof ISatChip) {
                    Satellite.orbit(level, load, ISatChip.getFreqS(load), getX(), getY(), getZ());
                }
            }
        }

        if (mode == 1) {
            EntitySoyuzCapsule capsule = new EntitySoyuzCapsule(ModNtmEntities.SOYUZ_CAPSULE.get(), level);
            capsule.payload = this.payload;
            capsule.soyuz = this.getSkin();
            capsule.setPos(targetX + 0.5, 600, targetZ + 0.5);

            forcePayloadChunk(level, targetX >> 4, targetZ >> 4);
            level.addFreshEntity(capsule);
        }
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

    public void setSkin(int i) {
        entityData.set(SKIN, i);
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        setSkin(SavedIds.getEnum(input, "skin", ItemEnums.SoyuzSkinType.NORMAL).ordinal());
        targetX = input.getIntOr("targetX", targetX);
        targetZ = input.getIntOr("targetZ", targetZ);
        mode = input.getIntOr("mode", mode);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        SavedIds.putEnum(output, "skin", ItemEnums.SoyuzSkinType.VALUES[Math.floorMod(getSkin(), ItemEnums.SoyuzSkinType.VALUES.length)]);
        output.putInt("targetX", targetX);
        output.putInt("targetZ", targetZ);
        output.putInt("mode", mode);
    }
}
