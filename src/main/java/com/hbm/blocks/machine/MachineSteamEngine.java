package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineSteamEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The legacy one-core steam engine with three ports in the upper row. */
public class MachineSteamEngine extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineSteamEngine(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineSteamEngine(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 5, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        addExtraPorts(level, x, y, z, dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addExtraPorts(level, core.getX(), core.getY(), core.getZ(), dir);
    }

    private void addExtraPorts(Level level, int x, int y, int z, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, x + rot.offsetX, y + 1, z + rot.offsetZ);
        makeExtra(level, x + rot.offsetX + dir.offsetX, y + 1, z + rot.offsetZ + dir.offsetZ);
        makeExtra(level, x + rot.offsetX - dir.offsetX, y + 1, z + rot.offsetZ - dir.offsetZ);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineSteamEngine engine
                && IEnergyConnectorBlock.isPort(engine.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return (type == Fluids.STEAM || type == Fluids.SPENTSTEAM)
                && findCoreTE(world, pos) instanceof TileEntityMachineSteamEngine engine
                && IEnergyConnectorBlock.isPort(engine.getConPos(), pos, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineSteamEngine engine)) return null;

        List<String> text = new ArrayList<>();
        text.add("§a-> §r" + tankLine(engine.tanks[0]));
        text.add("§c<- §r" + tankLine(engine.tanks[1]));
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    private static String tankLine(com.hbm.inventory.fluid.tank.FluidTankNTM tank) {
        return tank.getTankType().getDisplayNameString() + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB";
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_STEAM_ENGINE.get(),
                        TileEntityMachineSteamEngine::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_STEAM_ENGINE.get(),
                        TileEntityMachineSteamEngine::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
