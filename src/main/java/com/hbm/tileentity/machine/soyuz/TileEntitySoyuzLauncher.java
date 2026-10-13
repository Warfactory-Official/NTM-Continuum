package com.hbm.tileentity.machine.soyuz;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.item.IDesignatorItem;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.inventory.container.ContainerSoyuzLauncher;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.CanisterBays;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.special.ItemSoyuz;
import com.hbm.blocks.machine.soyuz.SoyuzLauncher;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModPowerGenSounds;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code TileEntitySoyuzLauncher}: the launch complex's controls. Twenty-seven slots - 0 rocket, 1 designator,
 * 2 satellite, 3 orbital module, 4/5 kerosene canister in/out, 6/7 oxygen canister in/out, 8 battery, 9..26 the
 * cargo - two 128,000 mB tanks and a mode byte: SATELLITE (0) puts the item in slot 2 into orbit on its frequency,
 * CARGO (1) flies the eighteen cargo stacks to the designator's target in a lander capsule. Thirty seconds of
 * countdown once started; the rocket leaves when the clock hits zero.
 *
 * <p>1.7's {@code canLaunch} tests {@code hasRocket()} twice and never {@code hasOxy()}, so a launch needs
 * kerosene but not oxygen, while liftoff still drains both tanks. Clamped at zero here; 1.7's {@code FluidTank.setFill}
 * does not clamp, so a 1.7 launch short of oxygen left a negative oxygen fill. Nothing may be piped into the inventory (1.7 {@code isItemValidForSlot} is the base's false).
 */
public class TileEntitySoyuzLauncher extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2 {

    public static final long maxPower = 1_000_000;
    /** Thirty seconds of hold-down, in ticks. */
    public static final int maxCount = 600;
    public static final int FUEL_REQUIRED = 128_000;

    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_SATELLITE = 2;
    public static final int SLOT_ORBITAL = 3;
    public static final int SLOT_BATTERY = 8;
    public static final int SLOT_CARGO = 9;

    public long power;
    public FluidTankNTM[] tanks;
    /** 0: satellite, 1: cargo. */
    public byte mode;
    public boolean starting;
    public int countdown = maxCount;
    public int rocketType = -1;

    private List<DirPos> conPos;
    private com.hbm.sound.@Nullable AudioWrapper audio;

    public TileEntitySoyuzLauncher(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOYUZ_LAUNCHER.get(), pos, state, 27);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.KEROSENE, 128_000);
        tanks[1] = new FluidTankNTM(Fluids.OXYGEN, 128_000);
    }

    @Override
    public String getDefaultName() {
        return "container.soyuzLauncher";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntitySoyuzLauncher launcher) {
        launcher.tickServer(level);
    }

    protected void tickServer(Level level) {
        if (level.getGameTime() % 20 == 0) {
            for (DirPos port : getConPos()) {
                this.trySubscribe(level, port);
                this.trySubscribe(tanks[0].getTankType(), level, port);
                this.trySubscribe(tanks[1].getTankType(), level, port);
            }
        }
        CanisterBays.process(inventory, tanks[0], 4, 5, before -> { });
        CanisterBays.process(inventory, tanks[1], 6, 7, before -> { });
        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, maxPower);
        rocketType = rocketType();

        if (!starting || !canLaunch()) {
            countdown = maxCount;
            starting = false;
        } else if (countdown > 0) {
            countdown--;
            if (countdown % 100 == 0 && countdown > 0) {
                level.playSound(null, getBlockPos(), ModPowerGenSounds.ALARM_HATCH.get(), SoundSource.BLOCKS, 100F, 1.1F);
            }
        } else {
            liftOff(level);
        }

        networkPackNT();
    }

    /**
     * 1.7's client half of {@code updateEntity}: the looping "soyuzReady" hum while counting down, and the smoke
     * ring under the pad while a rocket is still inside the tower.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntitySoyuzLauncher launcher) {
        if (!launcher.starting || !launcher.canLaunch()) {
            if (launcher.audio != null) {
                launcher.audio.stopSound();
                launcher.audio = null;
            }
            launcher.countdown = maxCount;
        } else if (launcher.countdown > 0) {
            if (launcher.audio == null || !launcher.audio.isPlaying()) {
                if (launcher.audio != null) launcher.audio.stopSound();
                launcher.audio = AudioSystem.getLoopedSound(ModSounds.SOYUZ_READY.get(), SoundSource.BLOCKS,
                        pos.getX(), pos.getY(), pos.getZ(), 2.0F, 100F, 1.0F, 20);
                if (launcher.audio != null) {
                    launcher.audio.updateVolume(100);
                    launcher.audio.startSound();
                }
            }
            if (launcher.audio != null) launcher.audio.keepAlive();
            launcher.countdown--;
        }

        List<EntitySoyuz> entities = level.getEntitiesOfClass(EntitySoyuz.class, new AABB(
                pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 10, pos.getZ() + 1.5));

        if (!entities.isEmpty()) {
            ClientEffects.spawnSmokeShockRand(level, pos.getX() + 0.5, pos.getY() - 3, pos.getZ() + 0.5,
                    50, level.getRandom().nextGaussian() * 3 + 6);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    /** The ring of ports around the apron's edge, at pad level and one below it. */
    public List<DirPos> getConPos() {
        if (conPos != null) return conPos;

        conPos = new ArrayList<>();
        BlockPos pos = getBlockPos();
        // 1.7: the core's own course and the one below it, which is the apron's rim
        int ground = pos.getY();
        for (ForgeDirection dir : new ForgeDirection[]{Library.POS_X, Library.POS_Z, Library.NEG_X, Library.NEG_Z}) {
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            for (int i = -6; i <= 6; i++) {
                int x = pos.getX() + dir.offsetX * 7 + rot.offsetX * i;
                int z = pos.getZ() + dir.offsetZ * 7 + rot.offsetZ * i;
                conPos.add(new DirPos(x, ground, z, dir));
                conPos.add(new DirPos(x, ground - 1, z, dir));
            }
        }
        return conPos;
    }

    public void startCountdown() {
        if (canLaunch()) starting = true;
    }

    public void liftOff(Level level) {
        this.starting = false;
        BlockPos pos = getBlockPos();

        int req = this.getFuelRequired();
        int pow = this.getPowerRequired();

        EntitySoyuz soyuz = new EntitySoyuz(ModNtmEntities.SOYUZ.get(), level);
        soyuz.setSkin(Math.max(0, rocketType()));
        soyuz.mode = this.mode;
        soyuz.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0F, 0F);
        level.addFreshEntity(soyuz);

        level.playSound(null, pos, ModSounds.SOYUZ_TAKEOFF.get(), SoundSource.BLOCKS, 100F, 1.1F);

        tanks[0].setFill(Math.max(0, tanks[0].getFill() - req));
        tanks[1].setFill(Math.max(0, tanks[1].getFill() - req));
        power -= pow;

        if (mode == 0) {
            soyuz.setSat(inventory.getStackInSlot(SLOT_SATELLITE));

            if (this.orbital() == 2) inventory.setStackInSlot(SLOT_ORBITAL, ItemStack.EMPTY);

            inventory.setStackInSlot(SLOT_SATELLITE, ItemStack.EMPTY);
        }

        if (mode == 1) {
            List<ItemStack> payload = new ArrayList<>();

            for (int i = SLOT_CARGO; i < 27; i++) {
                payload.add(inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }

            net.minecraft.world.phys.Vec3 target = designatorTarget();
            soyuz.targetX = target == null ? 0 : (int) target.x;
            soyuz.targetZ = target == null ? 0 : (int) target.z;
            soyuz.setPayload(payload);
        }

        inventory.setStackInSlot(SLOT_ROCKET, ItemStack.EMPTY);
        setChanged();
    }

    public boolean canLaunch() {
        // 1.7: hasRocket() twice, hasOxy() never
        return hasRocket() && hasFuel() && hasRocket() && hasPower() && designator() != 1 && orbital() != 1
                && satellite() != 1;
    }

    public boolean hasRocket() {
        return inventory.getStackInSlot(SLOT_ROCKET).getItem() instanceof ItemSoyuz;
    }

    public boolean hasFuel() {
        return tanks[0].getFill() >= getFuelRequired();
    }

    public boolean hasOxy() {
        return tanks[1].getFill() >= getFuelRequired();
    }

    public int getFuelRequired() {
        if (mode == 1) return Math.min(5000 + getDist(), 128_000);
        return 128_000;
    }

    public int getDist() {
        if (designator() == 2) {
            net.minecraft.world.phys.Vec3 target = designatorTarget();
            if (target == null) return 0;
            int x = (int) target.x;
            int z = (int) target.z;
            BlockPos pos = getBlockPos();
            return (int) Math.sqrt((double) (pos.getX() - x) * (pos.getX() - x) + (double) (pos.getZ() - z) * (pos.getZ() - z));
        }
        return 0;
    }

    private net.minecraft.world.phys.@Nullable Vec3 designatorTarget() {
        ItemStack stack = inventory.getStackInSlot(SLOT_DESIGNATOR);
        if (level == null || !(stack.getItem() instanceof IDesignatorItem designator)) return null;
        return designator.getCoords(level, stack, getBlockPos());
    }

    public boolean hasPower() {
        return power >= getPowerRequired();
    }

    public int getPowerRequired() {
        return (int) (maxPower * 0.75);
    }

    /** 0: designator not required, 1: required but not present, 2: present. */
    public int designator() {
        if (mode == 0) return 0;
        ItemStack stack = inventory.getStackInSlot(SLOT_DESIGNATOR);
        if (level != null && stack.getItem() instanceof IDesignatorItem designator
                && designator.isReady(level, stack, getBlockPos())) return 2;
        return 1;
    }

    /** 0: satellite not required, 1: required but not present, 2: present. */
    public int satellite() {
        if (mode == 1) return 0;
        if (!inventory.getStackInSlot(SLOT_SATELLITE).isEmpty()) return 2;
        return 1;
    }

    /**
     * 0: orbital module not required, 1: required but not present, 2: present. Gerald and the legacy lunar miner
     * chip need the lander ({@code missile_soyuz_lander}) in slot 3, which the launch then consumes.
     */
    public int orbital() {
        if (mode == 1) return 0;
        ItemStack sat = inventory.getStackInSlot(SLOT_SATELLITE);
        if (sat.is(ModSatelliteItems.sat_gerald.get())
                || sat.is(ModSatelliteItems.sat_lunar_miner.get())) {
            if (inventory.getStackInSlot(SLOT_ORBITAL).is(ModItems.missile_soyuz_lander.get())) return 2;
            return 1;
        }
        return 0;
    }

    private int rocketType() {
        ItemStack stack = inventory.getStackInSlot(SLOT_ROCKET);
        if (!(stack.getItem() instanceof ItemSoyuz rocket)) return -1;
        var type = rocket.typeOf(stack);
        return type == null ? -1 : type.ordinal();
    }

    public long getPowerScaled(long i) {
        return (power * i) / maxPower;
    }

    /** 1.7 AuxButtonPacket: id 0 sets the mode to {@code value}, id 1 starts the countdown. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        if (meta == 0) this.mode = (byte) value;
        if (meta == 1) this.startCountdown();
        setChanged();
    }

    /** 1.7 TileEntityMachineBase.isItemValidForSlot: false, so nothing is piped in. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { this.power = i; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UNKNOWN; }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && dir != ForgeDirection.UP && dir != ForgeDirection.DOWN;
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeByte(mode);
        buf.writeBoolean(starting);
        buf.writeInt(countdown);
        buf.writeInt(rocketType);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        mode = buf.readByte();
        starting = buf.readBoolean();
        countdown = buf.readInt();
        rocketType = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putByte("mode", mode);
        tanks[0].writeToNBT(output, "fuel");
        tanks[1].writeToNBT(output, "oxidizer");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        mode = input.getByteOr("mode", (byte) 0);
        tanks[0].readFromNBT(input, "fuel");
        tanks[1].readFromNBT(input, "oxidizer");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerSoyuzLauncher(id, playerInventory, this);
    }
}
