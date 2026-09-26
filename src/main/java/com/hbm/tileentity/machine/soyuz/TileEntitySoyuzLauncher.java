package com.hbm.tileentity.machine.soyuz;

import net.minecraft.core.Direction;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.inventory.container.ContainerSoyuzLauncher;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ISatChip;
import com.hbm.items.special.ItemSoyuz;
import com.hbm.blocks.machine.soyuz.SoyuzLauncher;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The launch complex's controls: a rocket, a satellite to put on it, kerosene, oxygen and power. With all
 * five present the countdown can be started, and thirty seconds later the rocket leaves with the satellite
 * on board.
 *
 * <p>Deviations from CE: only the satellite flight is ported, so the cargo mode and everything it needs —
 * the designator that aims it, the return capsule and its landing pad — are absent, and with them the
 * launcher's mode switch. The orbital-module check for the Horizons probe goes with that satellite.
 */
public class TileEntitySoyuzLauncher extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2 {

    public static final long maxPower = 1_000_000;
    /** Thirty seconds of hold-down, in ticks. */
    public static final int maxCount = 600;
    public static final int FUEL_REQUIRED = 128_000;

    /** 0 rocket, 1 satellite, 2 kerosene in, 3 kerosene out, 4 oxygen in, 5 oxygen out, 6 battery. */
    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_SATELLITE = 1;
    public static final int SLOT_BATTERY = 6;

    public long power;
    public FluidTankNTM[] tanks;
    public boolean starting;
    public int countdown = maxCount;
    public int rocketType = -1;

    private List<DirPos> conPos;

    public TileEntitySoyuzLauncher(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOYUZ_LAUNCHER.get(), pos, state, 7);
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
        com.hbm.inventory.fluid.tank.CanisterBays.process(inventory, tanks[0], 2, 3, before -> { });
        com.hbm.inventory.fluid.tank.CanisterBays.process(inventory, tanks[1], 4, 5, before -> { });
        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, maxPower);
        rocketType = rocketType();

        if (!starting || !canLaunch()) {
            countdown = maxCount;
            starting = false;
        } else if (countdown > 0) {
            countdown--;
            if (countdown % 100 == 0 && countdown > 0) {
                level.playSound(null, getBlockPos(), com.hbm.sound.ModPowerGenSounds.ALARM_HATCH.get(), SoundSource.BLOCKS, 100F, 1.1F);
            }
        } else {
            liftOff(level);
        }

        syncState();
    }

    /** The ring of ports around the apron's edge, at pad level and one below it. */
    public List<DirPos> getConPos() {
        if (conPos != null) return conPos;

        conPos = new ArrayList<>();
        BlockPos pos = getBlockPos();
        // 1.7 states this ring at the core's own level, four courses above the apron it belongs to, so
        // upstream's ports sit in open air and nothing can reach them. The ring is stated at the apron.
        int ground = pos.getY() - SoyuzLauncher.HEIGHT;
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

        EntitySoyuz soyuz = new EntitySoyuz(ModNtmEntities.SOYUZ.get(), level);
        soyuz.setSkin(Math.max(0, rocketType()));
        soyuz.setSat(inventory.getStackInSlot(SLOT_SATELLITE));
        soyuz.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0F, 0F);
        level.addFreshEntity(soyuz);

        level.playSound(null, pos, ModSounds.SOYUZ_TAKEOFF.get(), SoundSource.BLOCKS, 100F, 1.1F);

        tanks[0].setFill(tanks[0].getFill() - FUEL_REQUIRED);
        tanks[1].setFill(tanks[1].getFill() - FUEL_REQUIRED);
        power -= getPowerRequired();

        inventory.setStackInSlot(SLOT_SATELLITE, ItemStack.EMPTY);
        inventory.setStackInSlot(SLOT_ROCKET, ItemStack.EMPTY);
        setChanged();
    }

    public boolean canLaunch() {
        return hasRocket() && hasSatellite() && hasFuel() && hasOxy() && hasPower();
    }

    public boolean hasRocket() {
        return inventory.getStackInSlot(SLOT_ROCKET).getItem() instanceof ItemSoyuz;
    }

    public boolean hasSatellite() {
        return inventory.getStackInSlot(SLOT_SATELLITE).getItem() instanceof ISatChip;
    }

    public boolean hasFuel() {
        return tanks[0].getFill() >= FUEL_REQUIRED;
    }

    public boolean hasOxy() {
        return tanks[1].getFill() >= FUEL_REQUIRED;
    }

    public boolean hasPower() {
        return power >= getPowerRequired();
    }

    public int getPowerRequired() {
        return (int) (maxPower * 0.75);
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

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_ROCKET) return stack.getItem() instanceof ItemSoyuz;
        if (slot == SLOT_SATELLITE) return stack.getItem() instanceof ISatChip;
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        return slot == 2 || slot == 4;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { this.power = i; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public boolean canConnect(Direction dir) { return dir != null; }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return type == tanks[0].getTankType() || type == tanks[1].getTankType();
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
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
        tanks[0].writeToNBT(output, "tank0");
        tanks[1].writeToNBT(output, "tank1");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tanks[0].readFromNBT(input, "tank0");
        tanks[1].readFromNBT(input, "tank1");
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
