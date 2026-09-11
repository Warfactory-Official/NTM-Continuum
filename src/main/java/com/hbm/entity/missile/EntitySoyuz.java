package com.hbm.entity.missile;

import com.hbm.entity.ModEntities;
import com.hbm.items.ISatChip;
import com.hbm.lib.ModDamageSources;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteRegistry;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Effects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The satellite launch vehicle in flight. It accelerates straight up until it is past six hundred, then
 * hands its payload to orbit on the frequency the satellite carries and disappears.
 *
 * <p>Deviations from CE: only the satellite flight is ported. Upstream's second mode carries cargo to a
 * landing pad and needs the return capsule, its receiving pad and the designator round trip, none of which
 * are ported; so is the flame-pony easter egg and the achievements the launch grants.
 */
public class EntitySoyuz extends CompatEntity {

    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(EntitySoyuz.class, EntityDataSerializers.INT);

    /** The altitude at which the payload separates. */
    public static final double ORBIT_ALTITUDE = 600;

    private double acceleration;
    private ItemStack satellite = ItemStack.EMPTY;

    public EntitySoyuz(EntityType<? extends EntitySoyuz> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntitySoyuz(Level level) {
        this(ModEntities.SOYUZ.get(), level);
    }

    public EntitySoyuz setSkin(int skin) {
        this.entityData.set(SKIN, skin);
        return this;
    }

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    public void setSat(ItemStack stack) {
        this.satellite = stack.copy();
    }

    public ItemStack getSat() {
        return satellite;
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(SKIN, 0);
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        if (getDeltaMovement().y < 2.0D) {
            acceleration += 0.00025D;
            setDeltaMovement(getDeltaMovement().add(0, acceleration, 0));
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (level() instanceof ServerLevel server) scorch(server);

        if (getY() > ORBIT_ALTITUDE) deployPayload();
    }

    /** Everything standing in the exhaust column under the rocket. */
    private void scorch(ServerLevel level) {
        AABB plume = new AABB(getX() - 5, getY() - 15, getZ() - 5, getX() + 5, getY(), getZ() + 5);
        for (Entity e : level.getEntities(this, plume)) {
            Effects.ignite(e, 15);
            EntityDamageUtil.attackEntityFromIgnoreIFrame(e, ModDamageSources.source(level, ModDamageSources.EXHAUST), 100F);
        }
    }

    private void deployPayload() {
        if (!satellite.isEmpty() && satellite.getItem() instanceof ISatChip) {
            Satellite.orbit(level(), SatelliteRegistry.idFromStack(satellite), satellite,
                    ISatChip.getFreqS(satellite), getX(), getY(), getZ());
        }
        discard();
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("skin", getSkin());
        output.store("satellite", ItemStack.OPTIONAL_CODEC, satellite);
    }

    @Override
    protected void loadData(DataIn input) {
        setSkin(input.getIntOr("skin", 0));
        satellite = input.read("satellite", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }
}
