package com.hbm.tileentity.machine;

import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.container.ContainerFirebox;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Heating oven (1.12.2 {@code TileEntityHeaterOven}): a high-temperature firebox that also pulls half
 * of the heat stored by the block entity directly below it before running the normal firebox tick.
 * Config-file overrides from the old mod remain deferred; these are the legacy defaults.
 */
public class TileEntityHeaterOven extends TileEntityFireboxBase {

    public static final int baseHeat = 500;
    public static final double timeMult = 0.125D;
    public static final int maxHeatEnergy = 500_000;
    public static final double heatEff = 0.5D;

    public static final ModuleBurnTime burnModule = new ModuleBurnTime()
            .setLigniteTimeMod(1.25)
            .setCoalTimeMod(1.25)
            .setCokeTimeMod(1.25)
            .setSolidTimeMod(1.5)
            .setRocketTimeMod(1.5)
            .setBalefireTimeMod(0.5)
            .setLigniteHeatMod(2)
            .setCoalHeatMod(2)
            .setCokeHeatMod(2)
            .setSolidHeatMod(3)
            .setRocketHeatMod(5)
            .setBalefireHeatMod(15);

    public TileEntityHeaterOven(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_OVEN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHeaterOven oven) {
        oven.tryPullHeat(level);
        TileEntityFireboxBase.serverTick(level, pos, state, oven);
    }

    private void tryPullHeat(Level level) {
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (!(below instanceof IHeatSource source)) return;

        int toPull = Math.max(Math.min(source.getHeatStored(), getMaxHeat() - heatEnergy), 0);
        heatEnergy += (int) (toPull * heatEff);
        source.useUpHeat(toPull);
    }

    @Override
    public String getDefaultName() {
        return "container.heaterOven";
    }

    @Override
    public ModuleBurnTime getModule() {
        return burnModule;
    }

    @Override
    public int getBaseHeat() {
        return baseHeat;
    }

    @Override
    public double getTimeMult() {
        return timeMult;
    }

    @Override
    public int getMaxHeat() {
        return maxHeatEnergy;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.heater_oven");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerFirebox(containerId, playerInventory, this);
    }
}
