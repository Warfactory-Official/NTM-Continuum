package com.hbm.entity.missile;

import com.hbm.interfaces.StoredItems;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.Satellite;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 * 1.7 {@code EntityRocketBase}: a launch vehicle that rides its own acceleration straight up and, past y=600,
 * deploys its payload and is gone. Slot 0 is the satellite.
 */
public abstract class EntityRocketBase extends CompatEntity implements StoredItems {

    /** 1.7 separates the payload here. */
    public static final int ORBIT_ALTITUDE = 600;

    protected final ItemStack[] payload;
    protected double acceleration = 0.00D;

    protected EntityRocketBase(EntityType<? extends EntityRocketBase> type, Level level, int payloadCapacity) {
        super(type, level);
        this.payload = new ItemStack[payloadCapacity];
        Arrays.fill(this.payload, ItemStack.EMPTY);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < payload.length; i++) {
            if (visitor.visit(payload[i]) && payload[i].isEmpty()) payload[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public void tick() {
        Vec3 motion = getDeltaMovement();
        if (motion.y < 2.0D) {
            acceleration += 0.00025D;
            motion = new Vec3(motion.x, motion.y + acceleration, motion.z);
            setDeltaMovement(motion);
        }

        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (getY() > ORBIT_ALTITUDE) {
            if (level() instanceof ServerLevel server) deployPayload(server);
            discard();
        }
    }

    public void setSat(ItemStack stack) {
        this.payload[0] = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack getSat() {
        return this.payload[0];
    }

    public void setPayload(List<ItemStack> payload) {
        for (int i = 0; i < payload.size() && i < this.payload.length; i++) {
            ItemStack stack = payload.get(i);
            this.payload[i] = stack == null ? ItemStack.EMPTY : stack;
        }
    }

    protected void deployPayload(ServerLevel level) {
        ItemStack load = payload[0];
        if (!load.isEmpty() && load.getItem() instanceof ISatChip) {
            Satellite.orbit(level, load, ISatChip.getFreqS(load), getX(), getY(), getZ());
        }
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    // 1.7 compared the squared distance against 500000 too
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void loadData(DataIn input) {
        List<ItemStack> list = input.read("items", ItemStacks.optionalCodec().listOf()).orElse(List.of());
        for (int i = 0; i < list.size() && i < payload.length; i++) payload[i] = list.get(i);
    }

    @Override
    protected void saveData(DataOut output) {
        output.store("items", ItemStacks.optionalCodec().listOf(), List.of(payload));
    }
}
