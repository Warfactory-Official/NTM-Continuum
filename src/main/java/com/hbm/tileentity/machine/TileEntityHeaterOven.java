package com.hbm.tileentity.machine;

import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.container.ContainerFirebox;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.module.ModuleBurnTime;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Heating oven (1.12.2 {@code TileEntityHeaterOven}): a high-temperature firebox that also pulls half
 * of the heat stored by the block entity directly below it before running the normal firebox tick.
 * Configured as {@code heatingoven} in {@code hbmMachines.json}.
 */
public class TileEntityHeaterOven extends TileEntityFireboxBase {

    public static int baseHeat = 500;
    public static double timeMult = 0.125D;
    public static int maxHeatEnergy = 500_000;
    public static double heatEff = 0.5D;

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

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "heatingoven"; }

        @Override public void readIfPresent(JsonObject obj) {
            baseHeat = IConfigurableMachine.grab(obj, "I:baseHeat", baseHeat);
            timeMult = IConfigurableMachine.grab(obj, "D:burnTimeMult", timeMult);
            heatEff = IConfigurableMachine.grab(obj, "D:heatPullEff", heatEff);
            maxHeatEnergy = IConfigurableMachine.grab(obj, "I:heatCap", maxHeatEnergy);
            // 1.7 tests for "burnModule" but reads "M:burnModule", so the written key alone changes nothing
            if (obj.has("burnModule")) burnModule.readIfPresent(obj.get("M:burnModule").getAsJsonObject());
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:baseHeat").value(baseHeat);
            writer.name("D:burnTimeMult").value(timeMult);
            writer.name("D:heatPullEff").value(heatEff);
            writer.name("I:heatCap").value(maxHeatEnergy);
            writer.name("M:burnModule").beginObject();
            burnModule.writeConfig(writer);
            writer.endObject();
        }
    };

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
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerFirebox(containerId, playerInventory, this);
    }
}
