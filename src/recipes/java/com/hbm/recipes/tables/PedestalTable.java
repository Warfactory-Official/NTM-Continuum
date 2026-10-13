package com.hbm.recipes.tables;

import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.inventory.recipes.PedestalRecipes.Input;
import com.hbm.inventory.recipes.PedestalRecipes.PedestalExtraCondition;
import com.hbm.inventory.recipes.PedestalRecipes.PedestalRecipe;
import com.hbm.inventory.recipes.PedestalRecipes.Row;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code PedestalRecipes.registerDefaults}. Rows name their items by id (the row type holds ids), so 1.7's
 * {@code new ComparableStack(item, n)} and {@code new OreDictStack(MAT.shape(), n)} read {@code i("hbm:item", n)} and
 * {@code i("#c:shapes/material", n)}; {@code null} is a pedestal that must stay empty.
 */
public final class PedestalTable extends RecipeSource<Row> {

    // 1.7 weapon_mod_special metadata SCOPE / SILENCER / FURNITURE_BLACK, items of their own
    private static final String SCOPE = "hbm:weapon_mod_special_scope";
    private static final String SILENCER = "hbm:weapon_mod_special_silencer";
    private static final String FURNITURE_BLACK = "hbm:weapon_mod_special_furniture_black";

    public PedestalTable() {
        super(PedestalRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        register(new PedestalRecipe("hbm:gun_light_revolver_dani", 1,
                null, i("#c:plates/lead"), null,
                i("#c:plates/gold"), i("hbm:gun_light_revolver"), i("#c:plates/gold"),
                null, i("#c:plates/lead"), null));

        register(new PedestalRecipe("hbm:gun_maresleg_broken", 1,
                i("hbm:barbed_wire"), i("#c:plates/weaponsteel"), i("hbm:barbed_wire"),
                i("#c:plates/weaponsteel"), i("hbm:gun_maresleg"), i("#c:plates/weaponsteel"),
                i("hbm:barbed_wire"), i("#c:plates/weaponsteel"), i("hbm:barbed_wire")));

        register(new PedestalRecipe("hbm:gun_heavy_revolver_lilmac", 1,
                null, i(SCOPE), null,
                i("hbm:powder_magic"), i("hbm:gun_heavy_revolver"), i("#c:plates/weaponsteel"),
                null, i("#c:grips/bone"), i("minecraft:apple", 3)));

        register(new PedestalRecipe("hbm:gun_heavy_revolver_protege", 1,
                i("hbm:dungeon_chain", 16), i("#c:gems/cinnabar"), i("hbm:dungeon_chain", 16),
                i("hbm:scrap_nuclear"), i("hbm:gun_heavy_revolver"), i("hbm:scrap_nuclear"),
                i("hbm:dungeon_chain", 16), i("#c:gems/cinnabar"), i("hbm:dungeon_chain", 16)));

        register(new PedestalRecipe("hbm:gun_amat_subtlety", 1,
                i("#c:ingots/starmetal"), i("#c:plates_cast/aluminium"), i("#c:ingots/starmetal"),
                i("#c:plates_cast/aluminium"), i("hbm:gun_amat"), i("#c:plates_cast/aluminium"),
                i("#c:ingots/starmetal"), i("#c:plates_cast/aluminium"), i("#c:ingots/starmetal")));

        register(new PedestalRecipe("hbm:gun_amat_penance", 1,
                i("#c:ingots/starmetal"), i("#c:plates_cast/dura_steel"), i("#c:ingots/starmetal"),
                i(SILENCER), i("hbm:gun_amat"), i(FURNITURE_BLACK),
                i("#c:ingots/starmetal"), i("#c:plates_cast/dura_steel"), i("#c:ingots/starmetal")));

        register(new PedestalRecipe("hbm:gun_flamer_daybreaker", 1,
                i("#c:plates_cast/gold"), i("hbm:canned_conserve_slime"), i("#c:plates_cast/gold"),
                i("#c:ingots/phosphorus"), i("hbm:gun_flamer"), i("#c:ingots/phosphorus"),
                i("#c:plates_cast/gold"), i("hbm:stick_dynamite"), i("#c:plates_cast/gold"))
                .extra(PedestalExtraCondition.SUN));

        register(new PedestalRecipe("hbm:gun_autoshotgun_sexy", 1,
                i("hbm:bolt_spike", 16), i("hbm:wild_p"), i("hbm:bolt_spike", 16),
                i("hbm:card_qos"), i("hbm:gun_autoshotgun"), i("hbm:card_aos"),
                i("hbm:bolt_spike", 16), i("#c:ingots/starmetal", 16), i("hbm:bolt_spike", 16)));

        register(new PedestalRecipe("hbm:gun_minigun_lacunae", 1,
                null, i("hbm:powder_magic", 4), null,
                i("hbm:item_secret_selenium_steel", 4), i("hbm:gun_minigun"), i("hbm:item_secret_selenium_steel", 4),
                null, i("hbm:powder_magic", 4), null)
                .extra(PedestalExtraCondition.FULL_MOON));

        register(new PedestalRecipe("hbm:gun_laser_pistol_morning_glory", 1,
                null, i("hbm:morning_glory"), null,
                i("hbm:item_secret_selenium_steel", 2), i("hbm:gun_laser_pistol"), i("hbm:item_secret_selenium_steel", 2),
                null, i("#c:gems/emerald", 16), null));

        register(new PedestalRecipe("hbm:gun_folly", 1,
                i("hbm:item_secret_folly", 4), i("hbm:item_secret_controller", 2), i("hbm:item_secret_folly", 4),
                i("#c:ingots/bscco", 16), i("#c:storage_blocks/starmetal", 64), i("#c:ingots/bscco", 16),
                i("hbm:item_secret_folly", 4), i("hbm:item_secret_controller", 2), i("hbm:item_secret_folly", 4))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));

        register(new PedestalRecipe("hbm:gun_aberrator", 1,
                null, i("hbm:item_secret_aberrator"), null,
                i("hbm:item_secret_aberrator"), i("#c:mechanisms/saturnite", 4), i("hbm:item_secret_aberrator"),
                null, i("hbm:item_secret_aberrator"), null).set(1));
        register(new PedestalRecipe("hbm:gun_aberrator_eott", 1,
                i("hbm:item_secret_aberrator"), i("hbm:item_secret_aberrator"), i("hbm:item_secret_aberrator"),
                i("hbm:item_secret_aberrator"), i("#c:mechanisms/saturnite", 16), i("hbm:item_secret_aberrator"),
                i("hbm:item_secret_aberrator"), i("hbm:item_secret_aberrator"), i("hbm:item_secret_aberrator"))
                .extra(PedestalExtraCondition.GOOD_KARMA).set(1));

        register(new PedestalRecipe("hbm:ammo_secret_folly_sm", 1,
                i("#c:ingots/starmetal"), i("hbm:powder_magic"), i("#c:ingots/starmetal"),
                i("hbm:powder_magic"), i("hbm:chunk_ore_moonstone"), i("hbm:powder_magic"),
                i("#c:ingots/starmetal"), i("hbm:powder_magic"), i("#c:ingots/starmetal"))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));
        register(new PedestalRecipe("hbm:ammo_secret_folly_nuke", 1,
                i("#c:ingots/starmetal"), i("hbm:powder_magic"), i("#c:ingots/starmetal"),
                i("hbm:powder_magic"), i("hbm:ammo_standard_nuke_high", 4), i("hbm:powder_magic"),
                i("#c:ingots/starmetal"), i("hbm:powder_magic"), i("#c:ingots/starmetal"))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));
        register(new PedestalRecipe("hbm:ammo_secret_p35_800", 5,
                null, null, null,
                null, i("hbm:item_secret_aberrator"), null,
                null, null, null).set(1));
        register(new PedestalRecipe("hbm:ammo_secret_p35_800_bl", 10,
                null, null, null,
                null, i("hbm:item_secret_aberrator", 3), null,
                null, null, null).set(1));
    }

    /** 1.7 {@code register(recipe)}; the id is the output's item name. */
    private void register(PedestalRecipe recipe) {
        List<Optional<Input>> grid = new ArrayList<>();
        for (Input input : recipe.input) grid.add(Optional.ofNullable(input));
        register(recipe.output.substring(recipe.output.indexOf(':') + 1),
                new Row(recipe.output, recipe.outputCount, grid, recipe.extra, recipe.recipeSet, nextOrder()));
    }

    private static Input i(String id) { return new Input(id, 1); }
    private static Input i(String id, int count) { return new Input(id, count); }
}
