package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.container.ContainerForceField;
import com.hbm.items.ModRadarUtilityItems;
import com.hbm.sound.ModGunSounds;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import com.hbm.tileentity.IConfigurableMachine;

/**
 * 1.7 {@code TileEntityForceField}: a sphere, 16 blocks across by default and 16 more per radius upgrade, that
 * no non-player entity may cross in either direction. A crossing entity is put back one block on its own side
 * of the shell, its speed reflected, and the impact ({@code height x width² x speed x 50}) is taken off the
 * shield; at zero health the emitter goes down for {@code 100 + radius} ticks.
 *
 * <p>The field logic runs on both sides, as in 1.7, so a client sees an entity bounce without waiting for the
 * server; only the server spends power, takes damage and changes the colour. The state the renderer and GUI
 * need travels in the tile's own sync, which is what 1.7's {@code TEFFPacket} carried every tick.
 *
 * <p>Only the inward bounce is muffle-aware, as in 1.7.
 */
public class TileEntityForceField extends TileEntityMachineBase implements IEnergyReceiverMK2, MenuProvider {

    public int health = 100;
    public int maxHealth = 100;
    public long power;
    public int powerCons;
    public int cooldown = 0;
    public int blink = 0;
    public float radius = 16;
    public boolean isOn = false;
    public int color = 0x0000FF;
    public static int baseCon = 1000;
    public static int radCon = 500;
    public static int shCon = 250;
    public static long maxPower = 1000000;

    // config options stuff.
    public static int baseRadius = 16;
    public static int radUpgrade = 16;
    public static int shUpgrade = 50;
    public static double cooldownModif = 1;
    public static double healthRegenModif = 1;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "forcefield"; }

        @Override public void readIfPresent(JsonObject obj) {
            maxPower = IConfigurableMachine.grab(obj, "L:powerCap", maxPower);
            baseCon = IConfigurableMachine.grab(obj, "I:baseConsumption", baseCon);
            radCon = IConfigurableMachine.grab(obj, "I:radiusConsumption", radCon);
            shCon = IConfigurableMachine.grab(obj, "I:shieldConsumption", shCon);
            baseRadius = IConfigurableMachine.grab(obj, "I:baseRadius", baseRadius);
            radUpgrade = IConfigurableMachine.grab(obj, "I:radiusUpgrade", radUpgrade);
            shUpgrade = IConfigurableMachine.grab(obj, "I:shieldUpgrade", shUpgrade);
            cooldownModif = IConfigurableMachine.grab(obj, "D:cooldownModifier", cooldownModif);
            healthRegenModif = IConfigurableMachine.grab(obj, "D:healthRegenModifier", healthRegenModif);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("L:powerCap").value(maxPower);
            writer.name("I:baseConsumption").value(baseCon);
            writer.name("I:radiusConsumption").value(radCon);
            writer.name("I:shieldConsumption").value(shCon);
            writer.name("I:baseRadius").value(baseRadius);
            writer.name("I:radiusUpgrade").value(radUpgrade);
            writer.name("I:shieldUpgrade").value(shUpgrade);
            writer.name("D:cooldownModifier").value(cooldownModif);
            writer.name("D:healthRegenModifier").value(healthRegenModif);
        }
    };

    List<Entity> outside = new ArrayList<>();
    List<Entity> inside = new ArrayList<>();

    public TileEntityForceField(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.FORCEFIELD.get(), pos, state, 3);
    }

    @Override
    public String getDefaultName() {
        return "container.forceField";
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == 0) return stack.getItem() instanceof IBatteryItem;
        return i == 1;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0};
    }

    public int getHealthScaled(int i) {
        return (health * i) / maxHealth;
    }

    public long getPowerScaled(long i) {
        return (power * i) / maxPower;
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityForceField field) {
        field.update(level);
    }

    private void update(Level level) {
        boolean remote = level.isClientSide();

        if (!remote) {
            this.setupPowerPorts(getPorts());
            this.updateAllPorts();
            this.receivePower();

            int rStack = 0;
            int hStack = 0;
            radius = baseRadius;
            maxHealth = 100;

            ItemStack radiusUp = inventory.getStackInSlot(1);
            if (!radiusUp.isEmpty() && radiusUp.getItem() == ModRadarUtilityItems.upgrade_radius.get()) {
                rStack = radiusUp.getCount();
                radius += rStack * radUpgrade;
            }

            ItemStack healthUp = inventory.getStackInSlot(2);
            if (!healthUp.isEmpty() && healthUp.getItem() == ModRadarUtilityItems.upgrade_health.get()) {
                hStack = healthUp.getCount();
                maxHealth += hStack * shUpgrade;
            }

            this.powerCons = baseCon + rStack * radCon + hStack * shCon;

            power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

            if (blink > 0) {
                blink--;
                color = 0xFF0000;
            } else {
                color = 0x00FF00;
            }
        }

        if (cooldown > 0) {
            cooldown--;
        } else {
            if (health < maxHealth) health += (int) ((maxHealth / 100) * healthRegenModif);
            if (health > maxHealth) health = maxHealth;
        }

        if (isOn && cooldown == 0 && health > 0 && power >= powerCons) {
            doField(level, radius);
            if (!remote) power -= powerCons;
        } else {
            this.outside.clear();
            this.inside.clear();
        }

        if (!remote) {
            if (power < powerCons) power = 0;
            networkPackNT();
        }
    }

    private int impact(Entity e) {
        double mass = e.getBbHeight() * e.getBbWidth() * e.getBbWidth();
        double speed = getMotionWithFallback(e);
        return (int) (mass * speed * 50);
    }

    private void damage(int ouch) {
        health -= ouch;

        if (ouch >= (this.maxHealth / 250)) blink = 5;

        if (health <= 0) {
            health = 0;
            cooldown = (int) (100 + radius * (float) cooldownModif);
        }
    }

    private void doField(Level level, float rad) {

        List<Entity> oLegacy = new ArrayList<>(outside);
        List<Entity> iLegacy = new ArrayList<>(inside);

        outside.clear();
        inside.clear();

        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        List<Entity> list = level.getEntities((Entity) null,
                new AABB(cx - (rad + 25), cy - (rad + 25), cz - (rad + 25), cx + (rad + 25), cy + (rad + 25), cz + (rad + 25)));

        for (Entity entity : list) {
            if (entity instanceof Player) continue;

            double dist = Math.sqrt(Math.pow(cx - entity.getX(), 2) + Math.pow(cy - entity.getY(), 2) + Math.pow(cz - entity.getZ(), 2));
            boolean out = dist > rad;

            // if the entity has not been registered yet
            if (!oLegacy.contains(entity) && !iLegacy.contains(entity)) {
                if (out) outside.add(entity);
                else inside.add(entity);

            // if the entity has crossed inwards
            } else if (oLegacy.contains(entity) && !out) {
                bounce(level, entity, rad + 1, -1);
                if (!this.muffled) playSpark(level, entity);
                outside.add(entity);
                if (!level.isClientSide()) this.damage(this.impact(entity));

            // if the entity has crossed outwards
            } else if (iLegacy.contains(entity) && out) {
                bounce(level, entity, rad - 1, 1);
                playSpark(level, entity);
                inside.add(entity);
                if (!level.isClientSide()) this.damage(this.impact(entity));
            } else {
                if (out) outside.add(entity);
                else inside.add(entity);
            }
        }
    }

    /**
     * Puts the entity back on the shell at {@code shell} blocks from the centre, facing yaw/pitch 0 as 1.7's
     * {@code setLocationAndAngles(..., 0, 0)} did, with its speed pointed {@code sign} along the centre line,
     * and then steps it back by that new motion.
     */
    private void bounce(Level level, Entity entity, float shell, int sign) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        Vec3 vec = new Vec3(cx - entity.getX(), cy - entity.getY(), cz - entity.getZ()).normalize();

        double mx = -vec.x * shell;
        double my = -vec.y * shell;
        double mz = -vec.z * shell;

        entity.snapTo(cx + mx, cy + my, cz + mz, 0, 0);

        Vec3 motion = entity.getDeltaMovement();
        double mo = motion.length();
        Vec3 newMotion = new Vec3(vec.x * mo * sign, vec.y * mo * sign, vec.z * mo * sign);
        entity.setDeltaMovement(newMotion);
        entity.setPos(entity.getX() - newMotion.x, entity.getY() - newMotion.y, entity.getZ() - newMotion.z);
    }

    private static void playSpark(Level level, Entity entity) {
        // 1.7 "hbm:weapon.sparkShoot": the sounds.json entry picked one of three files
        var sounds = ModGunSounds.SPARK_SHOOT;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                sounds[level.getRandom().nextInt(sounds.length)].get(), SoundSource.BLOCKS, 2.5F, 1.0F);
    }

    private double getMotionWithFallback(Entity e) {
        Vec3 v1 = e.getDeltaMovement();
        Vec3 v2 = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);

        double s1 = v1.length();
        double s2 = v2.length();

        if (s1 == 0) return s2;
        if (s2 == 0) return s1;
        return Math.min(s1, s2);
    }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }

    /** 1.7 subscribed five sides; everything but the top connects. */
    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UP && dir != ForgeDirection.UNKNOWN;
    }

    /** 1.7 {@code AuxButtonPacket}: the one button toggles the field. */
    public void toggle() {
        this.isOn = !this.isOn;
        this.setChanged();
    }

    // --- sync: 1.7 TEFFPacket (radius, health, maxHealth, power as an int, isOn, color, cooldown) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeFloat(radius);
        buf.writeInt(health);
        buf.writeInt(maxHealth);
        buf.writeInt((int) power);
        buf.writeBoolean(isOn);
        buf.writeInt(color);
        buf.writeInt(cooldown);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        radius = buf.readFloat();
        health = buf.readInt();
        maxHealth = buf.readInt();
        power = buf.readInt();
        isOn = buf.readBoolean();
        color = buf.readInt();
        cooldown = buf.readInt();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.power = input.getLongOr("powerTime", 0L);
        this.health = input.getIntOr("health", 0);
        this.maxHealth = input.getIntOr("maxHealth", 0);
        this.cooldown = input.getIntOr("cooldown", 0);
        this.blink = input.getIntOr("blink", 0);
        this.radius = input.getFloatOr("radius", 0F);
        this.isOn = input.getBooleanOr("isOn", false);
        if (this.maxHealth <= 0) this.maxHealth = 100; // 1.7 divides by it before the first tick sets it
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("powerTime", power);
        output.putInt("health", health);
        output.putInt("maxHealth", maxHealth);
        output.putInt("cooldown", cooldown);
        output.putInt("blink", blink);
        output.putFloat("radius", radius);
        output.putBoolean("isOn", isOn);
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerForceField(containerId, playerInventory, this);
    }
}
