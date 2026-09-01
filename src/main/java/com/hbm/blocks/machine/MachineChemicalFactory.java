package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.chemicalfactory.ChemicalFactoryRegistrations;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
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

/** The legacy 5x3 chemical factory: four chemical-plant lanes around one shared controller. */
public class MachineChemicalFactory extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder, ILookOverlay {

    public MachineChemicalFactory(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineChemicalFactory(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    /** Adds the old outer wall and the two upper proxy strips. */
    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);

        x -= dir.offsetX * 2;
        z -= dir.offsetZ * 2;

        addFactoryExtras(level, new BlockPos(x, y, z), dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addFactoryExtras(level, core, dir);
    }

    private void addFactoryExtras(Level level, BlockPos core, ForgeDirection dir) {
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            if (Math.abs(i) == 2 || Math.abs(j) == 2) makeExtra(level,
                    core.getX() + i, core.getY(), core.getZ() + j);
        }

        ForgeDirection rotation = dir.getRotation(ForgeDirection.UP);
        for (int i = -2; i <= 2; i++) {
            makeExtra(level, core.getX() + dir.offsetX * i + rotation.offsetX * 2, core.getY() + 2,
                    core.getZ() + dir.offsetZ * i + rotation.offsetZ * 2);
            makeExtra(level, core.getX() + dir.offsetX * i - rotation.offsetX * 2, core.getY() + 2,
                    core.getZ() + dir.offsetZ * i - rotation.offsetZ * 2);
        }
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        // The old proxy tiles exposed automation only from extra cells; ordinary footprint dummies did
        // not carry an inventory capability. The core itself remains directly accessible.
        if (state.getValue(META) < extra) return null;
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineChemicalFactory factory
                ? factory : null;
    }

    /** Both the ordinary network ring and the four coolant ports are real connection cells. */
    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(level, pos) instanceof TileEntityMachineChemicalFactory factory
                && isFactoryPort(factory, pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter level, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(level, pos) instanceof TileEntityMachineChemicalFactory factory
                && isFactoryPort(factory, pos, dir);
    }

    private static boolean isFactoryPort(TileEntityMachineChemicalFactory factory, BlockPos cell,
                                         ForgeDirection dir) {
        return IEnergyConnectorBlock.isPort(factory.getConPos(), cell, dir)
                || IEnergyConnectorBlock.isPort(factory.getCoolPos(), cell, dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ChemicalFactoryRegistrations.MACHINE_CHEMICAL_FACTORY_BE.get(),
                TileEntityMachineChemicalFactory::clientTick)
                : createTickerHelper(type, ChemicalFactoryRegistrations.MACHINE_CHEMICAL_FACTORY_BE.get(),
                TileEntityMachineChemicalFactory::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityMachineChemicalFactory factory)) return null;
        for (DirPos port : factory.getCoolPos()) {
            if (port.getPos().equals(pos.relative(port.getDir().toDirection()))) {
                return new ILookOverlay.Data(getName().getString(), 0xFFFF00, 0x404000, List.of(
                        "§a-> §r" + factory.water.getTankType().getDisplayNameString(),
                        "§c<- §r" + factory.lps.getTankType().getDisplayNameString()));
            }
        }
        DirPos[] io = factory.getIOPos();
        for (int i = 0; i < io.length; i++) {
            DirPos port = io[i];
            if (port.getPos().equals(pos.relative(port.getDir().toDirection()))) {
                return new ILookOverlay.Data(getName().getString(), 0xFFFF00, 0x404000,
                        List.of("§e-> §rRecipe field [" + (i + 1) + "]"));
            }
        }
        return null;
    }
}
