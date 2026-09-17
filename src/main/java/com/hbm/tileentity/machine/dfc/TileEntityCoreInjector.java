package com.hbm.tileentity.machine.dfc;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.dfc.CoreComponent;
import com.hbm.inventory.container.ContainerCoreInjector;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Feeds the core its two fuels down the same line the beams travel. The two identifier slots per tank
 * choose what it carries, and a core reached along the facing is topped up from whichever tank matches
 * it — or retyped to match, if the core's own tank is empty.
 */
public class TileEntityCoreInjector extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardReceiverMK2 {

    public static final int range = 15;

    public FluidTankNTM[] tanks;
    public int beam;

    public TileEntityCoreInjector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DFC_INJECTOR.get(), pos, state, 4);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.DEUTERIUM, 128_000);
        tanks[1] = new FluidTankNTM(Fluids.TRITIUM, 128_000);
    }

    @Override
    public String getDefaultName() {
        return "container.dfcInjector";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCoreInjector injector) {
        injector.tickServer(level);
    }

    protected void tickServer(Level level) {
        BlockPos pos = getBlockPos();

        setTypeFromFluidId(0, 0);
        setTypeFromFluidId(1, 2);

        beam = 0;
        ForgeDirection dir = facing();

        for (int i = 1; i <= range; i++) {
            BlockPos target = pos.offset(dir.offsetX * i, dir.offsetY * i, dir.offsetZ * i);
            BlockEntity be = level.getBlockEntity(target);

            if (be instanceof TileEntityCore core) {
                for (int t = 0; t < 2; t++) fill(core, t);
                beam = i;
                break;
            }

            if (!level.getBlockState(target).isAir()) break;
        }

        setChanged();
        networkPackNT();
    }

    private void fill(TileEntityCore core, int t) {
        if (core.tanks[t].getTankType() != tanks[t].getTankType()) {
            if (core.tanks[t].getFill() != 0) return;
            core.tanks[t].setTankType(tanks[t].getTankType());
        }
        int moved = Math.min(tanks[t].getFill(), core.tanks[t].getMaxFill() - core.tanks[t].getFill());
        tanks[t].setFill(tanks[t].getFill() - moved);
        core.tanks[t].setFill(core.tanks[t].getFill() + moved);
        core.setChanged();
    }

    /**
     * The identifier slot pair for a tank — upstream's {@code tanks[t].setType(in, out, inventory)}: the
     * identifier retypes the tank and then passes from the in slot to the out slot, so the out slot has to
     * be free before the swap can happen.
     */
    private void setTypeFromFluidId(int tank, int in) {
        ItemStack id = inventory.getStackInSlot(in);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        if (!inventory.getStackInSlot(in + 1).isEmpty()) return;

        FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type == tanks[tank].getTankType()) return;

        tanks[tank].setTankType(type);
        inventory.insertItem(in + 1, id.copy(), false);
        inventory.extractItem(in, 1, false);
        setChanged();
    }

    public ForgeDirection facing() {
        return ForgeDirection.getOrientation(getBlockState().getValue(CoreComponent.FACING).ordinal());
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return (slot == 0 || slot == 2) && stack.getItem() instanceof IItemFluidId;
    }

    @Override public boolean canConnect(FluidType type, ForgeDirection dir) {
        return type == tanks[0].getTankType() || type == tanks[1].getTankType();
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return tanks; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(beam);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.beam = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "fuel1");
        tanks[1].writeToNBT(output, "fuel2");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "fuel1");
        tanks[1].readFromNBT(input, "fuel2");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerCoreInjector(id, playerInventory, this);
    }
}
