package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Coker block — 1:1 port of the 1.12.2 {@code MachineCoker}: a 3×3×23 heat tower (dims
 * {@code {22,0,1,1,1,1}}, offset 1; a y+1 ring, 4 corner pillars, 4 makeExtra fluid ports). */
public class MachineCoker extends BlockDummyable implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineCoker(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCoker(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{22, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        if (!super.checkRequirement(world, x, y, z, dir, o)) return false;
        x += dir.offsetX * o;
        z += dir.offsetZ * o;
        return MultiblockHandlerXR.checkSpace(world, x, y + 1, z, new int[]{5, 0, 2, 2, 2, 2}, x, y, z, ForgeDirection.NORTH) &&
                MultiblockHandlerXR.checkSpace(world, x + 2, y + 1, z + 2, new int[]{0, 1, 0, 0, 0, 0}, x, y, z, ForgeDirection.NORTH) &&
                MultiblockHandlerXR.checkSpace(world, x + 2, y + 1, z - 2, new int[]{0, 1, 0, 0, 0, 0}, x, y, z, ForgeDirection.NORTH) &&
                MultiblockHandlerXR.checkSpace(world, x - 2, y + 1, z + 2, new int[]{0, 1, 0, 0, 0, 0}, x, y, z, ForgeDirection.NORTH) &&
                MultiblockHandlerXR.checkSpace(world, x - 2, y + 1, z - 2, new int[]{0, 1, 0, 0, 0, 0}, x, y, z, ForgeDirection.NORTH);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        x += dir.offsetX * o;
        z += dir.offsetZ * o;

        MultiblockHandlerXR.fillSpace(world, x, y + 1, z, new int[]{5, 0, 2, 2, 2, 2}, this, ForgeDirection.NORTH);
        MultiblockHandlerXR.fillSpace(world, x + 2, y + 1, z + 2, new int[]{0, 1, 0, 0, 0, 0}, this, ForgeDirection.NORTH);
        MultiblockHandlerXR.fillSpace(world, x + 2, y + 1, z - 2, new int[]{0, 1, 0, 0, 0, 0}, this, ForgeDirection.NORTH);
        MultiblockHandlerXR.fillSpace(world, x - 2, y + 1, z + 2, new int[]{0, 1, 0, 0, 0, 0}, this, ForgeDirection.NORTH);
        MultiblockHandlerXR.fillSpace(world, x - 2, y + 1, z - 2, new int[]{0, 1, 0, 0, 0, 0}, this, ForgeDirection.NORTH);

        this.makeExtra(world, x + 1, y, z + 1);
        this.makeExtra(world, x + 1, y, z - 1);
        this.makeExtra(world, x - 1, y, z + 1);
        this.makeExtra(world, x - 1, y, z - 1);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineCoker te ? te : null;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineCoker te
                && com.hbm.api.energymk2.IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

}
