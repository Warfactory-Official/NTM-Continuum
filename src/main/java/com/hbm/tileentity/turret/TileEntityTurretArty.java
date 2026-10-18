package com.hbm.tileentity.turret;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.weapon.ItemAmmoArty;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The 16-inch gun. Three modes off one button: a high-arc artillery piece reaching three thousand
 * blocks, a flat-trajectory cannon good for a quarter of that, and a manual battery that only fires
 * where a fire mission points it.
 *
 * <p>The aim is a closed-form ballistic solution rather than a look-at, and it is solved from the
 * <b>muzzle</b>: over three thousand blocks a nine-block barrel is worth a visible angle. The mode
 * picks which of the two solutions to take: the high arc for artillery, the low one for the cannon.
 */
public class TileEntityTurretArty extends TileEntityTurretBaseArtillery {

    public static final short MODE_ARTILLERY = 0;
    public static final short MODE_CANNON = 1;
    public static final short MODE_MANUAL = 2;
    /** The mode button's id in the shared turret screen. */
    public static final int BUTTON_MODE = 5;

    private static final Identifier GUI = turretGui("arty");
    /** Gravity per tick, matching {@link EntityArtilleryShell#getGravityVelocity}. */
    private static final double GRAVITY = 9.81D * 0.05D;

    public short mode = MODE_ARTILLERY;
    public double barrelPos;
    public double lastBarrelPos;

    private boolean didJustShoot;
    private boolean retracting;
    private int timer;

    public TileEntityTurretArty(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_ARTY.get(), pos, state);
    }

    /** The gun takes no Sedna round: its ammunition is {@link ItemAmmoArty}, which carries a warhead. */
    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return List.of();
    }

    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemAmmoArty.ArtilleryShellType type : ItemAmmoArty.ArtilleryShellType.values()) {
            stacks.add(ModAmmoItems.ammo_arty.stack(type));
        }

        ammoStacks = stacks;
        return ammoStacks;
    }

    @Override
    public String getDefaultName() {
        return "container.turretArty";
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
        return 100_000L;
    }

    @Override
    public double getBarrelLength() {
        return 9D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 0D;
    }

    @Override
    public double getHeightOffset() {
        return 3D;
    }

    @Override
    public double getDetectorRange() {
        return mode == MODE_CANNON ? 250D : 3000D;
    }

    @Override
    public double getDetectorGrace() {
        return mode == MODE_CANNON ? 32D : 250D;
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
    public double getTurretDepression() {
        return 30D;
    }

    @Override
    public double getTurretElevation() {
        return 90D;
    }

    @Override
    public int getDetectorInterval() {
        return mode == MODE_CANNON ? 20 : 200;
    }

    /** Only the flat-trajectory cannon needs to see what it is shooting at. */
    @Override
    public boolean doLOSCheck() {
        return mode == MODE_CANNON;
    }

    @Override
    protected void alignTurret() {
        // Upstream uses +pitch for aim compensation, while firing uses -pitch.
        Vec3 muzzle = getTurretPos().add(toWorldDirection(new Vec3(getBarrelLength(), 0D, 0D)
                .zRot((float) rotationPitch)
                .yRot((float) -(rotationYaw + Math.PI * 0.5D))));
        Vec3 delta = tPos.subtract(muzzle);
        double targetYaw = -Math.atan2(delta.x, delta.z);

        double x = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double y = delta.y;
        double v0 = getV0();
        double v02 = v0 * v0;
        // The two solutions of the ballistic equation: the artillery takes the high arc, the cannon the low.
        double branch = mode == MODE_CANNON ? -1D : 1D;
        double targetPitch = Math.atan((v02 + Math.sqrt(v02 * v02 - GRAVITY * (GRAVITY * x * x + 2D * y * v02))
                * branch) / (GRAVITY * x));

        // a ballistic solution is in world angles (gravity is the world's)
        turnTowardsWorldAngle(targetPitch, targetYaw);
    }

    public double getV0() {
        return mode == MODE_CANNON ? 20D : 50D;
    }

    public ItemStack getShellLoaded() {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            if (stack.getItem() instanceof ItemAmmoArty) return stack;
        }
        return ItemStack.EMPTY;
    }

    public void consumeShell() {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            if (getItem(i).getItem() instanceof ItemAmmoArty) {
                removeItem(i, 1);
                return;
            }
        }
        setChanged();
    }

    public void spawnShell(ItemStack loaded) {
        Vec3 heading = alongBarrel(getBarrelLength(), 0D, 0D);
        Vec3 pos = getTurretPos().add(heading);
        int variant = loaded.getItem() instanceof ItemAmmoArty shell ? shell.getVariant(loaded) : 0;

        EntityArtilleryShell shell = new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), level);
        shell.setPos(pos.x, pos.y, pos.z);
        shell.setThrowableHeading(heading.x, heading.y, heading.z, (float) getV0(), 0F);
        shell.setTarget((int) tPos.x, (int) tPos.y, (int) tPos.z);
        shell.setType(variant);

        if (variant == ItemAmmoArty.ArtilleryShellType.CARGO.ordinal()) {
            ItemStack cargo = ModDataComponents.ARTY_CARGO.getOr(loaded, ItemStack.EMPTY);
            if (!cargo.isEmpty()) shell.setCargo(cargo.copy());
        }

        // Only the lobbed shots announce themselves; a flat cannon round arrives before the sound would.
        if (mode != MODE_CANNON) shell.setWhistle(true);

        level.addFreshEntity(shell);
    }

    @Override
    protected void tickClient() {
        lastBarrelPos = barrelPos;

        if (retracting) {
            barrelPos += 0.5D;
            if (barrelPos >= 1D) retracting = false;
        } else {
            barrelPos -= 0.05D;
            if (barrelPos < 0D) barrelPos = 0D;
        }

        super.tickClient();
    }

    /** @see TileEntityTurretHIMARS#tickServer, the same reason it does not call {@code super}. */
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
            if (tPos != null) alignTurret();
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

        if (aligned) updateFiringTick();

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, getMaxPower());

        setChanged();
        networkPackNT();

        didJustShoot = false;
    }

    @Override
    public void updateFiringTick() {
        timer++;

        int delay = mode == MODE_ARTILLERY ? 300 : 40;
        if (timer % delay != 0) return;

        ItemStack loaded = getShellLoaded();
        if (!loaded.isEmpty()) {
            spawnShell(loaded);
            consumeShell();
            level.playSound(null, worldPosition, ModSounds.TURRET_JEREMY_FIRE.get(), SoundSource.BLOCKS, 25.0F, 1.0F);
            didJustShoot = true;
            muzzleFlash(barrelTip(), 0F, 5);
        }

        // One mission per volley: the queue advances whether or not there was a shell to send.
        if (mode == MODE_MANUAL && !targetQueue.isEmpty()) {
            targetQueue.remove(0);
            tPos = null;
        }
    }

    @Override
    public void handleButtonPacket(int value, int meta) {
        if (meta == BUTTON_MODE) {
            mode = (short) ((mode + 1) % 3);
            abandonEngagement();
            return;
        }
        super.handleButtonPacket(value, meta);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_FUNCTION + "setactive" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "targetplayers" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetanimals" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetmobs" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetmachines" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "addwhitelist" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "removewhitelist" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode",
                PREFIX_FUNCTION + "enqueue" + NAME_SEPARATOR + "x" + PARAM_SEPARATOR + "y" + PARAM_SEPARATOR + "z"};
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        super.runRORFunction(name, params);

        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length == 1) {
            short next = (short) IRORInteractive.parseInt(params[0], 0, 2);
            // port: a mode change drops the engagement as the mode button does (see abandonEngagement)
            if (next != mode) abandonEngagement();
            mode = next;
            markChanged();
        }

        return null;
    }

    /**
     * The recoil animation is armed by the shot the server just took: the server sends
     * {@code didJustShoot} and the client reads it back into {@code retracting}.
     */
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        buf.writeBoolean(didJustShoot);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        retracting = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putShort("mode", mode);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        mode = input.getShortOr("mode", (short) 0);
    }
}
