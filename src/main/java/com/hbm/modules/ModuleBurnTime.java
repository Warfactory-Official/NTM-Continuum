package com.hbm.modules;

import dev.golem.core.world.Compat;
import com.hbm.items.ModFuelItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Determines the burn time/heat of a fuel stack with per-fuel-class bonuses — the 1.12.2
 * {@code ModuleBurnTime}, slimmed. Base burn time comes from {@code level.fuelValues()} (the 26.1
 * replacement for {@code TileEntityFurnace.getItemBurnTime}); classification uses exact HBM fuel items
 * plus registry-path/tag checks in place of the old ore-dict name scan. The JSON config plumbing
 * ({@code IConfigurableMachine}) is deferred with that system.
 */
public class ModuleBurnTime {

    private static final int modLog = 0;
    private static final int modWood = 1;
    private static final int modCoal = 2;
    private static final int modLignite = 3;
    private static final int modCoke = 4;
    private static final int modSolid = 5;
    private static final int modRocket = 6;
    private static final int modBalefire = 7;

    private final double[] modTime = new double[8];
    private final double[] modHeat = new double[8];

    public ModuleBurnTime() {
        for (int i = 0; i < modTime.length; i++) {
            modTime[i] = 1.0D;
            modHeat[i] = 1.0D;
        }
    }

    public int getBurnTime(Level level, ItemStack stack) {
        int fuel = Compat.burnDuration(level, stack);
        if (fuel == 0) return 0;
        return (int) (fuel * getMod(stack, modTime));
    }

    public int getBurnHeat(int base, ItemStack stack) {
        if (base <= 0) return 0;
        return (int) (base * getMod(stack, modHeat));
    }

    private double getMod(ItemStack stack, double[] mod) {
        if (stack.isEmpty()) return 0;

        if (stack.is(ModFuelItems.solid_fuel.get())
                || stack.is(ModFuelItems.solid_fuel_presto.get())
                || stack.is(ModFuelItems.solid_fuel_presto_triplet.get())) return mod[modSolid];
        if (stack.is(ModFuelItems.rocket_fuel.get())) return mod[modRocket];

        // The old ore-dict name scan ("Coke"/"Coal"/"Lignite"/log*/"Wood"), on registry path + tags.
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.contains("coke")) return mod[modCoke];
        if (path.contains("lignite")) return mod[modLignite];
        if (path.contains("coal") && !path.contains("charcoal")) return mod[modCoal];
        if (stack.is(ItemTags.LOGS)) return mod[modLog];
        if (path.contains("wood") || stack.is(ItemTags.PLANKS)) return mod[modWood];

        return 1D;
    }

    public ModuleBurnTime setLogTimeMod(double mod) { this.modTime[modLog] = mod; return this; }
    public ModuleBurnTime setWoodTimeMod(double mod) { this.modTime[modWood] = mod; return this; }
    public ModuleBurnTime setCoalTimeMod(double mod) { this.modTime[modCoal] = mod; return this; }
    public ModuleBurnTime setLigniteTimeMod(double mod) { this.modTime[modLignite] = mod; return this; }
    public ModuleBurnTime setCokeTimeMod(double mod) { this.modTime[modCoke] = mod; return this; }
    public ModuleBurnTime setSolidTimeMod(double mod) { this.modTime[modSolid] = mod; return this; }
    public ModuleBurnTime setRocketTimeMod(double mod) { this.modTime[modRocket] = mod; return this; }
    public ModuleBurnTime setBalefireTimeMod(double mod) { this.modTime[modBalefire] = mod; return this; }

    public ModuleBurnTime setLogHeatMod(double mod) { this.modHeat[modLog] = mod; return this; }
    public ModuleBurnTime setWoodHeatMod(double mod) { this.modHeat[modWood] = mod; return this; }
    public ModuleBurnTime setCoalHeatMod(double mod) { this.modHeat[modCoal] = mod; return this; }
    public ModuleBurnTime setLigniteHeatMod(double mod) { this.modHeat[modLignite] = mod; return this; }
    public ModuleBurnTime setCokeHeatMod(double mod) { this.modHeat[modCoke] = mod; return this; }
    public ModuleBurnTime setSolidHeatMod(double mod) { this.modHeat[modSolid] = mod; return this; }
    public ModuleBurnTime setRocketHeatMod(double mod) { this.modHeat[modRocket] = mod; return this; }
    public ModuleBurnTime setBalefireHeatMod(double mod) { this.modHeat[modBalefire] = mod; return this; }
}
