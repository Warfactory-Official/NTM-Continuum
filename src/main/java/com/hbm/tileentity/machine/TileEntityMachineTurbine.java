package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineTurbine;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
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
import org.jspecify.annotations.Nullable;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Steam Turbine: the original single-block 85%-efficient turbine, generalized over
 * {@link CoolingRecipes}. 1:1 port of the 1.12.2 {@code TileEntityMachineTurbine}: the player picks the
 * steam stage with a fluid identifier (slot 0 to 1), the input tank is drained at most 6000 mB/tick into
 * the cooled-down output fluid, and 85% of the released heat energy becomes HE pushed to all six sides.
 *
 * <p>7 slots: 0/1 fluid identifier in/out, 2/3 input canister bay, 4 battery (charged from the buffer),
 * 5/6 output canister bay.
 */
public class TileEntityMachineTurbine extends TileEntityMachineBase
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final long MAX_POWER = 1_000_000L;
    public static final int MAX_INPUT_PER_TICK = 6_000;
    public final FluidTankNTM input = new FluidTankNTM(Fluids.STEAM, 64_000);
    public final FluidTankNTM output = new FluidTankNTM(Fluids.SPENTSTEAM, 128_000);
    public long power;

    public TileEntityMachineTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TURBINE.get(), pos, state, 7);
    }

    @Override public String getDefaultName() { return "container.machineTurbine"; }

    /** The old {@code getPowerScaled(int)}, the GUI's power-bar fill. */
    public long getPowerScaled(long i) {
        return (power * i) / MAX_POWER;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTurbine te) {
        te.subscribeToAllAround(te.input.getTankType(), te);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            te.tryProvide(level, pos.relative(direction.toDirection()), direction);
        }

        te.updateTypeFromFluidId();
        if (MachineFluidContainerIO.load(te.input, te.inventory, 2, 3)) te.setChanged();
        te.power = Library.chargeItemsFromTE(te.inventory, 4, te.power, MAX_POWER);

        te.convert();

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (te.output.getTankType() != Fluids.NONE) {
                te.tryProvide(te.output, level, pos.relative(direction.toDirection()), direction);
            }
        }
        if (MachineFluidContainerIO.unload(te.output, te.inventory, 5, 6)) te.setChanged();
        te.networkPackNT();
    }

    private void convert() {
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(input.getTankType());
        if (recipe == null) {
            if (input.getFill() == 0) output.setTankType(Fluids.NONE);
            return;
        }
        if (output.getTankType() != recipe.output()) {
            if (output.getFill() != 0) return;
            output.setTankType(recipe.output());
        }
        int ops = Math.min(input.getFill() / recipe.inputAmount(), (output.getMaxFill() - output.getFill()) / recipe.outputAmount());
        ops = Math.min(ops, MAX_INPUT_PER_TICK / recipe.inputAmount());
        if (ops <= 0) return;

        input.setFill(input.getFill() - ops * recipe.inputAmount());
        output.setFill(output.getFill() + ops * recipe.outputAmount());
        power = Math.min(MAX_POWER, power + (long) Math.floor(ops * recipe.heatEnergy() * 0.85D));
        setChanged();
    }

    /**
     * Slot 0 holds a fluid identifier; adopt its type for the input tank and pass the identifier down to
     * slot 1: the 1.12.2 {@code tanksNew[0].setType(0, 1, inventory)}. Retyping resets the output tank so
     * the next {@link #convert()} re-derives it from the new steam stage.
     */
    private void updateTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        if (!inventory.getStackInSlot(1).isEmpty()) return; // out slot must be free to receive it

        FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type == input.getTankType()) return;

        input.setTankType(type); // zeroes the fill, as the original did
        output.setTankType(Fluids.NONE);
        inventory.insertItem(1, id.copy(), false);
        inventory.extractItem(0, 1, false);
        setChanged();
    }

    /**
     * Slots 0/1 take identifiers, 4 a battery. The container bays 2/5 and their outputs 3/6 take anything, as the 1.7
     * GUI's plain slots did; the menu keeps players out of 3/6.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidId;
            case 4 -> Library.isBattery(stack);
            default -> true;
        };
    }

    /** 1.7 sided access: a battery into slot 4 from the top and sides, nothing taken out. */
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{6} : new int[]{4}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 4 && Library.isBattery(stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power = Math.max(0L, Math.min(MAX_POWER, value)); }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{input}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{output}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{input, output}; }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineTurbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineTurbine(containerId, playerInventory, this);
    }

    // --- sync + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        input.serialize(buf);
        output.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        input.deserialize(buf);
        output.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut outputTag) {
        super.saveData(outputTag);
        outputTag.putLong("power", power);
        input.writeToNBT(outputTag, "input");
        output.writeToNBT(outputTag, "output");
    }

    @Override
    protected void loadData(DataIn inputTag) {
        super.loadData(inputTag);
        power = inputTag.getLongOr("power", 0L);
        input.readFromNBT(inputTag, "input");
        output.readFromNBT(inputTag, "output");
    }
}
