package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import net.minecraft.core.BlockPos;
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
 * GUI; the look-overlay shows the stored TU + the current burn rate.
 */
public class HeaterFirebox extends BlockDummyable implements ILookOverlay {

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
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityFireboxBase heater)) return null;

        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", heater.heatEnergy) + " TU");
        text.add("§c<- §r" + String.format("%,d", heater.burnHeat) + " TU/t");
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
