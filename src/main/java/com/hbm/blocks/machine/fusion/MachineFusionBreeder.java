package com.hbm.blocks.machine.fusion;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
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

import java.util.List;

/** The fusion breeder's 5 x 3 x 5 dummyable footprint. */
public class MachineFusionBreeder extends BlockDummyable implements IFluidConnectorBlockMK2, WorldlyContainerHolder, ILookOverlay {

    public MachineFusionBreeder(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionBreeder(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 2, 2, 1, 1}; }
    @Override public int getOffset() { return 2; }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        makeExtra(level, x + rot.offsetX, y, z + rot.offsetZ);
        makeExtra(level, x - rot.offsetX, y, z - rot.offsetZ);
        makeExtra(level, x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ);
        makeExtra(level, x + dir.offsetX - rot.offsetX, y, z + dir.offsetZ - rot.offsetZ);
        makeExtra(level, x + dir.offsetX * 2, y + 2, z + dir.offsetZ * 2);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(META) < extra) return null;
        return findCoreTE(level, pos) instanceof TileEntityFusionBreeder breeder ? breeder : null;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection direction) {
        BlockPos cell = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(cell);
        if (state.getBlock() != this || state.getValue(META) < extra || state.getValue(META) >= offset + 2) return false;

        BlockPos outside = cell.offset(direction.offsetX, direction.offsetY, direction.offsetZ);
        return findCoreTE(world, cell) instanceof TileEntityFusionBreeder breeder
                && breeder.getConPos().stream().anyMatch(port -> port.getPos().equals(outside) && port.getDir() == direction);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityFusionBreeder breeder)) return null;
        List<String> text = new java.util.ArrayList<>();
        text.add("§a-> §r" + String.format(java.util.Locale.US, "%,.0f", breeder.neutronEnergy) + " flux/t");
        for (int i = 0; i < breeder.tanks.length; i++) {
            text.add((i == 0 ? "§a-> §r" : "§c<- §r") + breeder.tanks[i].getTankType().getDisplayNameString()
                    + ": " + String.format(java.util.Locale.US, "%,d", breeder.tanks[i].getFill())
                    + "/" + String.format(java.util.Locale.US, "%,d", breeder.tanks[i].getMaxFill()) + " mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

}
