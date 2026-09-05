package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.inventory.container.ContainerMachineRTG;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModRtgBlockEntities;
import com.hbm.tileentity.TileEntityInventoryBase;
import com.hbm.util.RTGUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
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

/** The legacy grey RTG's 15 fuel cells, 6,000-heat cap, and one-MHE output buffer. */
public class TileEntityMachineRTG extends TileEntityInventoryBase
        implements MenuProvider, IEnergyProviderMK2, IEnergyConnectorMK2 {
    public static final int SLOT_COUNT = 15;
    public static final int HEAT_MAX = 6_000;
    public static final long MAX_POWER = 1_000_000L;
    private static final int[] FUEL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};

    public int heat;
    public long power;

    public TileEntityMachineRTG(BlockPos pos, BlockState state) {
        super(ModRtgBlockEntities.MACHINE_RTG.get(), pos, state, SLOT_COUNT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRTG rtg) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            rtg.tryProvide(level, pos.offset(direction.offsetX, direction.offsetY, direction.offsetZ), direction);
        }
        rtg.heat = Math.min(RTGUtil.updateRTGs(rtg.inventory, FUEL_SLOTS), HEAT_MAX);
        rtg.power = Math.min(rtg.power + rtg.heat * 5L, MAX_POWER);
        rtg.setChanged();
        rtg.networkPackNT();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemRTGPellet;
    }

    public int getHeatScaled(int pixels) { return heat * pixels / HEAT_MAX; }
    public long getPowerScaled(long pixels) { return power * pixels / MAX_POWER; }
    public boolean hasHeat() { return heat > 0; }
    public boolean hasPower() { return power > 0; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }

    @Override
    public Component getDisplayName() {
        return Component.literal("RT Generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineRTG(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeLong(power);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        power = buf.readLong();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("heat", heat);
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        heat = input.getIntOr("heat", 0);
        power = input.getLongOr("power", 0L);
    }
}
