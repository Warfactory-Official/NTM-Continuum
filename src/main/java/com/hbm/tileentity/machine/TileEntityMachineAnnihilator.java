package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.EntityEffects;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import com.hbm.inventory.container.ContainerMachineAnnihilator;
import com.hbm.saveddata.AnnihilatorSavedData;
import com.hbm.tileentity.ModProduction2BlockEntities;

/**
 * 1.7 {@code TileEntityMachineAnnihilator}: destroys everything fed into it and credits the named pool.
 * Slots: 0 trash input, 1 fluid identifier, 2-7 milestone payouts, 8 monitor, 9 payout request, 10 payout.
 */
public final class TileEntityMachineAnnihilator extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardReceiverMK2, IControlReceiver, WorldlyContainer {

    public String pool = "Recycling";
    public int timer;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 2_500_000);
    public BigInteger monitorBigInt = BigInteger.ZERO;

    private static final int[] SLOT_ACCESS = new int[] {0, 2, 3, 4, 5, 6, 7};

    public TileEntityMachineAnnihilator(BlockPos pos, BlockState state) {
        super(ModProduction2BlockEntities.ANNIHILATOR.get(), pos, state, 11);
    }

    @Override
    public String getDefaultName() {
        return "container.annihilator";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAnnihilator te) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        te.setTankTypeFromSlot(1);

        if (te.pool != null && !te.pool.isEmpty()) {

            AnnihilatorSavedData data = AnnihilatorSavedData.getData(serverLevel);
            boolean didSomething = false;

            ItemStack trash = te.inventory.getStackInSlot(0);
            if (!trash.isEmpty()) {
                te.onDestroy(trash);
                te.tryAddPayout(data.pushToPool(te.pool, trash, false));
                te.inventory.setStackInSlot(0, ItemStack.EMPTY);
                te.setChanged();
                didSomething = true;
            }
            if (te.tank.getFill() > 0) {
                // 1.7: FT_Polluting.pollute(..., FluidReleaseType.BURN, fill * 2) - the pollution system is not ported.
                te.tryAddPayout(data.pushToPool(te.pool, te.tank.getTankType(), te.tank.getFill(), false));
                te.tank.setFill(0);
                te.setChanged();
                didSomething = true;
            }

            if (didSomething) {
                ForgeDirection dir = MachineCore.facing(state);
                double fx = pos.getX() + 0.5 - dir.offsetX * 3, fy = pos.getY() + 8.75, fz = pos.getZ() + 0.5 - dir.offsetZ * 3;
                var rand = level.getRandom();
                EntityEffects.gasFlame(level, fx, fy, fz, rand.nextGaussian() * 0.05, 0.1, rand.nextGaussian() * 0.05,
                        6.5F, 150D); // 1.7 ParticleUtil.spawnGasFlame sets no scale; the gasfire handler defaults to 6.5

                if (level.getGameTime() % 3 == 0)
                    level.playSound(null, fx, fy, fz, ModSounds.FLAMETHROWER_SHOOT.get(), SoundSource.BLOCKS,
                            te.getVolume(1F), 0.5F + rand.nextFloat() * 0.25F);
            }

            ItemStack monitor = te.inventory.getStackInSlot(8);
            if (!monitor.isEmpty()) {
                if (monitor.getItem() instanceof IItemFluidId) {
                    FluidType type = ItemFluidId.getFluid(monitor);
                    te.monitor(data, type);
                } else {
                    te.monitor(data, monitor.getItem());
                }
            }

            ItemStack request = te.inventory.getStackInSlot(9);
            if (!request.isEmpty()) {
                ItemStack single = request.copy();
                single.setCount(1);
                te.onDestroy(single);
                ItemStack payout = data.pushToPool(te.pool, single, true);
                te.inventory.extractItem(9, 1, false);
                if (payout != null) {
                    ItemStack out = te.inventory.getStackInSlot(10);
                    if (out.isEmpty()) {
                        te.inventory.setStackInSlot(10, payout);
                    } else if (ItemStacks.sameItemAndData(out, payout) && out.getMaxStackSize() >= out.getCount() + payout.getCount()) {
                        out.grow(payout.getCount());
                        te.inventory.setStackInSlot(10, out);
                    }
                }
            }
        }

        te.syncState();
    }

    /** 1.7 {@code FluidTank.setType(slot, slots)}: a fluid identifier in the slot retypes the tank. */
    private void setTankTypeFromSlot(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.getItem() instanceof ItemFluidId) {
            FluidType type = ItemFluidId.getFluid(stack);
            if (type != tank.getTankType()) tank.setTankType(type);
        }
    }

    public void onDestroy(ItemStack stack) {
        double radiation = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
        if (radiation > 0) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ChunkRadiationManager.proxy.incrementRad(level,
                    new BlockPos(worldPosition.getX() - dir.offsetX * 3, worldPosition.getY() + 9, worldPosition.getZ() - dir.offsetZ * 3),
                    Math.min(radiation * 5F, 1_000F));
        }
    }

    public void monitor(AnnihilatorSavedData data, Object type) {
        AnnihilatorSavedData.AnnihilatorPool pool = data.pools.get(this.pool);
        if (pool != null) {
            this.monitorBigInt = pool.items.get(type);
            if (this.monitorBigInt == null) this.monitorBigInt = BigInteger.ZERO;
        } else {
            this.monitorBigInt = BigInteger.ZERO;
        }
    }

    public void tryAddPayout(@Nullable ItemStack payout) {
        if (payout == null) return;

        for (int i = 2; i <= 7; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStacks.sameItemAndData(slot, payout) && slot.getMaxStackSize() >= slot.getCount() + payout.getCount()) {
                slot.grow(payout.getCount());
                inventory.setStackInSlot(i, slot);
                this.setChanged();
                return;
            }
        }

        for (int i = 2; i <= 7; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, payout);
                this.setChanged();
                return;
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true; // trash
        if (slot == 1 && stack.getItem() instanceof IItemFluidId) return true;
        if (slot == 8) return true; // monitor
        if (slot == 9) return true; // payout request
        return false;
    }

    @Override public int[] getSlotsForFace(Direction side) { return SLOT_ACCESS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return SLOT_ACCESS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 2 && slot <= 7; }

    @Override
    protected void loadData(DataIn nbt) {
        super.loadData(nbt);
        this.tank.readFromNBT(nbt, "t");
        this.pool = nbt.getStringOr("pool", "");
    }

    @Override
    protected void saveData(DataOut nbt) {
        super.saveData(nbt);
        this.tank.writeToNBT(nbt, "t");
        nbt.putString("pool", pool == null ? "" : pool);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeUtf(this.pool == null ? "" : this.pool);
        byte[] array = this.monitorBigInt.toByteArray();
        buf.writeInt(array.length);
        for (byte b : array) buf.writeByte(b);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.pool = buf.readUtf();
        byte[] array = new byte[buf.readInt()];
        for (int i = 0; i < array.length; i++) array[i] = buf.readByte();
        this.monitorBigInt = new BigInteger(array);
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {tank}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tank}; }
    @Override public com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority getFluidPriority() { return com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority.LOW; }

    @Override public Component getDisplayName() { return Component.translatable("container.annihilator"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineAnnihilator(containerId, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("pool")) {
            String pool = dev.golem.core.data.Tags.getStringOr(data, "pool", "");
            if (pool != null && !pool.isEmpty()) {
                this.pool = pool;
                this.setChanged();
            }
        }
    }
}
