package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityDeuteriumTower;
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

/** Ten-block deuterium tower with an irregular 3x3 base and eight network ports. */
public class MachineDeuteriumTower extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineDeuteriumTower(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDeuteriumTower(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{9, 0, 1, 0, 0, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);

        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        ForgeDirection rotation = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, x - dir.offsetX - rotation.offsetX, y, z - dir.offsetZ - rotation.offsetZ);
        makeExtra(level, x, y, z - dir.offsetZ - rotation.offsetZ);
        makeExtra(level, x - dir.offsetX - rotation.offsetX, y, z);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityDeuteriumTower tower
                && IEnergyConnectorBlock.isPort(tower.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityDeuteriumTower tower
                && (type == Fluids.NONE || type == Fluids.WATER || type == Fluids.HEAVYWATER)
                && IEnergyConnectorBlock.isPort(tower.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityDeuteriumTower tower)) return null;

        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(tower.power) + "/" + Library.getShortNumber(tower.getMaxPower()) + " HE");
        text.add("§a-> §r" + tower.tanks[0].getTankType().getDisplayNameString() + ": "
                + tower.tanks[0].getFill() + "/" + tower.tanks[0].getMaxFill() + "mB");
        text.add("§c<- §r" + tower.tanks[1].getTankType().getDisplayNameString() + ": "
                + tower.tanks[1].getFill() + "/" + tower.tanks[1].getMaxFill() + "mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_DEUTERIUM_TOWER.get(),
                TileEntityDeuteriumTower::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
