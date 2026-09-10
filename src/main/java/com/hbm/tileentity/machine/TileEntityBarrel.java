package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.BlockFluidBarrel;
import com.hbm.inventory.container.ContainerBarrel;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.CanisterBays;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.HashSet;
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
import org.jspecify.annotations.Nullable;

/**
 * A one-block fluid drum, ported from 1.12.2's {@code TileEntityBarrel}.
 *
 * <p>Mechanically it is {@link TileEntityMachineFluidTank} at drum scale and with drum capacities: the
 * same four modes, the same buffer-mode node, the same two interchangeable canister bays. What differs
 * is that the capacity comes from the block — six drums share one block entity type — and the six
 * connection points are the drum's own faces rather than a 3×3 footprint's edges.
 *
 * <p><b>Deviations from the fork.</b> Two, both following decisions this port already made elsewhere.
 * The Forge fluid-capability wrappers are dropped along with {@code IFFtoNTMF}, which nothing here
 * carries. And the fluid-interaction hazards — a drum melting, burning or annihilating from what is in
 * it — are dropped exactly as {@link TileEntityMachineFluidTank} drops the tank's.
 */
public class TileEntityBarrel extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive {

    public static final short MODES = 4;

    /** 0 = receive-only, 1 = both (acts like a pipe), 2 = send-only, 3 = disabled. */
    public short mode = 0;
    public final FluidTankNTM tank;

    protected FluidNode node;
    protected FluidType lastType;

    public TileEntityBarrel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL.get(), pos, state, 6);
        this.tank = new FluidTankNTM(Fluids.NONE, capacityOf(state));
    }

    private static int capacityOf(BlockState state) {
        return state.getBlock() instanceof BlockFluidBarrel barrel ? barrel.getCapacity() : 0;
    }

    @Override
    public String getDefaultName() {
        return "container.barrel";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBarrel barrel) {
        barrel.updateTypeFromFluidId();
        barrel.bay(2, 3);
        barrel.bay(4, 5);
        FluidType type = barrel.tank.getTankType();

        if (barrel.mode == 1) {
            if (barrel.node == null || barrel.node.expired || type != barrel.lastType) {
                barrel.node = (FluidNode) UniNodespace.getNode(level, pos, type.getNetworkProvider());
                if (barrel.node == null || barrel.node.expired || type != barrel.lastType) {
                    barrel.node = barrel.createNode(type);
                    UniNodespace.createNode(level, barrel.node);
                    barrel.lastType = type;
                }
            }
            if (barrel.node != null && barrel.node.hasValidNet()) {
                barrel.node.net.addProvider(barrel);
                barrel.node.net.addReceiver(barrel);
            }
        } else {
            if (barrel.node != null) {
                UniNodespace.destroyNode(level, pos, type.getNetworkProvider());
                barrel.node = null;
            }
            for (DirPos dp : barrel.getConPos()) {
                FluidNode dirNode = (FluidNode) UniNodespace.getNode(level, dp.getPos(), type.getNetworkProvider());
                if (barrel.mode == 2) {
                    barrel.tryProvide(barrel.tank, level, dp.getPos(), dp.getDir());
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeProvider(barrel);
                }
                if (barrel.mode == 0) {
                    if (dirNode != null && dirNode.hasValidNet()) dirNode.net.addReceiver(barrel);
                } else if (dirNode != null && dirNode.hasValidNet()) {
                    dirNode.net.removeReceiver(barrel);
                }
            }
        }

        barrel.setChanged();
        barrel.networkPackNT();
    }

    private void bay(int inSlot, int outSlot) {
        CanisterBays.process(inventory, tank, inSlot, outSlot, before -> {
            if (before != tank.getTankType() && node != null) {
                UniNodespace.destroyNode(level, getBlockPos(), before.getNetworkProvider());
                node = null;
            }
        });
    }

    public void cycleMode() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    private void updateTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        if (!inventory.getStackInSlot(1).isEmpty()) return;

        FluidType type = ident.getType(level, getBlockPos().getX(), getBlockPos().getY(),
                getBlockPos().getZ(), id);
        if (type == tank.getTankType()) return;

        tank.setTankType(type);
        if (node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), tank.getTankType().getNetworkProvider());
            node = null;
        }
        inventory.insertItem(1, id.copy(), false);
        inventory.extractItem(0, 1, false);
        setChanged();
    }

    /** The drum's six faces; unlike the tank there is no footprint, so each side is one point. */
    public DirPos[] getConPos() {
        int x = getBlockPos().getX(), y = getBlockPos().getY(), z = getBlockPos().getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Library.POS_X), new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z), new DirPos(x, y, z - 1, Library.NEG_Z),
                new DirPos(x, y + 1, z, Library.POS_Y), new DirPos(x, y - 1, z, Library.NEG_Y)};
    }

    protected FluidNode createNode(FluidType type) {
        DirPos[] conPos = getConPos();
        HashSet<BlockPos> posSet = new HashSet<>();
        posSet.add(getBlockPos());
        return new FluidNode(type.getNetworkProvider(), posSet.toArray(new BlockPos[0])).setConnections(conPos);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide() && node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), tank.getTankType().getNetworkProvider());
        }
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return (mode == 1 || mode == 2) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0];
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return (mode == 0 || mode == 1) ? new FluidTankNTM[]{tank} : new FluidTankNTM[0];
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putShort("mode", mode);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        mode = input.getShortOr("mode", (short) 0);
        tank.readFromNBT(input, "tank");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerBarrel(containerId, inventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "type", PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "type").equals(name)) return tank.getTankType().getName();
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "fillpercent").equals(name)) {
            return "" + (tank.getMaxFill() > 0 ? tank.getFill() * 100 / tank.getMaxFill() : 0);
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int requested = IRORInteractive.parseInt(params[0], 0, 3);
            if (requested != mode) {
                mode = (short) requested;
            } else if (params.length > 1) {
                mode = (short) IRORInteractive.parseInt(params[1], 0, 3);
            }
            markChanged();
        }
        return null;
    }
}
