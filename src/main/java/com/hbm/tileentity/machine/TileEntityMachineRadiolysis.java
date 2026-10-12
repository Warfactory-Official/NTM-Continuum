package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import com.hbm.inventory.recipes.RadiolysisRecipes.RadiolysisRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.ItemRTGPelletDepleted;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.RTGUtil;
import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import com.hbm.inventory.container.ContainerRadiolysis;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;

/**
 * The RTG that also cracks fluids and sterilises items: 10 pellet slots, fluid identifier in/out (10/11),
 * sterilisation in/out (12/13) and a battery slot (14). 1:1 with the 1.7 {@code TileEntityMachineRadiolysis}.
 */
public class TileEntityMachineRadiolysis extends TileEntityMachineBase
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final int maxPower = 1_000_000;
    /** The 1.7 {@code ntmContagion} item tag: nothing in the port sets it yet, so sterilisation waits on it. */
    public static final GolemComponent<Boolean> CONTAGION = GolemComponent.of("hbm", "ntm_contagion", Codec.BOOL);

    public long power;
    public int heat;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.NONE, 2_000),
            new FluidTankNTM(Fluids.NONE, 2_000),
            new FluidTankNTM(Fluids.NONE, 2_000)
    };

    private static final int[] slot_io = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 13};
    private static final int[] slot_rtg = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public TileEntityMachineRadiolysis(BlockPos pos, BlockState state) {
        super(ModNuclearMiscBlockEntities.RADIOLYSIS.get(), pos, state, 15); // 10 rtg slots, 2 fluid ID slots (io), 2 irradiation slots (io), battery slot
    }

    @Override
    public String getDefaultName() {
        return "container.radiolysis";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRadiolysis te) {
        te.power = Library.chargeItemsFromTE(te.inventory, 14, te.power, maxPower);

        te.heat = RTGUtil.updateRTGs(te.inventory, slot_rtg);
        te.power += te.heat * 10L;

        if (te.power > maxPower) te.power = maxPower;

        te.setTypeFromFluidId();
        te.setupTanks();

        if (te.heat > 100) {
            int crackTime = (int) Math.max(-0.1 * (te.heat - 100) + 30, 5);

            if (level.getGameTime() % crackTime == 0) te.crack();

            if (te.heat >= 200 && level.getGameTime() % 100 == 0) te.sterilize();
        }

        for (DirPos con : te.getConPos()) {
            te.tryProvide(level, con.getPos(), con.getDir());
            te.trySubscribe(te.tanks[0].getTankType(), level, con);
            if (te.tanks[1].getFill() > 0) te.tryProvide(te.tanks[1], level, con);
            if (te.tanks[2].getFill() > 0) te.tryProvide(te.tanks[2], level, con);
        }

        te.setChanged();
        te.networkPackNT();
    }

    /** 1.7 {@code tanks[0].setType(10, 11, slots)}: the identifier passes from slot 10 to slot 11 once used. */
    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(10);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        if (!inventory.getStackInSlot(11).isEmpty()) return;
        FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type == tanks[0].getTankType()) return;
        tanks[0].setTankType(type);
        inventory.setStackInSlot(11, id.copy());
        inventory.setStackInSlot(10, ItemStack.EMPTY);
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

    /* Processing Methods */
    private void crack() {
        RadiolysisRecipe quart = RadiolysisRecipes.getRadiolysis(tanks[0].getTankType());

        if (quart != null) {
            int left = quart.left().fill;
            int right = quart.right().fill;

            if (tanks[0].getFill() >= 100 && hasSpace(left, right)) {
                tanks[0].setFill(tanks[0].getFill() - 100);
                tanks[1].setFill(tanks[1].getFill() + left);
                tanks[2].setFill(tanks[2].getFill() + right);
            }
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[1].getFill() + left <= tanks[1].getMaxFill() && tanks[2].getFill() + right <= tanks[2].getMaxFill();
    }

    private void setupTanks() {
        RadiolysisRecipe quart = RadiolysisRecipes.getRadiolysis(tanks[0].getTankType());

        if (quart != null) {
            tanks[1].setTankType(quart.left().type);
            tanks[2].setTankType(quart.right().type);
        } else {
            tanks[0].setTankType(Fluids.NONE);
            tanks[1].setTankType(Fluids.NONE);
            tanks[2].setTankType(Fluids.NONE);
        }
    }

    // Code: pressure, sword, sterilize.
    private void sterilize() {
        ItemStack input = inventory.getStackInSlot(12);
        if (input.isEmpty()) return;

        // 1.7 eats one ItemFood per cycle, except the pancake; the canned conserves were never ItemFood
        if (ItemStacks.foodProperties(input) != null
                && !input.is(com.hbm.items.ModFinalConsumableItems.pancake.get())
                && !(input.getItem() instanceof com.hbm.items.food.ItemConserve)) {
            inventory.extractItem(12, 1, false);
            input = inventory.getStackInSlot(12);
        }

        if (!checkIfValid()) return;

        ItemStack output = input.copy();
        output.setCount(1);
        CONTAGION.remove(output);

        ItemStack existing = inventory.getStackInSlot(13);
        if (existing.isEmpty()) {
            inventory.extractItem(12, 1, false);
            inventory.setStackInSlot(13, output);
        } else if (ItemStacks.sameItemAndData(existing, output) && existing.getCount() + output.getCount() <= existing.getMaxStackSize()) {
            inventory.extractItem(12, 1, false);
            existing.grow(output.getCount());
        }
    }

    private boolean checkIfValid() {
        ItemStack input = inventory.getStackInSlot(12);
        return !input.isEmpty() && CONTAGION.getOr(input, false);
    }

    /* IO Methods */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 12 || (slot < 10 && stack.getItem() instanceof ItemRTGPellet)
                || slot == 10 || slot == 14;
    }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return slot_io; }
    @Override public int[] getSlotsForFace(Direction side) { return slot_io; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 12 || (slot < 10 && stack.getItem() instanceof ItemRTGPellet);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot < 10 && stack.getItem() instanceof ItemRTGPelletDepleted) || slot == 13;
    }

    /* Power methods */
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && dir != ForgeDirection.DOWN;
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerRadiolysis(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(heat);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        tanks[2].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        heat = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        tanks[2].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("heat", heat);
        tanks[0].writeToNBT(output, "input");
        tanks[1].writeToNBT(output, "output1");
        tanks[2].writeToNBT(output, "output2");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        heat = input.getIntOr("heat", 0);
        tanks[0].readFromNBT(input, "input");
        tanks[1].readFromNBT(input, "output1");
        tanks[2].readFromNBT(input, "output2");
    }
}
