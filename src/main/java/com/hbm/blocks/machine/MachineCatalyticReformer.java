package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;
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

/** Catalytic Reformer block — 1:1 port of the 1.12.2 {@code MachineCatalyticReformer} (dims
 * {@code {2,0,1,1,2,2}}, offset 1; two large body fills + 6 makeExtra corner/side ports). */
public class MachineCatalyticReformer extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineCatalyticReformer(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCatalyticReformer(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 1, 2, 2}; }
    @Override public int getOffset() { return 1; }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return super.checkRequirement(world, x, y, z, dir, o) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{3, -3, 1, 0, -1, 2}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{6, -3, 1, 1, 2, 0}, x, y, z, dir);
    }

    @Override
    public void formStructure(Level world, BlockPos core, ForgeDirection dir) {
        super.formStructure(world, core, dir);

        int cx = core.getX(), cy = core.getY(), cz = core.getZ();
        MultiblockHandlerXR.fillSpace(world, cx, cy, cz, new int[]{3, -3, 1, 0, -1, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(world, cx, cy, cz, new int[]{6, -3, 1, 1, 2, 0}, this, dir);

        // Upstream wrote these against the CLICKED position; with offset 1 that is exactly the core.
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        this.makeExtra(world, cx + 1, cy, cz + 1);
        this.makeExtra(world, cx + 1, cy, cz - 1);
        this.makeExtra(world, cx - 1, cy, cz + 1);
        this.makeExtra(world, cx - 1, cy, cz - 1);
        this.makeExtra(world, cx + rot.offsetX * 2, cy, cz + rot.offsetZ * 2);
        this.makeExtra(world, cx - rot.offsetX * 2, cy, cz - rot.offsetZ * 2);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineCatalyticReformer te ? te : null;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineCatalyticReformer te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineCatalyticReformer te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

}
