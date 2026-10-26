package com.hbm.blocks.generic;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.Direction;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.NonNull;

import java.util.List;
import com.hbm.tileentity.ModTerrainBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/**
 * 1.7 {@code BlockFissure} ({@code ore_volcano}, "Geothermal Vent"): unbreakable bedrock with a molten
 * overlay that keeps the block above it flooded with volcanic lava on its random tick (radioactive lava
 * when {@code crater} is set, the non-zero meta placed by the crater feature) and pushes 1,000 mB of
 * {@code LAVA} per tick into whatever fluid receiver sits on top of it. The Hephaestus counts it as a
 * 300-heat source by registry id.
 */
public class BlockFissure extends CompatBlock implements TickingBlock, ICapabilityBlock {

    public static final BooleanProperty CRATER = BooleanProperty.create("crater");

    public BlockFissure(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(CRATER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRATER);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(); // unbreakable anyway
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).canBeReplaced()) {
            Block lava = state.getValue(CRATER) ? ModFluidNetBlocks.rad_lava_block.get() : ModFluidNetBlocks.volcanic_lava_block.get();
            level.setBlockAndUpdate(above, lava.defaultBlockState());
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFissure(pos, state);
    }

    /** 1.7 {@code TileEntityFissure}: a permanently full 1,000 mB lava tank. */
    public static class TileEntityFissure extends TileEntityLoadedBase implements IFluidStandardSenderMK2 {

        public final FluidTankNTM lava = new FluidTankNTM(Fluids.LAVA, 1_000);

        public TileEntityFissure(BlockPos pos, BlockState state) {
            super(ModTerrainBlockEntities.FISSURE.get(), pos, state);
        }

        protected PortDef[] cachedPorts;
        public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFissure fissure) {
            fissure.setupFluidPorts(fissure.getPorts());
            fissure.updatePortFIFO();
            fissure.lava.setFill(1_000);
        }

        @Override public @NonNull FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{lava}; }
        @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{lava}; }
    }

    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().fluidOut()
            .fluidFaces(BlockEntity.class, (be, side) -> side == Direction.DOWN);

    @Override
    public MachineCaps caps() { return CAPS; }
}
