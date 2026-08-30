package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityDeuteriumExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Single-block heavy-water extractor. */
public class MachineDeuteriumExtractor extends dev.golem.core.world.CompatBlock
        implements EntityBlock, IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineDeuteriumExtractor(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDeuteriumExtractor(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_DEUTERIUM_EXTRACTOR.get(),
                TileEntityDeuteriumExtractor::serverTick);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && world.getBlockEntity(pos) instanceof TileEntityDeuteriumExtractor;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && world.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityDeuteriumExtractor
                && (type == Fluids.WATER || type == Fluids.HEAVYWATER);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityDeuteriumExtractor extractor)) return null;

        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(extractor.power) + "/" + Library.getShortNumber(extractor.getMaxPower()) + " HE");
        text.add("§a-> §rWater: " + extractor.tanks[0].getFill() + "/" + extractor.tanks[0].getMaxFill() + "mB");
        text.add("§c<- §rHeavy water: " + extractor.tanks[1].getFill() + "/" + extractor.tanks[1].getMaxFill() + "mB");
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
