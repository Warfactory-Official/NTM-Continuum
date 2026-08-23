package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineGasFlare;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Gas Flare (Flare Stack) — 1:1 port of the 1.12.2 {@code TileEntityMachineGasFlare}: a 12-tall 3×3
 * chimney that either <b>vents</b> gaseous fluids (no power) or <b>burns</b> flammable fluids for power.
 * 6 slots (0 battery-to-charge, 1→2 fluid container drain, 3 fluid identifier, 4/5 upgrades: SPEED = rate,
 * EFFECT = yield). maxPower 1M. {@code isOn}/{@code doesBurn} toggle via GUI buttons.
 *
 * <p>Vent: {@code min(50·(1+speed), fill)} mB, no power. Burn: {@code min(10·(1+speed), fill)} mB →
 * {@code power += flammableEnergy·eject/1000/penalty · (1 + yield/3)}, penalty 5 gaseous / 10 liquid.
 * Effects use vanilla particle stand-ins (the old custom gas-tower / flamethrower particles are unported,
 * §20): a smoke column while venting, FLAME + LARGE_SMOKE at the tip while burning; burning also sets
 * fire to entities in the flame AABB.
 */
public class TileEntityMachineGasFlare extends TileEntityMachineBase
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardReceiverMK2, IUpgradeInfoProvider {

    public static final long maxPower = 1000000;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    public long power;
    public FluidTankNTM tank;
    public boolean isOn = false;
    public boolean doesBurn = false;

    public TileEntityMachineGasFlare(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_GAS_FLARE.get(), pos, state, 6);
        tank = new FluidTankNTM(Fluids.GAS, 64000);
    }

    @Override
    public String getDefaultName() {
        return "container.gasFlare";
    }

    public long getPowerScaled(long i) {
        return (power * i) / maxPower;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineGasFlare te) {
        for (DirPos dp : te.getConPos()) {
            te.tryProvide(level, dp.getPos(), dp.getDir());
            te.trySubscribe(te.tank.getTankType(), level, dp);
        }

        te.setTypeFromFluidId();
        te.loadTank();

        int maxVent = 50;
        int maxBurn = 10;

        if (te.isOn && te.tank.getFill() > 0) {
            te.upgradeManager.checkSlots(te.inventory, 4, 5);
            int burn = te.upgradeManager.getLevel(UpgradeType.SPEED);
            int yield = te.upgradeManager.getLevel(UpgradeType.EFFECT);

            maxVent += maxVent * burn;
            maxBurn += maxBurn * burn;

            FluidType type = te.tank.getTankType();
            boolean flammable = FluidCombustion.isFlammable(type);

            if (!te.doesBurn || !flammable) {
                // vent path (gaseous only)
                if (FluidCombustion.isGaseous(type)) {
                    int eject = Math.min(maxVent, te.tank.getFill());
                    te.tank.setFill(te.tank.getFill() - eject);

                    if (level.getGameTime() % 7 == 0) {
                        level.playSound(null, pos.getX(), pos.getY() + 11, pos.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, te.getVolume(1.5F), 0.5F);
                    }
                }
            } else {
                // burn path (flammable)
                int eject = Math.min(maxBurn, te.tank.getFill());
                te.tank.setFill(te.tank.getFill() - eject);

                int penalty = FluidCombustion.isGaseous(type) ? 5 : 10;

                long powerProd = FluidCombustion.getFlammableEnergy(type) * eject / 1_000;
                powerProd /= penalty;
                powerProd += powerProd * yield / 3;

                te.power = Math.min(te.power + powerProd, maxPower);

                AABB flameBox = new AABB(pos.getX() - 1, pos.getY() + 12, pos.getZ() - 2, pos.getX() + 2, pos.getY() + 17, pos.getZ() + 2);
                List<Entity> list = level.getEntitiesOfClass(Entity.class, flameBox);
                for (Entity e : list) {
                    dev.golem.core.world.Effects.ignite(e, 5);
                    e.hurt(level.damageSources().onFire(), 5F);
                }
            }
        }

        te.power = Library.chargeItemsFromTE(te.inventory, 0, te.power, maxPower);
        te.networkPackNT();
    }

    /** Client: vanilla-particle stand-ins for the old custom gas-tower / flame particles (§20). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineGasFlare te) {
        if (!te.isOn || te.tank.getFill() <= 0) return;
        FluidType type = te.tank.getTankType();
        boolean flammable = FluidCombustion.isFlammable(type);

        if ((!te.doesBurn || !flammable) && FluidCombustion.isGaseous(type)) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 11, pos.getZ() + 0.5,
                    level.getRandom().nextGaussian() * 0.02, 0.05, level.getRandom().nextGaussian() * 0.02);
        } else if (te.doesBurn && flammable) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 11.75, pos.getZ() + 0.5,
                    level.getRandom().nextGaussian() * 0.05, 0.1, level.getRandom().nextGaussian() * 0.05);
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 12.5, pos.getZ() + 0.5,
                    0, 0.1, 0);
        }
    }

    private void loadTank() {
        ItemStack in = inventory.getStackInSlot(1);
        if (in.isEmpty()) return;

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);
            if (held <= 0 || heldType == Fluids.NONE) return;
            int addable = tank.fill(heldType, held, false);
            if (addable <= 0) return;
            ItemStack work = in.copy();
            work.setCount(1);
            int taken = fillable.tryEmpty(heldType, addable, work);
            if (taken <= 0 || !inventory.insertItem(2, work.copy(), true).isEmpty()) return;
            tank.fill(heldType, taken, true);
            inventory.extractItem(1, 1, false);
            inventory.insertItem(2, work, false);
            setChanged();
        } else if (in.getItem() instanceof ItemFluidBucket) {
            FluidType fluid = ItemFluidBucket.getFluid(in);
            if (fluid == Fluids.NONE) return;
            if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!inventory.insertItem(2, empty.copy(), true).isEmpty()) return;
            tank.fill(fluid, ItemFluidBucket.VOLUME, true);
            inventory.extractItem(1, 1, false);
            inventory.insertItem(2, empty, false);
            setChanged();
        }
    }

    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(3);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tank.getTankType()) {
            tank.setTankType(t);
            setChanged();
        }
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z, Library.POS_X),
                new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y, z + 2, Library.POS_Z),
                new DirPos(x, y, z - 2, Library.NEG_Z)
        };
    }

    /** GUI buttons: 0 = valve (isOn), 1 = ignition (doesBurn). */
    @Override
    public void handleButtonPacket(int value, int meta) {
        switch (value) {
            case 0 -> this.isOn = !this.isOn;
            case 1 -> this.doesBurn = !this.doesBurn;
        }
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 3) return stack.getItem() instanceof IItemFluidId;
        if (slot >= 4 && slot <= 5) return Library.isMachineUpgrade(stack);
        // Slot 2 (emptied-container out) stays TRUE — the machine fills it via inventory.insertItem,
        // which validates against this method; the menu's take-only slot blocks the player.
        return true;
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{4, 5};
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { power = i; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.EFFECT;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_flare.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(KEY_EFFICIENCY, "+" + (100 * level / 3) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public java.util.HashMap<UpgradeType, Integer> getValidUpgrades() {
        java.util.HashMap<UpgradeType, Integer> upgrades = new java.util.HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.EFFECT, 3);
        return upgrades;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.gasFlare");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineGasFlare(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(this.power);
        buf.writeBoolean(isOn);
        buf.writeBoolean(doesBurn);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.isOn = buf.readBoolean();
        this.doesBurn = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("powerTime", power);
        output.putBoolean("isOn", isOn);
        output.putBoolean("doesBurn", doesBurn);
        tank.writeToNBT(output, "gas");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("powerTime", 0L);
        isOn = input.getBooleanOr("isOn", false);
        doesBurn = input.getBooleanOr("doesBurn", false);
        tank.readFromNBT(input, "gas");
    }
}
