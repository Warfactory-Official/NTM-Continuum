package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.container.ContainerStorageDrum;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.StorageDrumRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Exact 24-slot waste transformer with separate liquid and gas waste tanks. */
public class TileEntityStorageDrum extends TileEntityMachineBase implements IFluidStandardSenderMK2,
        WorldlyContainer, MenuProvider {

    private static final int[] SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };
    private static final float DECAY_RATE = 0.9965402628F;

    public final FluidTankNTM liquid = new FluidTankNTM(Fluids.WASTEFLUID, 16_000);
    public final FluidTankNTM gas = new FluidTankNTM(Fluids.WASTEGAS, 16_000);
    /** Legacy array order: [liquid waste, gaseous waste]. */
    public final FluidTankNTM[] tanks = {liquid, gas};
    public int age;

    public TileEntityStorageDrum(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_STORAGE_DRUM.get(), pos, state, SLOTS.length);
        StorageDrumRecipes.registerRecipes();
    }

    @Override
    public String getDefaultName() {
        return "container.storageDrum";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityStorageDrum drum) {
        double rad = 0D;
        int liquid = 0;
        int gas = 0;

        for (int slot : SLOTS) {
            ItemStack stack = drum.inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            if (TickPhase.every(level, pos, 20)) rad += HazardSystem.getRawRadsFromStack(stack);

            int[] waste = StorageDrumRecipes.getWaste(stack);
            if (waste != null) {
                if (level.getRandom().nextInt(waste[0]) == 0) {
                    ItemStack output = StorageDrumRecipes.getOutput(stack);
                    if (output != null) {
                        liquid += waste[1];
                        gas += waste[2];
                        drum.inventory.setStackInSlot(slot, output.copy());
                    }
                }
            } else {
                ContaminationUtil.neutronActivateItem(stack, 0.0F, DECAY_RATE);
            }
        }

        int liquidOverflow = Math.max(drum.liquid.getFill() + liquid - drum.liquid.getMaxFill(), 0);
        int gasOverflow = Math.max(drum.gas.getFill() + gas - drum.gas.getMaxFill(), 0);
        if (liquidOverflow > 0) {
            ChunkRadiationManager.proxy.incrementRad(level, pos, liquidOverflow * 0.5F);
        }
        if (gasOverflow > 0) {
            ChunkRadiationManager.proxy.incrementRad(level, pos, gasOverflow * 0.5F);
        }

        drum.liquid.fill(Fluids.WASTEFLUID, liquid, true);
        drum.gas.fill(Fluids.WASTEGAS, gas, true);

        drum.age++;
        if (drum.age >= 20) drum.age -= 20;
        if (drum.age == 9 || drum.age == 19) drum.sendTank(drum.liquid);
        if (drum.age == 8 || drum.age == 18) drum.sendTank(drum.gas);
        drum.syncState();

        if (rad > 0) {
            // Preserve the legacy call's X/Z argument order; old storage drums emitted at (z,y,x).
            ContaminationUtil.radiate(level, pos.getZ(), pos.getY(), pos.getX(), 32, (float) rad);
        }
    }

    private void sendTank(FluidTankNTM tank) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            sendTank(tank, new DirPos(worldPosition.relative(direction.toDirection()), direction));
        }
    }

    private void sendTank(FluidTankNTM tank, DirPos connection) {
        tryProvide(tank, level, connection.getPos(), connection.getDir().toDirection());
    }

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return !ContaminationUtil.isContaminated(stack) && StorageDrumRecipes.getOutput(stack) == null;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return StorageDrumRecipes.getOutput(stack) != null || ContaminationUtil.isContaminated(stack);
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.storageDrum");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerStorageDrum(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        liquid.serialize(buf);
        gas.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        liquid.deserialize(buf);
        gas.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        liquid.writeToNBT(output, "liquid");
        gas.writeToNBT(output, "gas");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        liquid.readFromNBT(input, "liquid");
        gas.readFromNBT(input, "gas");
    }
}
