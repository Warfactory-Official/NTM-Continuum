package com.hbm.inventory.recipes;

import com.hbm.api.data.ReactorDataMaps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.7 {@code RTGRecipes}: the grey RTG's construction and the fresh-to-depleted pairs recipe viewers show.
 *
 * <p>The construction is a row of the assembly machine's datapack table ({@code data/hbm/recipe/assembly_machine/rtg.json},
 * {@code ass.rtg}). The decay pairs are read from the {@code hbm:rtg_pellet} item data map ({@code decays_to}) each time
 * they are asked for, so an addon pellet (NTM: Space's americium) and a datapack change show up without code.
 */
public final class RTGRecipes {
    private RTGRecipes() {}

    /** Loads the assembly table if nothing has yet (kept for its callers; the row itself is data). */
    public static void registerAssembly() {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
    }

    /** Every pellet of the {@code hbm:rtg_pellet} map and the depleted pellet it decays to, in the map's id order. */
    public static Map<ItemStack, ItemStack> decayPairs() {
        Map<ItemStack, ItemStack> result = new LinkedHashMap<>();
        ReactorDataMaps.RTG_PELLET.byId().forEach((id, pellet) -> {
            Item fresh = ReactorDataMaps.itemOf(id);
            Item depleted = ReactorDataMaps.itemOf(pellet.decaysTo());
            if (fresh == Items.AIR || depleted == Items.AIR) return; // a row for an item that is not registered
            result.put(new ItemStack(fresh), new ItemStack(depleted));
        });
        return result;
    }
}
