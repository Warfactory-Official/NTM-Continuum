package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.ContainerBatterySocket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * The Battery Socket multiblock's core BlockEntity. Unlike the {@code machine_battery} block (which stores
 * its own {@code long power}), the socket holds <b>no internal energy</b> — its power IS the charge of the
 * {@link IBatteryItem} placed in slot 0. {@link #getPower()}/{@link #setPower(long)}/{@link #getMaxPower()}
 * read and write that item's charge, so charging the socket from the grid literally fills the battery item,
 * which can then be carried away full. Acts as a cable: all 8 connection ports share one network.
 *
 * <p>Port of 1.7's {@code TileEntityBatterySocket}; its OpenComputers component is served to CC: Tweaked by
 * {@code com.hbm.compat.computercraft.storage.BatterySocketPeripheral}. A loaded radiovoltaic battery
 * ({@code battery_sc_*}, not the empty one) fluctuates its output ({@code scPowerMult}, 0.1..1, drifting by up to 1% a
 * tick) and every 1 to 3 minutes discharges: electric beams at every living thing within 15 blocks, each ending in a
 * small electric explosion, plus one explosion at the socket (History §19ek-hooks).
 */
public class TileEntityBatterySocket extends TileEntityBatteryBase implements MenuProvider, IRORValueProvider, IRORInteractive {

    /** Net throughput over the last second (HE/s), for the GUI energy-bar tooltip; synced to clients. */
    public long delta = 0L;
    /** Carried battery stack, synced to clients so the renderer can draw the inserted battery. */
    public ItemStack syncStack = ItemStack.EMPTY;

    private final long[] log = new long[20];

    // 1.7 radiovoltaic discharge state
    public int damageTimer;
    public int damageTarget;
    public double scPowerMult = 1D;

    /** 1.7 {@code discharge}: the beam config of the discharge arcs, registered with the gun configs (GunFactory). */
    public static com.hbm.items.weapon.sedna.BulletConfig discharge;

    /** 1.7 {@code BEAM_DISCHARGE_HIT}: a block hit breaks the block (no drops); block or entity, an explosion. */
    public static final java.util.function.BiConsumer<com.hbm.entity.projectile.EntityBulletBeamBase, net.minecraft.world.phys.HitResult>
            BEAM_DISCHARGE_HIT = (beam, mop) -> {
                if (mop.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                        && mop instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                    beam.level().destroyBlock(blockHit.getBlockPos(), false);
                    explodeDischarge(beam.level(), mop.getLocation().x, mop.getLocation().y, mop.getLocation().z);
                }
                if (mop.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                    explodeDischarge(beam.level(), mop.getLocation().x, mop.getLocation().y, mop.getLocation().z);
                }
            };

    public static synchronized void registerDischarge() {
        if (discharge != null) return;
        discharge = com.hbm.items.weapon.sedna.BulletConfigs.register("battery_socket_discharge",
                new com.hbm.items.weapon.sedna.BulletConfig()
                        .setupDamageClass(com.hbm.lib.ModDamageSources.DamageClass.ELECTRIC).setBeam().setSpread(0.0F)
                        .setLife(3).setThresholdNegation(20F).setArmorPiercing(0.5F).setRenderRotations(false)
                        .setDoesPenetrate(true).setOnBeamImpact(BEAM_DISCHARGE_HIT));
    }

    /** 1.7 {@code explodeDischarge}: a size 5 VNT whose entity damage is the electric cross-smooth 20, and the UFO blast. */
    public static void explodeDischarge(Level level, double x, double y, double z) {
        new com.hbm.explosion.vanillant.ExplosionVNT(level, x, y, z, 5F)
                .setEntityProcessor(new com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth(1D, 20F)
                        .setDamageClass(com.hbm.lib.ModDamageSources.DamageClass.ELECTRIC))
                .setPlayerProcessor(new com.hbm.explosion.vanillant.standard.PlayerProcessorStandard())
                .setSFX(new com.hbm.explosion.vanillant.standard.ExplosionEffectStandard())
                .explode();
        level.playSound(null, x, y, z, com.hbm.sound.ModSounds.UFO_BLAST.get(), net.minecraft.sounds.SoundSource.BLOCKS, 5F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
    }

    public TileEntityBatterySocket(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_SOCKET.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "container.batterySocket";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBatterySocket socket) {
        long prevPower = socket.getPower();
        socket.serverTickBattery(level);

        if (socket.hasSCLoaded()) {
            if (socket.damageTarget == 0) socket.pickNewSCTarget();
            socket.damageTimer++;
            if (socket.damageTimer >= socket.damageTarget) socket.discharge();
            socket.fluctuate();
        }

        // Net throughput over the last second (HE/s), averaged like the 1.12.2 socket's rolling log.
        long avg = (socket.getPower() + prevPower) / 2;
        socket.delta = avg - socket.log[0];
        System.arraycopy(socket.log, 1, socket.log, 0, socket.log.length - 1);
        socket.log[socket.log.length - 1] = avg;

        socket.syncStack = socket.inventory.getStackInSlot(0);
    }

    // --- 1.7 radiovoltaic discharge ---

    public boolean hasSCLoaded() {
        ItemStack stack = battery();
        return stack.getItem() instanceof com.hbm.items.machine.ItemBatterySC sc
                && sc.pack != com.hbm.items.machine.ItemBatterySC.EnumBatterySC.EMPTY;
    }

    protected void pickNewSCTarget() {
        this.damageTimer = 0;
        this.damageTarget = 1200 + level.getRandom().nextInt(2400); // 1-3 minutes
        setChanged();
    }

    public void discharge() {
        pickNewSCTarget();
        registerDischarge();

        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int xCoord = getBlockPos().getX(), yCoord = getBlockPos().getY(), zCoord = getBlockPos().getZ();

        double x = xCoord + 0.5 - dir.offsetX * 0.5 + rot.offsetX * 0.5;
        double y = yCoord + 1;
        double z = zCoord + 0.5 - dir.offsetZ * 0.5 + rot.offsetX * 0.5; // 1.7 adds rot.offsetX here too; ported as written

        double range = 15;
        java.util.List<net.minecraft.world.entity.LivingEntity> potentialTargets = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(x, y, z, x, y, z).inflate(range, range, range));
        java.util.Collections.shuffle(potentialTargets);

        for (net.minecraft.world.entity.LivingEntity target : potentialTargets) {
            net.minecraft.world.phys.Vec3 initialDelta = new net.minecraft.world.phys.Vec3(target.getX() - x,
                    target.getY() + target.getBbHeight() / 2 - y, target.getZ() - z);
            if (initialDelta.length() > range) continue;
            com.hbm.entity.projectile.EntityBulletBeamBase sub =
                    new com.hbm.entity.projectile.EntityBulletBeamBase(level, discharge, 50F);
            initialDelta = initialDelta.normalize();
            double dominantAxis = Math.max(Math.abs(initialDelta.x), Math.max(Math.abs(initialDelta.y), Math.abs(initialDelta.z)));
            initialDelta = initialDelta.scale(1.125D / dominantAxis); // move 1.125 blocks outwards
            // 1.7 starts the arc from the block corner, not from the arc point above
            sub.setPos(xCoord + initialDelta.x, yCoord + initialDelta.y, zCoord + initialDelta.z);
            net.minecraft.world.phys.Vec3 actualDelta = new net.minecraft.world.phys.Vec3(target.getX() - sub.getX(),
                    target.getY() + target.getBbHeight() / 2 - sub.getY(), target.getZ() - sub.getZ());
            sub.setRotationsFromVector(actualDelta);
            sub.performHitscanExternal(actualDelta.length());
            level.addFreshEntity(sub);
        }

        explodeDischarge(level, x + level.getRandom().nextGaussian() * 0.5, y + level.getRandom().nextGaussian() * 0.5,
                z + level.getRandom().nextGaussian() * 0.5);
    }

    protected void fluctuate() {
        double steppy = 1D / 100D;
        this.scPowerMult += (steppy * (level.getRandom().nextDouble() * 2 - 1));
        this.scPowerMult = net.minecraft.util.Mth.clamp(scPowerMult, 0.1D, 1D);
    }

    // --- the inserted battery item IS the storage ---

    private ItemStack battery() {
        return inventory.getStackInSlot(0);
    }

    public static long powerFromStack(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IBatteryItem bat ? bat.getCharge(stack) : 0L;
    }

    public static long maxPowerFromStack(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IBatteryItem bat ? bat.getMaxCharge(stack) : 0L;
    }

    @Override
    public long getPower() {
        long power = powerFromStack(battery());
        if (hasSCLoaded()) power *= this.scPowerMult; // 1.7: long *= double, truncated
        return power;
    }

    @Override
    public long getMaxPower() {
        return maxPowerFromStack(battery());
    }

    @Override
    public void setPower(long power) {
        ItemStack stack = battery();
        if (stack.getItem() instanceof IBatteryItem bat) bat.setCharge(stack, power);
    }

    @Override
    public long getReceiverSpeed() {
        ItemStack stack = battery();
        if (!(stack.getItem() instanceof IBatteryItem bat)) return 0L;
        int mode = getRelevantMode(true);
        return (mode == mode_input || mode == mode_buffer) ? bat.getChargeRate(stack) : 0L;
    }

    @Override
    public long getProviderSpeed() {
        ItemStack stack = battery();
        if (!(stack.getItem() instanceof IBatteryItem bat)) return 0L;
        int mode = getRelevantMode(true);
        return (mode == mode_output || mode == mode_buffer) ? bat.getDischargeRate(stack) : 0L;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof IBatteryItem;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0};
    }

    // --- multiblock port geometry (ported verbatim from the 1.12.2 socket) ---

    private ForgeDirection facing() {
        return MachineCore.facing(getBlockState());
    }

    @Override
    public BlockPos[] getPortPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new BlockPos[]{
                new BlockPos(x, y, z),
                new BlockPos(x - dir.offsetX, y, z - dir.offsetZ),
                new BlockPos(x + rot.offsetX, y, z + rot.offsetZ),
                new BlockPos(x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ)};
    }

    @Override
    public DirPos[] getConPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new DirPos[]{
                new DirPos(x + dir.offsetX, y, z + dir.offsetZ, dir),
                new DirPos(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, dir),

                new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),

                new DirPos(x + rot.offsetX * 2, y, z + rot.offsetZ * 2, rot),
                new DirPos(x + rot.offsetX * 2 - dir.offsetX, y, z + rot.offsetZ * 2 - dir.offsetZ, rot),

                new DirPos(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                new DirPos(x - rot.offsetX - dir.offsetX, y, z - rot.offsetZ - dir.offsetZ, rot.getOpposite())};
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_battery_socket");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerBatterySocket(containerId, playerInventory, this);
    }

    // --- sync (BufPacket) + persistence: add delta + the carried stack on top of the base fields ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(delta);
        dev.golem.core.world.ItemStacks.writeOptional(buf, inventory.getStackInSlot(0));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        delta = buf.readLong();
        syncStack = dev.golem.core.world.ItemStacks.readOptional(buf);
    }

    @Override
    protected void saveData(dev.golem.core.data.DataOut output) {
        super.saveData(output);
        output.putInt("damageTimer", damageTimer);
        output.putInt("damageTarget", damageTarget);
        output.putDouble("scPowerMult", scPowerMult);
    }

    @Override
    protected void loadData(dev.golem.core.data.DataIn input) {
        super.loadData(input);
        damageTimer = input.getIntOr("damageTimer", 0);
        damageTarget = input.getIntOr("damageTarget", 0);
        scPowerMult = input.getDoubleOr("scPowerMult", 1D);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent", PREFIX_VALUE + "delta",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + getPower();
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + (getPower() * 100L / Math.max(getMaxPower(), 1L));
        if ((PREFIX_VALUE + "delta").equals(name)) return "" + delta;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int mode = IRORInteractive.parseInt(params[0], 0, 3);
            if (mode != redLow) redLow = (short) mode;
            else if (params.length > 1) redLow = (short) IRORInteractive.parseInt(params[1], 0, 3);
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setredmode").equals(name) && params.length > 0) {
            int mode = IRORInteractive.parseInt(params[0], 0, 3);
            if (mode != redHigh) redHigh = (short) mode;
            else if (params.length > 1) redHigh = (short) IRORInteractive.parseInt(params[1], 0, 3);
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            priority = ConnectionPriority.VALUES[IRORInteractive.parseInt(params[0], 0, 2) + 1];
            setChanged();
        }
        return null;
    }
}
