package com.hbm.tileentity.machine;

import com.hbm.tileentity.IRelocatable;
import com.hbm.uninos.graph.PortClaims;
import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.golem.core.Ids;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.tileentity.ModPwrBlockEntities;

/**
 * The block entity behind every assembled-reactor cell (1.7 {@code BlockPWR.TileEntityBlockPWR}). It remembers
 * the block the cell was assembled from and the controller it belongs to, restores that block when the cell
 * is broken, and on a port cell forwards fluid, item and Redstone-over-Radio access to the controller.
 *
 * <p>The link survives save/load: the original block's registry id and the controller position are written
 * with the entity. The 1.7 entity was a plain {@code TileEntity}; here it sits on
 * {@link TileEntityLoadedBase} so the fluid net's loaded check and the chunk-load tag sync apply.
 */
public class TileEntityBlockPWR extends TileEntityLoadedBase
        implements IFluidReceiverMK2, IFluidProviderMK2, IRORValueProvider, IRORInteractive, IRelocatable,
        PortClaims.Delegate {

    /** 1.7: the controller subscribes and provides at the port positions; the port block only fronts it. */
    @Override
    public @org.jspecify.annotations.Nullable Object portClaimOwner() {
        return portCore();
    }

    /** A sub-level move carried this block; the controller follows when it was carried too. */
    @Override
    public void relocate(SubLevels.Move move) {
        BlockPos core = IRelocatable.follow(move, new BlockPos(coreX, coreY, coreZ));
        coreX = core.getX(); coreY = core.getY(); coreZ = core.getZ();
        cachedCore = null;
        setChanged();
    }


    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];

    public @Nullable Block block;
    public int coreX;
    public int coreY;
    public int coreZ;

    private @Nullable TileEntityPWRController cachedCore;

    public TileEntityBlockPWR(BlockPos pos, BlockState state) {
        super(ModPwrBlockEntities.PWR_BLOCK.get(), pos, state);
    }

    /** 1.7 {@code updateEntity}: once a second, a cell whose controller is gone or disassembled reverts. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBlockPWR cell) {
        if (level.getGameTime() % 20 != 0 || cell.block == null) return;

        TileEntityPWRController controller = cell.getCore();
        if (controller != null) {
            if (!controller.assembled) BlockPWR.revert(level, pos);
        } else if (level.hasChunk(cell.coreX >> 4, cell.coreZ >> 4)) {
            BlockPWR.revert(level, pos);
        }
    }

    public boolean isPort() {
        return getBlockState().getBlock() instanceof BlockPWR && getBlockState().getValue(BlockPWR.PORT);
    }

    /** 1.7 {@code getCore}: the controller, cached while it stays valid, and only looked up in loaded chunks. */
    public @Nullable TileEntityPWRController getCore() {
        if (cachedCore != null && !cachedCore.isRemoved()) return cachedCore;
        if (level == null) return null;

        if (level.hasChunk(coreX >> 4, coreZ >> 4)) {
            BlockEntity tile = level.getBlockEntity(new BlockPos(coreX, coreY, coreZ));
            if (tile instanceof TileEntityPWRController controller) {
                cachedCore = controller;
                return controller;
            }
        }
        return null;
    }

    private @Nullable TileEntityPWRController portCore() {
        if (!isPort() || block == null) return null;
        return getCore();
    }

    /**
     * 1.7 {@code BlockPWR.breakBlock}: breaking a cell puts the block it was assembled from back and flags the
     * controller as disassembled. This is the block entity's removal hook because it is the one place across
     * targets where the entity is still in hand; the section already holds the new state here, so setting the
     * original block makes the outer removal return "unchanged", which is exactly the 1.7 re-set trick.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || block == null) return;
        Block original = block;
        block = null; // never restore twice
        level.setBlock(pos, original.defaultBlockState(), 3);
        BlockEntity controller = level.getBlockEntity(new BlockPos(coreX, coreY, coreZ));
        if (controller instanceof TileEntityPWRController pwr) pwr.assembled = false;
    }

    // Persistence

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (block != null) {
            output.putString("block", BuiltInRegistries.BLOCK.getKey(block).toString());
            output.putInt("cX", coreX);
            output.putInt("cY", coreY);
            output.putInt("cZ", coreZ);
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        String id = input.getStringOr("block", "");
        block = null;
        if (!id.isEmpty()) {
            Block found;
            try {
                found = BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElse(null);
            } catch (RuntimeException malformed) {
                found = null;
            }
            if (found != null && found != Blocks.AIR) {
                block = found;
                coreX = input.getIntOr("cX", 0);
                coreY = input.getIntOr("cY", 0);
                coreZ = input.getIntOr("cZ", 0);
            }
        }
    }

    // Fluid: only a port cell talks to the net, and it answers with the controller's tanks

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return isPort();
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        TileEntityPWRController controller = portCore();
        return controller != null ? controller.getAllTanks() : NO_TANKS;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        TileEntityPWRController controller = portCore();
        return controller != null ? controller.transferFluid(type, pressure, amount) : amount;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        TileEntityPWRController controller = portCore();
        return controller != null ? controller.getDemand(type, pressure) : 0;
    }

    @Override
    public int[] getReceivingPressureRange(FluidType type) {
        TileEntityPWRController controller = portCore();
        return controller != null ? controller.getReceivingPressureRange(type) : DEFAULT_PRESSURE_RANGE;
    }

    @Override
    public void useUpFluid(FluidType type, int pressure, long amount) {
        TileEntityPWRController controller = portCore();
        if (controller != null) controller.useUpFluid(type, pressure, amount);
    }

    @Override
    public long getFluidAvailable(FluidType type, int pressure) {
        TileEntityPWRController controller = portCore();
        return controller != null ? controller.getFluidAvailable(type, pressure) : 0;
    }

    @Override
    public int[] getProvidingPressureRange(FluidType type) {
        TileEntityPWRController controller = portCore();
        return controller != null ? controller.getProvidingPressureRange(type) : DEFAULT_PRESSURE_RANGE;
    }

    // Redstone over Radio: a port cell relays the controller's values and functions

    @Override
    public String[] getFunctionInfo() {
        return TileEntityPWRController.ROR;
    }

    @Override
    public String provideRORValue(String name) {
        if (!isPort()) return "";
        TileEntityPWRController controller = getCore();
        return controller != null ? controller.provideRORValue(name) : null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if (!isPort()) return "";
        TileEntityPWRController controller = getCore();
        return controller != null ? controller.runRORFunction(name, params) : null;
    }
}
