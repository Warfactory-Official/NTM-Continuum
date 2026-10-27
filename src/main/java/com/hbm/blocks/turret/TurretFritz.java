package com.hbm.blocks.turret;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.Direction;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.turret.TileEntityTurretFritz;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The one turret that drinks: its flamethrower runs on diesel, so a duct may attach to its pad exactly
 * where a cable may. The other turrets carry no tank and answer no duct.
 */
public class TurretFritz extends TurretBaseNT implements IFluidConnectorBlockMK2, ICapabilityBlock {

    public TurretFritz(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return padConnects(world, new BlockPos(x, y, z), dir);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityTurretFritz(pos, state);
    }


    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn().fluidIn().itemsAtCells()
            .fluidFaces(BlockEntity.class, (be, side) -> side != Direction.UP && side != Direction.DOWN);

    @Override
    public MachineCaps caps() { return CAPS; }
}
