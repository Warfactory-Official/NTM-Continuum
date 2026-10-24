package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerFirebox;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.module.ModuleBurnTime;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.tileentity.ModBlockEntities;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Firebox (1.12.2 {@code TileEntityHeaterFirebox}), the tier-1 solid-fuel heater. Constants and fuel
 * bonuses as in the 1.12.2 class: base 100 TU/t, 100k TU cap; lignite/coal/coke ×1.25 time ×2 heat, solid fuel
 * ×1.5/×3, rocket fuel ×1.5/×5, balefire ×0.5/×15. Configured as {@code firebox} in {@code hbmMachines.json}.
 */
public class TileEntityHeaterFirebox extends TileEntityFireboxBase {

    public static int baseHeat = 100;
    public static double timeMult = 1D;
    public static int maxHeatEnergy = 100_000;

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
        @Override public String getConfigName() { return "firebox"; }

        @Override public void readIfPresent(JsonObject obj) {
            baseHeat = IConfigurableMachine.grab(obj, "I:baseHeat", baseHeat);
            timeMult = IConfigurableMachine.grab(obj, "D:burnTimeMult", timeMult);
            maxHeatEnergy = IConfigurableMachine.grab(obj, "I:heatCap", maxHeatEnergy);
            // 1.7 tests for "burnModule" but reads "M:burnModule", so the written key alone changes nothing
            if (obj.has("burnModule")) burnModule.readIfPresent(obj.get("M:burnModule").getAsJsonObject());
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:baseHeat").value(baseHeat);
            writer.name("D:burnTimeMult").value(timeMult);
            writer.name("I:heatCap").value(maxHeatEnergy);
            writer.name("M:burnModule").beginObject();
            burnModule.writeConfig(writer);
            writer.endObject();
        }
    };

    public TileEntityHeaterFirebox(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_FIREBOX.get(), pos, state);
    }

    @Override
    public String getDefaultName() {
        return "container.heaterFirebox";
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

    // --- MenuProvider ---

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerFirebox(containerId, playerInventory, this);
    }
}
