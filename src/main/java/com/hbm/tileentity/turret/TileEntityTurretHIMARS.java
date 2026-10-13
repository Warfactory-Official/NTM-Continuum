package com.hbm.tileentity.turret;

import dev.golem.core.data.Tags;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The rocket artillery. It fires at a fixed forty-five degrees and lets the rocket do the aiming. Only
 * the yaw is ever laid on the target, which is why {@link #alignTurret} ignores the pitch entirely.
 *
 * <p>Reloading is the visible part: the launcher levels out, the crane walks the pod back over five
 * seconds, and a rocket is drawn from the ammunition slots only once the crane is fully extended. It
 * cannot fire until the crane is home again, so the whole cycle is one animation the renderer reads
 * straight off {@link #crane}.
 */
public class TileEntityTurretHIMARS extends TileEntityTurretBaseArtillery {

    public static final short MODE_AUTO = 0;
    public static final short MODE_MANUAL = 1;
    /** The mode button's id in the shared turret screen. */
    public static final int BUTTON_MODE = 5;

    private static final Identifier GUI = turretGui("himars");
    private static final int FIRE_INTERVAL = 40;
    private static final float CRANE_STEP = 0.0125F;

    public short mode = MODE_AUTO;
    /** The loaded rocket's variant index, or -1 for an empty launcher. */
    public int typeLoaded = -1;
    public int ammo;
    public float crane;
    public float lastCrane;

    private int timer;

    public TileEntityTurretHIMARS(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_HIMARS.get(), pos, state);
    }

    /**
     * The HIMARS takes no Sedna round: its ammunition is {@link ItemAmmoHIMARS}, which carries a
     * warhead rather than a bullet config.
     */
    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return List.of();
    }

    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemAmmoHIMARS.HIMARSRocketType type : ItemAmmoHIMARS.HIMARSRocketType.values()) {
            stacks.add(ModAmmoItems.ammo_himars.stack(type));
        }

        ammoStacks = stacks;
        return ammoStacks;
    }

    @Override
    public String getDefaultName() {
        return "container.turretHIMARS";
    }

    @Override
    public Identifier guiTexture() {
        return GUI;
    }

    @Override
    public int guiFontColor() {
        return 0x433B33;
    }

    @Override
    public long getMaxPower() {
        return 1_000_000L;
    }

    @Override
    public double getBarrelLength() {
        return 0.5D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 5D;
    }

    @Override
    public double getHeightOffset() {
        return 5D;
    }

    @Override
    public double getDetectorRange() {
        return 5000D;
    }

    @Override
    public double getDetectorGrace() {
        return 250D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 1D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 0.5D;
    }

    @Override
    public boolean doLOSCheck() {
        return false;
    }

    /** Yaw onto the target, pitch parked at forty-five degrees: the rocket flies the rest. */
    @Override
    protected void alignTurret() {
        Vec3 delta = tPos.subtract(getTurretPos());
        turnTowardsWorldAngle(Math.PI / 4D, -Math.atan2(delta.x, delta.z));
    }

    /** The variant of the first HIMARS pod in the ammunition slots, or -1 if there is none. */
    public int getSpareRocket() {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            if (stack.getItem() instanceof ItemAmmoHIMARS pod) {
                return pod.getVariant(stack);
            }
        }
        return -1;
    }

    public void consumeRocket(int variant) {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            if (stack.getItem() instanceof ItemAmmoHIMARS pod && pod.getVariant(stack) == variant) {
                removeItem(i, 1);
                return;
            }
        }
        setChanged();
    }

    public boolean hasAmmo() {
        return typeLoaded >= 0 && ammo > 0;
    }

    @Override
    protected void tickClient() {
        lastCrane = crane;
        super.tickClient();
    }

    /**
     * Not a call to {@code super}: the base tick aims and fires in one pass, and this gun has to
     * interleave a reload that takes the launcher off the target and back. The order below is 1.12.2's.
     */
    @Override
    protected void tickServer() {
        if (mode == MODE_MANUAL) {
            if (!targetQueue.isEmpty()) tPos = targetQueue.get(0);
        } else {
            targetQueue.clear();
        }

        aligned = false;

        if (target != null && !target.isAlive()) {
            target = null;
            stattrak++;
        }

        if (target != null && mode != MODE_MANUAL && !entityInLOS(target)) target = null;

        if (target != null) tPos = getEntityPos(target);
        else if (mode != MODE_MANUAL) tPos = null;

        if (isOn() && hasPower()) {
            if (!hasAmmo() || crane > 0F) {
                turnTowardsAngle(0D, rotationYaw);

                if (aligned) {
                    if (hasAmmo()) {
                        crane -= CRANE_STEP;
                    } else {
                        crane += CRANE_STEP;

                        if (crane >= 1F) {
                            int available = getSpareRocket();
                            if (available != -1) {
                                typeLoaded = available;
                                ammo = ItemAmmoHIMARS.byIndex(available).amount;
                                consumeRocket(available);
                            }
                        }
                    }
                }

                crane = Mth.clamp(crane, 0F, 1F);
            } else if (tPos != null) {
                alignTurret();
            }
        } else {
            target = null;
            tPos = null;
        }

        if (!isOn()) targetQueue.clear();

        if (target != null && !target.isAlive()) {
            target = null;
            tPos = null;
            stattrak++;
        }

        if (isOn() && hasPower()) {
            searchTimer--;
            setPower(getPower() - getConsumption());

            if (searchTimer <= 0) {
                searchTimer = getDetectorInterval();
                if (target == null && mode != MODE_MANUAL) seekNewTarget();
            }
        } else {
            searchTimer = 0;
        }

        if (aligned && crane <= 0F) updateFiringTick();

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, getMaxPower());

        setChanged();
        networkPackNT();
    }

    @Override
    public void updateFiringTick() {
        timer++;
        if (timer % FIRE_INTERVAL != 0) return;

        if (hasAmmo() && tPos != null) {
            spawnRocket(typeLoaded);
            ammo--;
            level.playSound(null, worldPosition, ModSounds.MISSILE_TAKEOFF.get(), SoundSource.BLOCKS, 25.0F, 1.0F);
        }

        // One mission per volley: the queue advances whether or not there was a rocket to send.
        if (mode == MODE_MANUAL && !targetQueue.isEmpty()) {
            targetQueue.remove(0);
            tPos = null;
        }
    }

    public void spawnRocket(int type) {
        Vec3 heading = alongBarrel(getBarrelLength(), 0D, 0D);
        Vec3 pos = getTurretPos().add(heading);

        EntityArtilleryRocket rocket = new EntityArtilleryRocket(ModEntities.HIMARS_ROCKET.get(), level);
        rocket.setPos(pos.x, pos.y, pos.z);
        rocket.setThrowableHeading(heading.x, heading.y, heading.z, 25F, 0F);

        if (target != null) rocket.setTarget(target);
        else rocket.setTarget(tPos.x, tPos.y, tPos.z);

        rocket.setType(type);
        level.addFreshEntity(rocket);
    }

    @Override
    public void handleButtonPacket(int value, int meta) {
        if (meta == BUTTON_MODE) {
            mode = (short) ((mode + 1) % 2);
            abandonEngagement();
            return;
        }
        super.handleButtonPacket(value, meta);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        buf.writeInt(typeLoaded);
        buf.writeInt(ammo);
        buf.writeFloat(crane);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        typeLoaded = buf.readInt();
        ammo = buf.readInt();
        crane = buf.readFloat();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putShort("mode", mode);
        output.putInt("type", typeLoaded);
        output.putInt("ammo", ammo);
        output.putFloat("crane", crane);
    }

    /**
     * 1.12.2 writes the loaded type as an int and reads it back as a short, so a reloaded world always
     * came up holding variant 0. Deliberate deviation: the rocket type survives a reload here.
     */
    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        mode = input.getShortOr("mode", (short) 0);
        typeLoaded = input.getIntOr("type", -1);
        ammo = input.getIntOr("ammo", 0);
        crane = input.getFloatOr("crane", 0F);
    }
}
