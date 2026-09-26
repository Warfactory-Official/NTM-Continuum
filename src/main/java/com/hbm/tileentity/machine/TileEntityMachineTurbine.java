package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineTurbine;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Steam Turbine — the original single-block 85%-efficient turbine, generalized over
 * {@link CoolingRecipes}. 1:1 port of the 1.12.2 {@code TileEntityMachineTurbine}: the player picks the
 * steam stage with a fluid identifier (slot 0 → 1), the input tank is drained at most 6000 mB/tick into
 * the cooled-down output fluid, and 85% of the released heat energy becomes HE pushed to all six sides.
 *
 * <p>7 slots: 0/1 fluid identifier in/out, 2/3 input canister bay, 4 battery (charged from the buffer),
 * 5/6 output canister bay.
 */
public class TileEntityMachineTurbine extends TileEntityMachineBase
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardTransceiverMK2 {

    public static final long MAX_POWER = 1_000_000L;
    public static final int MAX_INPUT_PER_TICK = 6_000;
    public final FluidTankNTM input = new FluidTankNTM(Fluids.STEAM, 64_000);
    public final FluidTankNTM output = new FluidTankNTM(Fluids.SPENTSTEAM, 128_000);
    public long power;

    public TileEntityMachineTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TURBINE.get(), pos, state, 7);
    }

    @Override public String getDefaultName() { return "container.machineTurbine"; }

    /** The old {@code getPowerScaled(int)} — the GUI's power-bar fill. */
    public long getPowerScaled(long i) {
        return (power * i) / MAX_POWER;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTurbine te) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            te.tryProvide(level, pos.relative(direction.toDirection()), direction.toDirection());
        }

        te.updateTypeFromFluidId();
        te.processCanister(te.input, 2, 3);
        te.power = Library.chargeItemsFromTE(te.inventory, 4, te.power, MAX_POWER);

        te.convert();

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (te.output.getTankType() != Fluids.NONE) {
                te.tryProvide(te.output, level, pos.relative(direction.toDirection()), direction.toDirection());
            }
        }
        te.processCanister(te.output, 5, 6);
        te.syncState();
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
     * slot 1 — the 1.12.2 {@code tanksNew[0].setType(0, 1, inventory)}. Retyping resets the output tank so
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
     * Processes one canister bay {@code (in → out)} against {@code tank}: a <b>full</b> container drains
     * into the tank, an <b>empty</b> one is filled from it, and the modified single container moves to
     * {@code out}. Covers the universal tank/barrel ({@link IFillableItem}) and buckets (HBM
     * {@link ItemFluidBucket} + vanilla). The old machine had a directional {@code loadTank(2,3)} /
     * {@code unloadTank(5,6)}; like the ported fluid tank, both bays here can fill <i>or</i> drain, which
     * only ever makes them more forgiving. Never runs when the result would not fit in {@code out}.
     */
    private void processCanister(FluidTankNTM tank, int inSlot, int outSlot) {
        ItemStack in = inventory.getStackInSlot(inSlot);
        if (in.isEmpty()) return;
        FluidType tankType = tank.getTankType();

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);

            if (held > 0 && heldType != null && heldType != Fluids.NONE && tankType == heldType) {
                int addable = tank.fill(heldType, held, false);
                if (addable > 0) {
                    ItemStack work = single(in);
                    int taken = fillable.tryEmpty(heldType, addable, work);
                    if (taken > 0 && fits(outSlot, work)) {
                        tank.fill(heldType, taken, true);
                        moveToOut(inSlot, outSlot, work);
                        return;
                    }
                }
            }
            if (tankType != Fluids.NONE && tank.getFill() > 0 && fillable.acceptsFluid(tankType, in)) {
                ItemStack work = single(in);
                int remainder = fillable.tryFill(tankType, tank.getFill(), work);
                int moved = tank.getFill() - remainder;
                if (moved > 0 && fits(outSlot, work)) {
                    tank.setFill(tank.getFill() - moved);
                    moveToOut(inSlot, outSlot, work);
                }
            }
            return;
        }

        FluidType bucketFluid = bucketFluid(in);
        if (bucketFluid != Fluids.NONE && tankType == bucketFluid
                && tank.fill(bucketFluid, ItemFluidBucket.VOLUME, false) >= ItemFluidBucket.VOLUME) {
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (fits(outSlot, empty)) {
                tank.fill(bucketFluid, ItemFluidBucket.VOLUME, true);
                moveToOut(inSlot, outSlot, empty);
            }
        } else if (in.is(Items.BUCKET) && tankType != Fluids.NONE && tank.getFill() >= ItemFluidBucket.VOLUME) {
            ItemStack filled = tankType == Fluids.WATER ? new ItemStack(Items.WATER_BUCKET)
                    : tankType == Fluids.LAVA ? new ItemStack(Items.LAVA_BUCKET)
                    : ItemFluidBucket.make(tankType);
            if (fits(outSlot, filled)) {
                tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
                moveToOut(inSlot, outSlot, filled);
            }
        }
    }

    /** The fluid a full bucket in {@code stack} would give a tank (NONE if it isn't a full bucket). */
    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private boolean fits(int outSlot, ItemStack stack) {
        return inventory.insertItem(outSlot, stack.copy(), true).isEmpty();
    }

    private void moveToOut(int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
        setChanged();
    }

    /** Any fluid container the canister bays accept: a tank/barrel, an HBM fluid bucket, or a vanilla bucket. */
    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof IFillableItem
                || stack.getItem() instanceof ItemFluidBucket
                || stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET);
    }

    /**
     * Slot roles: 0/1 identifier in/out, 2/5 canister bays, 4 battery. Slots 3 and 6 are the bays' result
     * outputs and must return {@code true}: the machine places the emptied/filled container there through
     * {@link dev.golem.core.machine.ItemHandler#insertItem}, which validates against this method — player
     * insertion is blocked by the take-only slots in {@link ContainerMachineTurbine} instead.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidId;
            case 2, 5 -> isFluidContainer(stack);
            case 4 -> Library.isBattery(stack);
            default -> true; // 3 + 6: bay outputs — the machine writes them; the menu keeps players out
        };
    }

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
