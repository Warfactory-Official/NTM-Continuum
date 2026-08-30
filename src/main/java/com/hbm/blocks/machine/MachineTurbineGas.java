package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
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

/** The five-long legacy gas turbine multiblock. */
public class MachineTurbineGas extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineTurbineGas(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineTurbineGas(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 1, 4, 5}; }
    @Override public int getOffset() { return 1; }

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
        makeExtra(level, x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ);
        makeExtra(level, x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ);
        makeExtra(level, x - dir.offsetX - rot.offsetX * 4, y, z - dir.offsetZ - rot.offsetZ * 4);
        makeExtra(level, x + dir.offsetX - rot.offsetX * 4, y, z + dir.offsetZ - rot.offsetZ * 4);
        makeExtra(level, x + rot.offsetX * 4, y + 1, z + rot.offsetZ * 4);
        makeExtra(level, x - rot.offsetX * 5, y + 1, z - rot.offsetZ * 5);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineTurbineGas turbine
                && IEnergyConnectorBlock.isPort(new com.hbm.lib.DirPos[]{turbine.getPowerPos()}, pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(world, pos) instanceof TileEntityMachineTurbineGas turbine
                && turbine.isFluidPort(pos, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityMachineTurbineGas turbine)) return null;

        ForgeDirection facing = turbine.getFacing();
        List<String> text = new ArrayList<>();

        if (hitCheck(facing, turbine.getBlockPos(), -1, -1, 0, pos)
                || hitCheck(facing, turbine.getBlockPos(), 1, -1, 0, pos)) {
            text.add("§2-> §r" + turbine.tanks[0].getTankType().getDisplayNameString());
            text.add("§2-> §r" + turbine.tanks[1].getTankType().getDisplayNameString());
        }

        if (hitCheck(facing, turbine.getBlockPos(), -1, 4, 0, pos)
                || hitCheck(facing, turbine.getBlockPos(), 1, 4, 0, pos)) {
            text.add("§2-> §r" + turbine.tanks[2].getTankType().getDisplayNameString());
        }

        if (hitCheck(facing, turbine.getBlockPos(), 0, 5, 1, pos)) {
            text.add("§4-> §r" + turbine.tanks[3].getTankType().getDisplayNameString());
        }

        if (hitCheck(facing, turbine.getBlockPos(), 0, -4, 1, pos)) {
            text.add("§4-> §rPower");
        }

        if (!text.isEmpty()) {
            return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
        }
        return null;
    }

    private static boolean hitCheck(ForgeDirection facing, BlockPos core, int facingOffset, int rotationOffset,
                                    int yOffset, BlockPos hit) {
        ForgeDirection turn = facing.getRotation(ForgeDirection.DOWN);
        int x = core.getX() + facing.offsetX * facingOffset + turn.offsetX * rotationOffset;
        int y = core.getY() + yOffset;
        int z = core.getZ() + facing.offsetZ * facingOffset + turn.offsetZ * rotationOffset;
        return x == hit.getX() && y == hit.getY() && z == hit.getZ();
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_TURBINE_GAS.get(), TileEntityMachineTurbineGas::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_TURBINE_GAS.get(), TileEntityMachineTurbineGas::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
