package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.assemblyfactory.AssemblyFactoryPorts;
import com.hbm.assemblyfactory.AssemblyFactoryRegistrations;
import com.hbm.assemblyfactory.AssemblyFactorySidedContainer;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.Containers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The legacy 5x3 Assembly Factory: four independent Assembly Machine lanes. */
public final class MachineAssemblyFactory extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder, ILookOverlay {

    public MachineAssemblyFactory(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAssemblyFactory(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        addFactoryExtras(level, new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset), dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addFactoryExtras(level, core, dir);
    }

    private void addFactoryExtras(Level level, BlockPos core, ForgeDirection dir) {
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            if (Math.abs(i) == 2 || Math.abs(j) == 2) {
                makeExtra(level, core.getX() + i, core.getY(), core.getZ() + j);
            }
        }

        ForgeDirection rotation = dir.getRotation(ForgeDirection.UP);
        for (int i = -2; i <= 2; i++) {
            makeExtra(level, core.getX() + dir.offsetX * i + rotation.offsetX * 2, core.getY() + 2,
                    core.getZ() + dir.offsetZ * i + rotation.offsetZ * 2);
            makeExtra(level, core.getX() + dir.offsetX * i - rotation.offsetX * 2, core.getY() + 2,
                    core.getZ() + dir.offsetZ * i - rotation.offsetZ * 2);
        }
    }

    /** Ordinary dummies have no proxy inventory; promoted dummies retain the old accessor-relative view. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(META) < extra) return null;
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineAssemblyFactory factory)) return null;
        return core.equals(pos) ? factory : new AssemblyFactorySidedContainer(factory, pos);
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection direction) {
        return findCoreTE(level, pos) instanceof TileEntityMachineAssemblyFactory factory
                && AssemblyFactoryPorts.isPort(factory, pos, direction);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter level, int x, int y, int z, ForgeDirection direction) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(level, pos) instanceof TileEntityMachineAssemblyFactory factory
                && AssemblyFactoryPorts.isFluidPort(factory, pos, direction, type);
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!level.isClientSide() && state.getValue(META) >= BlockDummyable.offset + 2
                && level.getBlockEntity(pos) instanceof TileEntityMachineAssemblyFactory factory) {
            Containers.dropContents(level, pos, factory);
        }
        super.onBlockRemoved(state, level, pos, movedByPiston);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityMachineAssemblyFactory factory)) return null;

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
