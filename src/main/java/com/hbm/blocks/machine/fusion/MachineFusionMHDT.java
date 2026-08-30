package com.hbm.blocks.machine.fusion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Magnetohydrodynamic turbine - the fusion chain's electrical output, the longest machine in the set
 * at fourteen blocks along its axis. Behaviour lives in {@link TileEntityFusionMHDT}.
 */
public class MachineFusionMHDT extends BlockDummyable implements ILookOverlay, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineFusionMHDT(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionMHDT(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 6, 7, 2, 2}; }

    @Override public int getOffset() { return 7; }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        int cx = core.getX(), cy = core.getY(), cz = core.getZ();

        MultiblockHandlerXR.fillSpace(level, cx, cy, cz, new int[]{3, -2, 6, 2, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, cx, cy, cz, new int[]{3, -2, -6, 7, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, cx, cy, cz, new int[]{3, -2, -3, 5, 2, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, cx, cy, cz, new int[]{4, -3, -3, 5, 1, 1}, this, dir);
        // The intake cowl sits three blocks further down the axis than the rest.
        MultiblockHandlerXR.fillSpace(level, cx + dir.offsetX * 3, cy, cz + dir.offsetZ * 3,
                new int[]{1, 0, 0, 1, 3, 3}, this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, cx + dir.offsetX * 4 + rot.offsetX * 3, cy, cz + dir.offsetZ * 4 + rot.offsetZ * 3);
        makeExtra(level, cx + dir.offsetX * 4 - rot.offsetX * 3, cy, cz + dir.offsetZ * 4 - rot.offsetZ * 3);
        makeExtra(level, cx + dir.offsetX * 7, cy + 1, cz + dir.offsetZ * 7);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return isPortCell(world, pos, dir);
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
        return findCoreTE(world, cell) instanceof TileEntityFusionMHDT te
                && te.getConPos().stream().anyMatch(p -> p.getPos().equals(outside) && p.getDir() == dir);
    }

    /**
     * The look-at readout. Beyond the throughput it carries the two warnings that explain a turbine
     * producing nothing: plasma below the threshold (half output), and no coolant (no output at all).
     * Both blink, as upstream.
     */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityFusionMHDT turbine)) return null;

        boolean hasPlasma = turbine.hasMinimumPlasma();
        boolean isCool = turbine.isCool();
        long power = (long) Math.floor(turbine.plasmaEnergy * TileEntityFusionMHDT.PLASMA_EFFICIENCY);
        if (!hasPlasma) power /= 2;

        java.util.List<String> text = new java.util.ArrayList<>();
        text.add("§a-> " + (hasPlasma ? "§r" : "§6")
                + String.format(java.util.Locale.US, "%,d", turbine.plasmaEnergy) + " TU/t / "
                + String.format(java.util.Locale.US, "%,d", TileEntityFusionMHDT.MINIMUM_PLASMA) + " TU/t");
        text.add("§c<- §r" + String.format(java.util.Locale.US, "%,d", isCool ? power : 0L) + " HE/t");

        com.hbm.inventory.fluid.tank.FluidTankNTM[] tanks = turbine.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            text.add((i == 0 ? "§a-> §r" : "§c<- §r") + tanks[i].getTankType().getDisplayNameString()
                    + ": " + String.format(java.util.Locale.US, "%,d", tanks[i].getFill())
                    + "/" + String.format(java.util.Locale.US, "%,d", tanks[i].getMaxFill()) + " mB");
        }

        boolean blink = (System.currentTimeMillis() / 500L) % 2L == 0L;
        if (turbine.plasmaEnergy > 0 && !hasPlasma) {
            text.add("&[" + (blink ? 0xff8000 : 0xffff00) + "&]! LOW POWER !");
        }
        if (!isCool) {
            text.add("&[" + (blink ? 0xff0000 : 0xffff00) + "&]! ! ! INSUFFICIENT COOLING ! ! !");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.FUSION_MHDT.get(), TileEntityFusionMHDT::clientTick)
                : createTickerHelper(type, ModBlockEntities.FUSION_MHDT.get(), TileEntityFusionMHDT::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
