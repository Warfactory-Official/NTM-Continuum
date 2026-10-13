package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Firebox (1.12.2 {@code heater_firebox}): a 1×1×3 dummyable solid-fuel heater; the crucible (or another
 * heat consumer) sits directly on top of the core and drains its heat. Right-click opens the 2-fuel-slot
 * GUI; the look-overlay shows the stored TU + the current burn rate. The ring cells are its smoke vents:
 * a duct against any of their outward faces draws the firebox's smoke tanks (1.7 {@code sendSmoke}).
 */
public class HeaterFirebox extends BlockMultiblockMachine implements ILookOverlay, IFluidConnectorBlockMK2 {

    public HeaterFirebox(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeaterFirebox(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFireboxBase heater)) return null;

        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", heater.heatEnergy) + " TU");
        text.add("§c<- §r" + String.format("%,d", heater.burnHeat) + " TU/t");
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_FLUID_IN | PASSIVE_ITEMS);
    }

    /**
     * Each ring cell opens outward on the side it faces, stated analytically because the vent must not
     * widen the footprint the table was sized from.
     */
    @Override
    public int maskAt(int lx, int ly, int lz) {
        if (maskTable().maskAtLocal(lx, ly, lz) == MultiblockMaskTable.NOT_A_CELL) {
            return MultiblockMaskTable.NOT_A_CELL;
        }
        int mask = MASK_NONE;
        if (lx == 1) mask |= MASK_EAST;
        if (lx == -1) mask |= MASK_WEST;
        if (lz == 1) mask |= MASK_SOUTH;
        if (lz == -1) mask |= MASK_NORTH;
        return mask;
    }
}
