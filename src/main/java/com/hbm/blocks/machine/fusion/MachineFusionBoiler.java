package com.hbm.blocks.machine.fusion;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fusion boiler - a plasma-fired drum that turns water straight into superheated steam. Behaviour
 * lives in {@link TileEntityFusionBoiler}.
 */
public class MachineFusionBoiler extends BlockDummyable implements ILookOverlay, IFluidConnectorBlockMK2 {

    public MachineFusionBoiler(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionBoiler(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 4, 4, 1, 1}; }

    @Override public int getOffset() { return 4; }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        int cx = core.getX(), cy = core.getY(), cz = core.getZ();

        // Four water/steam ports, paired either side of the drum at both ends.
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, cx - dir.offsetX + rot.offsetX, cy, cz - dir.offsetZ + rot.offsetZ);
        makeExtra(level, cx - dir.offsetX - rot.offsetX, cy, cz - dir.offsetZ - rot.offsetZ);
        makeExtra(level, cx + dir.offsetX * 2 + rot.offsetX, cy, cz + dir.offsetZ * 2 + rot.offsetZ);
        makeExtra(level, cx + dir.offsetX * 2 - rot.offsetX, cy, cz + dir.offsetZ * 2 - rot.offsetZ);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return isPortCell(world, new BlockPos(x, y, z), dir);
    }

    /** A pipe may attach only at a promoted port cell, and only on the face the machine declares. */
    private boolean isPortCell(BlockGetter world, BlockPos cell, ForgeDirection dir) {
        BlockState state = world.getBlockState(cell);
        if (state.getBlock() != this || state.getValue(META) < extra || state.getValue(META) >= offset + 2) return false;
        BlockPos outside = cell.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
        return findCoreTE(world, cell) instanceof TileEntityFusionBoiler te
                && te.getConPos().stream().anyMatch(p -> p.getPos().equals(outside) && p.getDir() == dir);
    }

    /**
     * The look-at readout: what the reactor is feeding this drum, and how full both tanks are. Without
     * it the boiler is completely opaque - it has no GUI of its own.
     */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityFusionBoiler boiler)) return null;

        java.util.List<String> text = new java.util.ArrayList<>();
        text.add("§a-> §r" + String.format(java.util.Locale.US, "%,d", boiler.plasmaEnergy) + " TU");
        com.hbm.inventory.fluid.tank.FluidTankNTM[] tanks = boiler.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            text.add((i == 0 ? "§a-> §r" : "§c<- §r") + tanks[i].getTankType().getDisplayNameString()
                    + ": " + String.format(java.util.Locale.US, "%,d", tanks[i].getFill())
                    + "/" + String.format(java.util.Locale.US, "%,d", tanks[i].getMaxFill()) + " mB");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.FUSION_BOILER.get(), TileEntityFusionBoiler::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
