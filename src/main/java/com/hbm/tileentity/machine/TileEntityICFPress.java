package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.container.ContainerICFPress;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.items.machine.ItemICFPellet.EnumICFFuel;
import com.hbm.tileentity.ModIcfBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
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

import java.util.List;

/**
 * The ICF fuel pellet maker's block entity (1.7 {@code TileEntityICFPress}). Slots: 0 empty pellet, 1 filled
 * pellet out, 2 muon in, 3 its empty particle capsule out, 4 and 5 solid fuels (left, right), 6 and 7 the fluid
 * identifiers of the left and right tanks.
 *
 * <p>Each tick with an empty pellet in and nothing in the output it fills one: each side's fuel is the tank's
 * fluid when 1000 mB of a pellet fluid are there, otherwise the side's solid slot when it holds exactly one ingot
 * of a pellet material. Both fuels must exist and differ. A loaded muon (sixteen presses per capsule) catalyses
 * the pellet.
 */
public class TileEntityICFPress extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardReceiverMK2, WorldlyContainer {

    public static final int SLOTS = 8;

    public final FluidTankNTM[] tanks;
    public int muon;
    public static final int maxMuon = 16;

    public TileEntityICFPress(BlockPos pos, BlockState state) {
        super(ModIcfBlockEntities.ICF_PRESS.get(), pos, state, SLOTS);
        this.tanks = new FluidTankNTM[2];
        this.tanks[0] = new FluidTankNTM(Fluids.DEUTERIUM, 16_000);
        this.tanks[1] = new FluidTankNTM(Fluids.TRITIUM, 16_000);
    }

    @Override
    public String getDefaultName() {
        return "container.machineICFPress";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityICFPress press) {
        press.setTypeFromIdentifier(0, 6);
        press.setTypeFromIdentifier(1, 7);

        // 1.7 subscribed both tanks to every neighbour once a second; the duct net finds the press itself.

        ItemStack muonIn = press.inventory.getStackInSlot(2);
        if (press.muon <= 0 && !muonIn.isEmpty() && muonIn.getItem() == ModItems.particle_muon.get()) {

            ItemStack container = Compat.craftingRemainder(muonIn);
            boolean canStore = false;
            ItemStack out = press.inventory.getStackInSlot(3);

            if (container.isEmpty()) {
                canStore = true;
            } else if (out.isEmpty()) {
                press.inventory.setStackInSlot(3, container.copy());
                canStore = true;
            } else if (out.getItem() == container.getItem() && out.getCount() < out.getMaxStackSize()) {
                ItemStack grown = out.copy();
                grown.grow(1);
                press.inventory.setStackInSlot(3, grown);
                canStore = true;
            }

            if (canStore) {
                press.muon = maxMuon;
                press.inventory.extractItem(2, 1, false);
                press.setChanged();
            }
        }

        press.press();

        press.networkPackNT();
    }

    public void press() {
        ItemStack empty = inventory.getStackInSlot(0);
        if (empty.isEmpty() || empty.getItem() != ModItems.icf_pellet_empty.get()) return;
        if (!inventory.getStackInSlot(1).isEmpty()) return;

        ItemICFPellet.init();

        EnumICFFuel fuel1 = getFuel(tanks[0], inventory.getStackInSlot(4), 0);
        EnumICFFuel fuel2 = getFuel(tanks[1], inventory.getStackInSlot(5), 1);

        if (fuel1 == null || fuel2 == null || fuel1 == fuel2) return;

        inventory.setStackInSlot(1, ItemICFPellet.setup(fuel1, fuel2, muon > 0));

        if (muon > 0) muon--;

        inventory.extractItem(0, 1, false);
        if (usedFluid[0]) tanks[0].setFill(tanks[0].getFill() - 1000); else inventory.extractItem(4, 1, false);
        if (usedFluid[1]) tanks[1].setFill(tanks[1].getFill() - 1000); else inventory.extractItem(5, 1, false);

        this.markChanged();
    }

    /** 1.7 kept these static, shared by every press; they are only read right after {@link #getFuel} sets them. */
    public static boolean[] usedFluid = new boolean[2];

    public @Nullable EnumICFFuel getFuel(FluidTankNTM tank, ItemStack slot, int index) {
        usedFluid[index] = false;
        if (tank.getFill() >= 1000 && ItemICFPellet.fluidMap.containsKey(tank.getTankType())) {
            usedFluid[index] = true;
            return ItemICFPellet.fluidMap.get(tank.getTankType());
        }
        if (slot.isEmpty()) return null;
        List<MaterialStack> mats = Mats.getMaterialsFromItem(slot);
        if (mats == null || mats.size() != 1) return null;
        MaterialStack mat = mats.get(0);
        if (mat.amount != MaterialShapes.INGOT.quantity) return null;
        return ItemICFPellet.materialMap.get(mat.material);
    }

    /** 1.7 {@code tank.setType(slot, slots)}: a new identifier retypes the tank and empties it. */
    private void setTypeFromIdentifier(int tank, int slot) {
        ItemStack id = inventory.getStackInSlot(slot);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type != tanks[tank].getTankType()) {
            tanks[tank].setTankType(type);
            setChanged();
        }
    }

    // --- sync ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte((byte) muon);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.muon = buf.readByte();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    // --- inventory ---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.getItem() == ModItems.icf_pellet_empty.get()) return slot == 0;
        if (stack.getItem() == ModItems.particle_muon.get()) return slot == 2;
        return slot == 4 || slot == 5;
    }

    public static final int[] topBottom = new int[]{0, 1, 2, 3, 4};
    public static final int[] sides = new int[]{0, 1, 2, 3, 5};

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 || side == 1 ? topBottom : sides;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN || side == Direction.UP ? topBottom : sides;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 || slot == 3;
    }

    // --- persistence ---

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "t0");
        tanks[1].readFromNBT(input, "t1");
        this.muon = input.getByteOr("muon", (byte) 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "t0");
        tanks[1].writeToNBT(output, "t1");
        output.putByte("muon", (byte) muon);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerICFPress(containerId, playerInventory, this);
    }

    // --- fluid ---

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
}
