package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower;
import com.hbm.tileentity.machine.oil.TileEntityOilDrillBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fracking Tower block — 1:1 port of the 1.12.2 {@code MachineFrackingTower}: a tall derrick built on a
 * 1×1 core column (dims {@code {3,0,0,0,0,0}}, offset 0), plus a y+2 platform, four corner legs, two
 * downward drill shafts and a y+15 crown (the old custom checkRequirement/fillSpace, copied verbatim).
 */
public class MachineFrackingTower extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineFrackingTower(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineFrackingTower(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        if (!MultiblockHandlerXR.checkSpace(world, x, y + 2, z, new int[]{1, 0, 3, 3, 3, 3}, x, y, z, dir)) return false;

        if (!MultiblockHandlerXR.checkSpace(world, x - 2, y + 2, z - 2, new int[]{-1, 2, 0, 1, 0, 1}, x, y, z, ForgeDirection.NORTH)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, x - 2, y + 2, z + 3, new int[]{-1, 2, 0, 1, 0, 1}, x, y, z, ForgeDirection.NORTH)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, x + 3, y + 2, z - 2, new int[]{-1, 2, 0, 1, 0, 1}, x, y, z, ForgeDirection.NORTH)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, x + 3, y + 2, z + 3, new int[]{-1, 2, 0, 1, 0, 1}, x, y, z, ForgeDirection.NORTH)) return false;

        if (!MultiblockHandlerXR.checkSpace(world, x, y, z, new int[]{10, -4, 2, 2, 2, 2}, x, y, z, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, x, y, z, new int[]{24, -9, 1, 1, 1, 1}, x, y, z, dir)) return false;

        if (!MultiblockHandlerXR.checkSpace(world, x, y + 15, z, new int[]{1, 0, 1, 1, -2, 3}, x, y, z, dir)) return false;

        return super.checkRequirement(world, x, y, z, dir, o);
    }

    @Override
    public void formStructure(Level world, BlockPos core, ForgeDirection dir) {
        int x = core.getX(), y = core.getY(), z = core.getZ();

        MultiblockHandlerXR.fillSpace(world, x, y, z, getDimensions(), this, dir);
        MultiblockHandlerXR.fillSpace(world, x, y + 2, z, new int[]{1, 0, 3, 3, 3, 3}, this, dir);

        MultiblockHandlerXR.fillSpace(world, x - 2, y + 2, z - 2, new int[]{-1, 2, 0, 1, 0, 1}, this, ForgeDirection.NORTH);
        MultiblockHandlerXR.fillSpace(world, x - 2, y + 2, z + 3, new int[]{-1, 2, 0, 1, 0, 1}, this, ForgeDirection.NORTH);
        MultiblockHandlerXR.fillSpace(world, x + 3, y + 2, z - 2, new int[]{-1, 2, 0, 1, 0, 1}, this, ForgeDirection.NORTH);
        MultiblockHandlerXR.fillSpace(world, x + 3, y + 2, z + 3, new int[]{-1, 2, 0, 1, 0, 1}, this, ForgeDirection.NORTH);

        MultiblockHandlerXR.fillSpace(world, x, y, z, new int[]{10, -4, 2, 2, 2, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x, y, z, new int[]{24, -9, 1, 1, 1, 1}, this, dir);

        MultiblockHandlerXR.fillSpace(world, x, y + 15, z, new int[]{1, 0, 1, 1, -2, 3}, this, ForgeDirection.WEST);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineFrackingTower te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineFrackingTower te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

}
