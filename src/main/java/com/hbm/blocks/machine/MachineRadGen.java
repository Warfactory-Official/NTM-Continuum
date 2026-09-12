package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The 12-channel radiation generator and its three extra dummy cells. */
public class MachineRadGen extends BlockDummyable implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public MachineRadGen(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRadGen(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 3, 2, 1, 1};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, x - dir.offsetX * 5, y, z - dir.offsetZ * 5);
        makeExtra(level, x + dir.offsetX * offset + rot.offsetX, y,
                z + dir.offsetZ * offset + rot.offsetZ);
        makeExtra(level, x + dir.offsetX * offset - rot.offsetX, y,
                z + dir.offsetZ * offset - rot.offsetZ);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(), getDimensions(), this, dir);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, core.getX() - dir.offsetX * 3, core.getY(), core.getZ() - dir.offsetZ * 3);
        makeExtra(level, core.getX() + rot.offsetX, core.getY(), core.getZ() + rot.offsetZ);
        makeExtra(level, core.getX() - rot.offsetX, core.getY(), core.getZ() - rot.offsetZ);
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(level, pos) instanceof TileEntityMachineRadGen generator
                && IEnergyConnectorBlock.isPort(generator.getConPos(), pos, dir);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineRadGen generator
                ? generator : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

}
